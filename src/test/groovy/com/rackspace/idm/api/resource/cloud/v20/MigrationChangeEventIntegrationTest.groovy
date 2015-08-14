package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.OTPDeviceDao
import com.rackspace.idm.domain.dao.UniqueId
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.util.migration.ChangeType
import com.rackspace.idm.util.migration.PersistenceTarget
import com.rackspace.idm.util.migration.ldap.LdapChangeEventRecorderListener
import com.rackspace.idm.util.migration.ldap.LdapChangeEventRepository
import com.rackspace.idm.util.migration.ldap.LdapMigrationChangeEvent
import com.unboundid.ldap.sdk.Filter
import org.apache.commons.collections4.IteratorUtils
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.junit.IgnoreByRepositoryProfile

class MigrationChangeEventIntegrationTest extends RootIntegrationTest {
    @Autowired
    LdapChangeEventRecorderListener ldapChangeEventRecorderListener

    @Autowired
    LdapChangeEventRepository ldapChangeEventRepository

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    OTPDeviceDao otpDeviceDao

    @Autowired
    UserDao userDao

    @Shared
    def specificationServiceAdminToken

    def setupSpec() {
        specificationServiceAdminToken = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD).getEntity(AuthenticateResponse).value.token.id
    }

    /**
     * Test full integration through REST service add, modify, delete. Includes specifying the type of events to record.
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    @Unroll
    def "Record Change Event :: OTP Device :: Recording '#recordChangeTypes' - A change event is recorded to LDAP for OTP Devices"() {
        given:
        //set up properties to record event
        enableListenerForChangeTypes(ldapChangeEventRecorderListener.getListenerName(), recordChangeTypes.toArray(new ChangeType[recordChangeTypes.size()]))

        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin()
        String userAdminToken = utils.getToken(userAdmin.username)
        DateTime beforeStart = new DateTime();

        def userEntity = userDao.getUserById(userAdmin.id)

        when: "add OTP Device"
        OTPDevice device = utils.addOtpDeviceToUser(userAdminToken, userAdmin)

        com.rackspace.idm.domain.entity.OTPDevice deviceEntity = otpDeviceDao.getOTPDeviceByParentAndId(userEntity, device.id)

        List<LdapMigrationChangeEvent> eventsAfterAdd = getEventsForEntity(deviceEntity)

        then: "an add ldap entry is recorded if applicable"
        if (recordChangeTypes.contains(ChangeType.ADD)) {
            assert eventsAfterAdd.size() == 1
            verifyEvent(eventsAfterAdd.get(0), ChangeType.ADD, deviceEntity, beforeStart)
        } else {
            assert eventsAfterAdd.size() == 0
        }

        when: "modify OTP Device"
        //verify will modify the OTP device by setting verified flag
        utils.verifyOTPDevice(userAdminToken, userAdmin.id, device.id, utils.getOTPVerificationCodeForDevice(device))
        List<LdapMigrationChangeEvent> eventsAfterModify = getEventsForEntity(deviceEntity)

        then: "modify event recorded"
        if (recordChangeTypes.contains(ChangeType.MODIFY)) {
            assert eventsAfterModify.size() == eventsAfterAdd.size() + 1
            verifyEvent(eventsAfterModify.last(), ChangeType.MODIFY, deviceEntity, beforeStart)
        } else {
            assert eventsAfterModify.size() == eventsAfterAdd.size()
        }

        when: "delete OTP Device"
        utils.deleteOTPDeviceFromUser(userAdminToken, userAdmin.id, device.id)
        List<LdapMigrationChangeEvent> eventsAfterDelete = getEventsForEntity(deviceEntity)

        then: "delete event is recorded"
        if (recordChangeTypes.contains(ChangeType.DELETE)) {
            assert eventsAfterDelete.size() == eventsAfterModify.size() + 1
            verifyEvent(eventsAfterDelete.last(), ChangeType.DELETE, deviceEntity, beforeStart)
        } else {
            assert eventsAfterDelete.size() == eventsAfterModify.size()
        }

        where:
        recordChangeTypes                                    | _
        [ChangeType.ADD,ChangeType.MODIFY,ChangeType.DELETE] | _
        [ChangeType.MODIFY,ChangeType.DELETE] | _
        [ChangeType.ADD,ChangeType.MODIFY] | _
        [ChangeType.DELETE] | _
    }

    /**
     * Test full integration through REST service add, modify, delete
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "Record Change Event :: User"() {
        given:
        //set up properties to record event
        enableListenerForAllChangeTypes(ldapChangeEventRecorderListener.getListenerName())
        DateTime beforeStart = new DateTime();

        User userAdmin
        def users

        when: "add User"
        (userAdmin, users) = utils.createUserAdmin()
        def entity = userDao.getUserById(userAdmin.id)

        List<LdapMigrationChangeEvent> eventsAfterAdd = getEventsForEntity(entity)

        then: "an add ldap entry is recorded if applicable"
        assert eventsAfterAdd.size() == 1
        verifyEvent(eventsAfterAdd.get(0), ChangeType.ADD, entity, beforeStart)

        when: "modify User"
        //verify will modify the OTP device by setting verified flag
        userAdmin.setEmail("somethingnew@rackspace.com")
        utils.updateUser(userAdmin)
        List<LdapMigrationChangeEvent> eventsAfterModify = getEventsForEntity(entity)

        then: "modify event recorded"
        assert eventsAfterModify.size() == eventsAfterAdd.size() + 1
        verifyEvent(eventsAfterModify.last(), ChangeType.MODIFY, entity, beforeStart)

        when: "delete user"
        utils.deleteUser(userAdmin)
        List<LdapMigrationChangeEvent> eventsAfterDelete = getEventsForEntity(entity)

        then: "delete event is recorded"
        assert eventsAfterDelete.size() == eventsAfterModify.size() + 1
        verifyEvent(eventsAfterDelete.last(), ChangeType.DELETE, entity, beforeStart)
    }

    /**
     * Test MFA Changes cause modify event
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "Record Change Event :: User MFA modification"() {
        given:
        //set up properties to record event
        enableListenerForAllChangeTypes(ldapChangeEventRecorderListener.getListenerName())

        DateTime beforeStart = new DateTime();

        User userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin()
        def entity = userDao.getUserById(userAdmin.id)
        List<LdapMigrationChangeEvent> eventsAfterAdd = getEventsForEntity(entity)

        when: "Add Mobile Phone To User"
        MobilePhone phone = utils.addMobilePhoneToUser(specificationServiceAdminToken, userAdmin)
        List<LdapMigrationChangeEvent> eventsAfterAddingPhone = getEventsForEntity(entity)

        then: "a modify ldap entry is recorded"
        assert eventsAfterAddingPhone.size() == eventsAfterAdd.size() + 1
        verifyEvent(eventsAfterAddingPhone.last(), ChangeType.MODIFY, entity, beforeStart)

        when: "Send verification code To User"
        utils.sendVerificationCodeToPhone(specificationServiceAdminToken, userAdmin.id, phone.id)
        List<LdapMigrationChangeEvent> eventsAfterSendVerification = getEventsForEntity(entity)

        then: "a modify ldap entry is recorded"
        assert eventsAfterSendVerification.size() == eventsAfterAddingPhone.size() + 1
        verifyEvent(eventsAfterSendVerification.last(), ChangeType.MODIFY, entity, beforeStart)

    }

    def void verifyEvent(LdapMigrationChangeEvent event, ChangeType expectedChangeType, UniqueId expectedEntityRecorded, DateTime expectedOccurredOnOrAfter) {
        assert event.getChangeType() == expectedChangeType
        assert event.getPersistenceTarget() == PersistenceTarget.LDAP

        assert event.getChangeOccurredDate() != null
        def deleteChangeDate = new DateTime(event.getChangeOccurredDate())
        assert deleteChangeDate.equals(expectedOccurredOnOrAfter) || deleteChangeDate.isAfter(expectedOccurredOnOrAfter)

        assert event.getEntityId() == expectedEntityRecorded.getUniqueId()
        assert event.getHostName() == identityConfig.getReloadableConfig().getNodeName()
        assert event.getId() != null
    }


    def void enableListenerForAllChangeTypes(String listener) {
        reloadableConfiguration.setProperty(getMigrationListerEnabledPropertyName(listener), true)
        setListenerIgnoreChangeTypes(listener)
    }

    def void enableListenerForChangeTypes(String listener, ChangeType ... allowedChangeType) {
        List<ChangeType> changeTypes = [ChangeType.ADD, ChangeType.DELETE, ChangeType.MODIFY]
        for (ChangeType allowedType : allowedChangeType) {
            changeTypes.remove(allowedType)
        }

        reloadableConfiguration.setProperty(getMigrationListerEnabledPropertyName(listener), true)
        setListenerIgnoreChangeTypes(listener, changeTypes.toArray(new ChangeType[changeTypes.size()]))
    }

    def void setListenerIgnoreChangeTypes(String listener, ChangeType... ignoredChangeTypes) {
        String changeTypeCSV = StringUtils.join(ignoredChangeTypes, ",");
        reloadableConfiguration.setProperty(getMigrationListerIgnoredChangeTypesPropertyName(listener), changeTypeCSV);
    }

    def String getMigrationListerEnabledPropertyName(String listenerName) {
        return String.format(IdentityConfig.MIGRATION_LISTENER_HANDLES_MIGRATION_CHANGE_EVENTS_PROP_REG, listenerName);
    }

    def String getMigrationListerIgnoredChangeTypesPropertyName(String listenerName) {
        return String.format(IdentityConfig.MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_REG, listenerName);
    }

    private List<LdapMigrationChangeEvent> getEventsForEntity(UniqueId entity) {
        Filter filter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_ENTITY_ID, entity.getUniqueId())
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CHANGE_EVENT).build();

        Iterable<LdapMigrationChangeEvent> it = ldapChangeEventRepository.getObjects(filter);
        return IteratorUtils.toList(it.iterator());
    }
}
