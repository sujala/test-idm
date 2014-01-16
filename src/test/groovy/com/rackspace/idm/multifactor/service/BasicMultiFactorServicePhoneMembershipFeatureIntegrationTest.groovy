package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.idm.multifactor.domain.BasicPin
import com.rackspace.idm.multifactor.domain.Pin
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared

/**
 * Tests the multi-factor service with the exception of the MobilePhoneVerification dependency. The production class will sends texts through Duo Security, which
 * costs money per text. This is NOT desirable for a regression test that will continually run. Instead a simulated service is injected that will return a constant
 * PIN. Actually testing SMS texts should be performed via some other mechanism - in a manual fashion.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class BasicMultiFactorServicePhoneMembershipFeatureIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification

    @Shared private boolean initialFeatureState = false;

    public setup() {
       initialFeatureState = multiFactorService.isPhoneUserMembershipEnabled()
    }

    public cleanup() {
        globalConfig.setProperty(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, initialFeatureState);
    }

    /**
     * This tests when feature is disabled
     *
     * @return
     */
    def "Add a phone to a user-admin without phone membership"() {
        setup:
        globalConfig.setProperty(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, false);
        assert !multiFactorService.isPhoneUserMembershipEnabled()
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify passed in object is updated with id, and phone number still as expected
        phone.getId() == retrievedPhone.getId()
        phone.getTelephoneNumber() == canonTelephoneNumber
        retrievedPhone.getTelephoneNumber() == canonTelephoneNumber
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()
        phone.getMembers() == null

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(retrievedPhone)
    }

    /**
     * This tests when feature is enabled
     *
     * @return
     */
    def "Add a phone to a user-admin with phone membership"() {
        setup:
        globalConfig.setProperty(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, true);
        assert multiFactorService.isPhoneUserMembershipEnabled()
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify passed in object is updated with id, and phone number still as expected
        phone.getId() == retrievedPhone.getId()
        phone.getTelephoneNumber() == canonTelephoneNumber
        retrievedPhone.getTelephoneNumber() == canonTelephoneNumber
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()
        phone.getMembers() != null
        phone.getMembers().contains(finalUserAdmin.getLdapEntry().getParsedDN())

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(retrievedPhone)
    }

    /**
     * This tests when feature is enabled, that will remove users from phone when mfa is removed.
     *
     * @return
     */
    def "Remove multifactor from a user-admin with phone membership enabled"() {
        setup:
        globalConfig.setProperty(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, true);
        assert multiFactorService.isPhoneUserMembershipEnabled()
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)

        when:
        multiFactorService.removeMultiFactorForUser(userAdminOpenStack.getId())
        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == null
        finalPhone.getMembers() == null

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(finalPhone)
    }

    /**
     * This tests when feature is enabled, that will remove users from phone when mfa is removed.
     *
     * @return
     */
    def "Remove multifactor from phone with multiple users with phone membership enabled"() {
        setup:
        globalConfig.setProperty(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, true);
        assert multiFactorService.isPhoneUserMembershipEnabled()
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        org.openstack.docs.identity.api.v2.User userAdmin2OpenStack = createUserAdmin()
        User initialUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        User initialUserAdmin2 = userRepository.getUserById(userAdmin2OpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        MobilePhone phone2 = multiFactorService.addPhoneToUser(userAdmin2OpenStack.getId(), telephoneNumber)
        MobilePhone initialPhone = mobilePhoneRepository.getById(phone.getId())
        assert initialPhone.getMembers().contains(initialUserAdmin.getLdapEntry().getParsedDN())
        assert initialPhone.getMembers().contains(initialUserAdmin2.getLdapEntry().getParsedDN())

        when:
        multiFactorService.removeMultiFactorForUser(userAdminOpenStack.getId())
        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == null
        !finalPhone.getMembers().contains(initialUserAdmin.getLdapEntry().getParsedDN())
        finalPhone.getMembers().contains(initialUserAdmin2.getLdapEntry().getParsedDN())

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(finalPhone)
    }

}
