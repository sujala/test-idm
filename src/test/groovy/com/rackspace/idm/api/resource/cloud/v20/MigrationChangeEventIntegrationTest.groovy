package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.OTPDeviceDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.UniqueId
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.migration.ChangeType
import com.rackspace.idm.domain.migration.dao.DeltaDao
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class MigrationChangeEventIntegrationTest extends RootIntegrationTest {

    @Autowired
    DeltaDao deltaDao

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    OTPDeviceDao otpDeviceDao

    @Autowired
    UserDao userDao

    @Autowired
    TenantRoleDao tenantRoleDao

    @Shared
    def specificationServiceAdminToken

    def setupSpec() {
        specificationServiceAdminToken = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD).getEntity(AuthenticateResponse).value.token.id
    }

    /**
     * Test full integration through REST service add, modify, delete. Includes specifying the type of events to record.
     * @return
     */
    @Unroll
    def "Record Change Event :: OTP Device :: Recording '#recordChangeTypes' - A change event is recorded to LDAP for OTP Devices"() {
        given:
        //set up properties to record event
        enableListenerForChangeTypes(recordChangeTypes.toArray(new ChangeType[recordChangeTypes.size()]))

        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin()
        String userAdminToken = utils.getToken(userAdmin.username)
        DateTime beforeStart = new DateTime();

        def userEntity = userDao.getUserById(userAdmin.id)

        when: "add OTP Device"
        OTPDevice device = utils.addOtpDeviceToUser(userAdminToken, userAdmin)

        com.rackspace.idm.domain.entity.OTPDevice deviceEntity = otpDeviceDao.getOTPDeviceByParentAndId(userEntity, device.id)

        List<?> eventsAfterAdd = getEventsForEntity(deviceEntity)

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
        List<?> eventsAfterModify = getEventsForEntity(deviceEntity)

        then: "modify event recorded"
        if (recordChangeTypes.contains(ChangeType.MODIFY)) {
            assert eventsAfterModify.size() == eventsAfterAdd.size() + 1
            verifyEvent(eventsAfterModify.last(), ChangeType.MODIFY, deviceEntity, beforeStart)
        } else {
            assert eventsAfterModify.size() == eventsAfterAdd.size()
        }

        when: "delete OTP Device"
        utils.deleteOTPDeviceFromUser(userAdminToken, userAdmin.id, device.id)
        List<?> eventsAfterDelete = getEventsForEntity(deviceEntity)

        then: "delete event is recorded"
        if (recordChangeTypes.contains(ChangeType.DELETE)) {
            assert eventsAfterDelete.size() == eventsAfterModify.size() + 1
            verifyEvent(eventsAfterDelete.last(), ChangeType.DELETE, deviceEntity, beforeStart)
        } else {
            assert eventsAfterDelete.size() == eventsAfterModify.size()
        }

        cleanup:
        deltaDao.deleteAll()

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
    def "Record Change Event :: User"() {
        given:
        //set up properties to record event
        enableListenerForAllChangeTypes()
        DateTime beforeStart = new DateTime();

        User userAdmin
        def users

        when: "add User"
        (userAdmin, users) = utils.createUserAdmin()
        def entity = userDao.getUserById(userAdmin.id)

        List<?> eventsAfterAdd = getEventsForEntity(entity)

        then: "an add ldap entry is recorded if applicable"
        assert eventsAfterAdd.size() == 1
        verifyEvent(eventsAfterAdd.get(0), ChangeType.ADD, entity, beforeStart)

        when: "modify User"
        //verify will modify the OTP device by setting verified flag
        userAdmin.setEmail("somethingnew@rackspace.com")
        utils.updateUser(userAdmin)
        List<?> eventsAfterModify = getEventsForEntity(entity)

        then: "modify event recorded"
        assert eventsAfterModify.size() == eventsAfterAdd.size() + 1
        verifyEvent(eventsAfterModify.last(), ChangeType.MODIFY, entity, beforeStart)

        when: "delete user"
        utils.deleteUser(userAdmin)
        List<?> eventsAfterDelete = getEventsForEntity(entity)

        then: "delete event is recorded"
        assert eventsAfterDelete.size() == eventsAfterModify.size() + 1
        verifyEvent(eventsAfterDelete.last(), ChangeType.DELETE, entity, beforeStart)

        cleanup:
        deltaDao.deleteAll()
    }

    /**
     * Test MFA Changes cause modify event
     * @return
     */
    def "Record Change Event :: User MFA modification"() {
        given:
        //set up properties to record event
        enableListenerForAllChangeTypes()

        DateTime beforeStart = new DateTime();

        User userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin()
        def entity = userDao.getUserById(userAdmin.id)
        List<?> eventsAfterAdd = getEventsForEntity(entity)

        when: "Add Mobile Phone To User"
        MobilePhone phone = utils.addMobilePhoneToUser(specificationServiceAdminToken, userAdmin)
        List<?> eventsAfterAddingPhone = getEventsForEntity(entity)

        then: "a modify ldap entry is recorded"
        assert eventsAfterAddingPhone.size() == eventsAfterAdd.size() + 1
        verifyEvent(eventsAfterAddingPhone.last(), ChangeType.MODIFY, entity, beforeStart)

        when: "Send verification code To User"
        utils.sendVerificationCodeToPhone(specificationServiceAdminToken, userAdmin.id, phone.id)
        List<?> eventsAfterSendVerification = getEventsForEntity(entity)

        then: "a modify ldap entry is recorded"
        assert eventsAfterSendVerification.size() == eventsAfterAddingPhone.size() + 1
        verifyEvent(eventsAfterSendVerification.last(), ChangeType.MODIFY, entity, beforeStart)

        cleanup:
        deltaDao.deleteAll()
    }

    def "record change event for adding role to two different tenants for same user"() {
        given:
        //set up properties to record event
        enableListenerForAllChangeTypes()
        def beforeStart = new DateTime();
        def user, users
        (user, users) = utils.createUserAdmin()
        def tenant1 = utils.createTenant()
        def tenant2 = utils.createTenant()
        def role = utils.createRole()

        when: "add the role to the user on the tenant"
        utils.addRoleToUserOnTenant(user, tenant1, role.id)
        def roleEntity = tenantRoleDao.getTenantRoleForUser(userDao.getUserById(user.id), role.id)
        def events = getEventsForEntity(roleEntity)

        then: "an ADD event was recorded for the role"
        events.size() == 1
        verifyEvent(events.last(), ChangeType.ADD, roleEntity, beforeStart)

        when: "add the role to the user on the other tenant"
        utils.addRoleToUserOnTenant(user, tenant2, role.id)
        events = getEventsForEntity(roleEntity)

        then: "an "
        events.size() == 2
        verifyEvent(events.last(), ChangeType.MODIFY, roleEntity, beforeStart)

        cleanup:
        deltaDao.deleteAll()
        utils.deleteUsers(user)
        utils.deleteRole(role)
        utils.deleteTenant(tenant1)
        utils.deleteTenant(tenant2)
    }

    def void verifyEvent(Object event, ChangeType expectedChangeType, UniqueId expectedEntityRecorded, DateTime expectedOccurredOnOrAfter) {
        assert expectedChangeType.equals(event.event)

        assert event.created != null
        def deleteChangeDate = new DateTime(event.created)
        assert deleteChangeDate.equals(expectedOccurredOnOrAfter) || deleteChangeDate.isAfter(expectedOccurredOnOrAfter)

        assert expectedEntityRecorded.getUniqueId().equals(event.type)
        assert identityConfig.getReloadableConfig().getNodeName().equals(event.host)
        assert event.id != null
    }


    def void enableListenerForAllChangeTypes() {
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, true)
        setListenerIgnoreChangeTypes()
    }

    def void enableListenerForChangeTypes(ChangeType ... allowedChangeType) {
        List<ChangeType> changeTypes = [ChangeType.ADD, ChangeType.DELETE, ChangeType.MODIFY]
        for (ChangeType allowedType : allowedChangeType) {
            changeTypes.remove(allowedType)
        }

        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, true)
        setListenerIgnoreChangeTypes(changeTypes.toArray(new ChangeType[changeTypes.size()]))
    }

    def void setListenerIgnoreChangeTypes(ChangeType... ignoredChangeTypes) {
        String changeTypeCSV = StringUtils.join(ignoredChangeTypes, ",");
        reloadableConfiguration.setProperty(IdentityConfig.MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, changeTypeCSV);
    }

    private List<?> getEventsForEntity(UniqueId entity) {
        return deltaDao.findByType(entity.getUniqueId())
    }

}
