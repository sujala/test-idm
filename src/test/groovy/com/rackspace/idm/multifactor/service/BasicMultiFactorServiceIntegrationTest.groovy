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
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.test.SingleTestConfiguration
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration

/**
 * Tests the multi-factor service with the exception of the MobilePhoneVerification dependency. The production class will sends texts through Duo Security, which
 * costs money per text. This is NOT desirable for a regression test that will continually run. Instead a simulated service is injected that will return a constant
 * PIN. Actually testing SMS texts should be performed via some other mechanism - in a manual fashion.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/service/BasicMultiFactorServiceIntegrationTest-context.xml"])
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

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        Date testExpirationDate = multiFactorService.generatePinExpirationDateFromNow().minusMinutes(1).toDate();

        //send first pin
        multiFactorService.sendVerificationPin(userAdmin.getId(), retrievedPhone.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        assert userAdmin.getMultiFactorDevicePin() == simulatorMobilePhoneVerification.constantPin.getPin()

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
     * TODO: This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "Golden use case of adding phone, send pin, verify pin works in harmony"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        //STEP 1: Add phone to user
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)

        //STEP 2: Send PIN
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())

        //STEP 3: Verify PIN
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), simulatorMobilePhoneVerification.getConstantPin())

        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUser = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify phone
        finalPhone.getId() != null
        finalPhone.getTelephoneNumber() == canonTelephoneNumber

        //verify user state
        finalUser.getMultiFactorMobilePhoneRsId() == finalPhone.getId()
        finalUser.getMultiFactorDevicePin() == null
        finalUser.getMultiFactorDevicePinExpiration() == null
        finalUser.getMultiFactorDeviceVerified()
        !finalUser.getMultifactorEnabled()

        cleanup:
        userRepository.deleteObject(userAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }

    /**
     * This bean configuration is used to substitute the DuoMobilePhoneVerification with a simulator so an SMS message is not actually sent and instead
     * a given pin is returned. The context file BasicMultiFactorServiceIntegrationTest-context.xml
     * loads this file.
     */
    @SingleTestConfiguration
    public static class Config {
        public static final DEFAULT_CONSTANT_PIN = new BasicPin("1234");

        /**
         * Create a simulator mobile phone verification so sms messages are not sent. Add the @Primary annotation so autowiring will choose this
         * bean over the standard mobilePhoneVerification bean that will be created as part of loading the main config. Without this the bean wiring
         * will fail due to 2 beans of type MobilePhoneVerification
         * @return
         */
        @Primary
        @Bean
        MobilePhoneVerification simulatorMobilePhoneVerification() {
            return new SimulatorMobilePhoneVerification(DEFAULT_CONSTANT_PIN);
        }
    }
}
