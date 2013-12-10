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

/**
 * Tests the multi-factor service with the exception of the MobilePhoneVerification dependency. The production class will sends texts through Duo Security, which
 * costs money per text. This is NOT desirable for a regression test that will continually run. Instead a simulated service is injected that will return a constant
 * PIN. Actually testing SMS texts should be performed via some other mechanism - in a manual fashion.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class BasicMultiFactorServiceIntegrationTest extends RootConcurrentIntegrationTest {
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

    /**
     * This tests linking a phone number to a user
     *
     * @return
     */
    def "Add a phone to a user-admin"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        then:
        //verify passed in object is updated with id, and phone number still as expected
        phone.getId() != null
        phone.getTelephoneNumber() == canonTelephoneNumber

        phone.getId() == retrievedPhone.getId()
        retrievedPhone.getTelephoneNumber() == canonTelephoneNumber

        finalUserAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(retrievedPhone)
    }

    /**
     * Verifies that sending pin to phone sets appropriate information on user.
     *
     * @return
     */
    def "Successfully send an SMS to a phone associated to a user-admin and update user object"() {
        setup:

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        Date testExpirationDate = multiFactorService.generatePinExpirationDateFromNow().minusMinutes(1).toDate();

        when:
        multiFactorService.sendVerificationPin(userAdmin.getId(), retrievedPhone.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorDevicePin() == simulatorMobilePhoneVerification.getConstantPin().getPin()
        userAdmin.getMultiFactorDevicePinExpiration().after(testExpirationDate)
        !userAdmin.getMultiFactorDeviceVerified()
        !userAdmin.getMultifactorEnabled()

        cleanup:
        userRepository.deleteObject(userAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * Verifies that sending pin to phone sets appropriate information on user.
     *
     * @return
     */
    def "Sending second pin overwrites initial"() {
        setup:
        Pin initialPin = simulatorMobilePhoneVerification.constantPin
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        Date testExpirationDate = multiFactorService.generatePinExpirationDateFromNow().minusMinutes(1).toDate();

        //send first pin
        multiFactorService.sendVerificationPin(userAdmin.getId(), retrievedPhone.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        assert userAdmin.getMultiFactorDevicePin() == initialPin.getPin()

        Pin updatedPin = new BasicPin("9999")
        simulatorMobilePhoneVerification.setConstantPin(updatedPin)

        when: "send a second pin code"
        multiFactorService.sendVerificationPin(userAdmin.getId(), retrievedPhone.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then: "old code is overwritten"
        userAdmin.getMultiFactorDevicePin() == updatedPin.getPin()
        userAdmin.getMultiFactorDevicePinExpiration().after(testExpirationDate)
        !userAdmin.getMultiFactorDeviceVerified()
        !userAdmin.getMultifactorEnabled()

        cleanup:
        simulatorMobilePhoneVerification.setConstantPin(initialPin)
        userRepository.deleteObject(userAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * Verifies that after a phone associated with a user is verified, the user object state is updated appropriately
     *
     * @return
     */
    def "Successfully verify a phone associated to a user-admin"() {
        setup:
        String pin = "1234";

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        userAdmin.setMultiFactorDevicePin(pin);
        userAdmin.setMultiFactorDevicePinExpiration(new DateTime().plusDays(1).toDate()) //set the expiration to tomorrow
        userRepository.updateObject(userAdmin)

        when:
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), retrievedPhone.getId(), new BasicPin(pin))
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorMobilePhoneRsId() == retrievedPhone.getId()
        userAdmin.getMultiFactorDevicePinExpiration() == null  //date reset
        userAdmin.getMultiFactorDevicePin() == null
        userAdmin.getMultiFactorDeviceVerified()
        !userAdmin.getMultifactorEnabled()

        cleanup:
        userRepository.deleteObject(userAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * Verifies can enable multi-factor on an account. Ultimately this goes through whole set of services to add a phone,
     * send verification pin, verify, and finally enable.
     *
     * @return
     */
    def "Successfully enable multi-factor"() {
        setup:
        Pin verificationCode = simulatorMobilePhoneVerification.constantPin;

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), verificationCode)

        when:
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()
        userAdmin.getMultiFactorDevicePinExpiration() == null
        userAdmin.getMultiFactorDevicePin() == null
        userAdmin.getMultiFactorDeviceVerified()
        userAdmin.getMultifactorEnabled()
        StringUtils.isNotBlank(userAdmin.getExternalMultiFactorUserId())

        cleanup:
        if (userAdmin != null) {
            multiFactorService.removeMultiFactorForUser(userAdmin.id)  //remove duo profile
            userRepository.deleteObject(userAdmin)
        }
        if (phone != null) mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * Verifies can disable multi-factor on an account. Ultimately this goes through whole set of services to add a phone,
     * send verification pin, verify, enable, and finally disable.
     *
     * @return
     */
    def "Successfully disable multi-factor"() {
        setup:
        Pin verificationCode = simulatorMobilePhoneVerification.constantPin;

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), verificationCode)
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))

        when:
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(false))
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()
        userAdmin.getMultiFactorDevicePinExpiration() == null
        userAdmin.getMultiFactorDevicePin() == null
        userAdmin.isMultiFactorDeviceVerified()
        !userAdmin.isMultiFactorEnabled()
        StringUtils.isBlank(userAdmin.getExternalMultiFactorUserId())

        cleanup:
        if (userAdmin != null) {
            multiFactorService.removeMultiFactorForUser(userAdmin.id)  //remove duo profile
            userRepository.deleteObject(userAdmin)
        }
        if (phone != null) mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * Verifies can re-enable multi-factor on an account after disabling.
     *
     * @return
     */
    def "Successfully re-enable multi-factor after disabling"() {
        setup:
        Pin verificationCode = simulatorMobilePhoneVerification.constantPin;

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), verificationCode)
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(false))

        when:
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()
        userAdmin.getMultiFactorDevicePinExpiration() == null
        userAdmin.getMultiFactorDevicePin() == null
        userAdmin.isMultiFactorDeviceVerified()
        userAdmin.isMultiFactorEnabled()
        StringUtils.isNotBlank(userAdmin.getExternalMultiFactorUserId())

        cleanup:
        if (userAdmin != null) {
            multiFactorService.removeMultiFactorForUser(userAdmin.id)  //remove duo profile
            userRepository.deleteObject(userAdmin)
        }
        if (phone != null) mobilePhoneRepository.deleteObject(phone)
    }


}
