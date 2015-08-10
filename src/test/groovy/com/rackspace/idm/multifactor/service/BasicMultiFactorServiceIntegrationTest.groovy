package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.OTPDeviceDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.OTPDevice
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.identity.multifactor.domain.BasicPin
import com.rackspace.identity.multifactor.domain.Pin
import com.rackspace.identity.multifactor.providers.UserManagement
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import static org.mockito.Mockito.*

/**
 * Tests the multi-factor service with the exception of the MobilePhoneVerification dependency. The production class will sends texts through Duo Security, which
 * costs money per text. This is NOT desirable for a regression test that will continually run. Instead a simulated service is injected that will return a constant
 * PIN. Actually testing SMS texts should be performed via some other mechanism - in a manual fashion.
 *
 * Note - the simulated service for multifactor is only injected into the main spring context. Integration tests also spin up a grizzly container that
 * contains its own spring context. The grizzly container does NOT have the simulated service. This means these tests can NOT use REST API calls to
 * perform services that send SMS messages. Creating users and authenticating and such is fine.
 */
class BasicMultiFactorServiceIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    UserDao userRepository;

    @Autowired
    private UserManagement duoUserManagement

    @Autowired
    private OTPDeviceDao otpDeviceDao

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
        userRepository.deleteUser(finalUserAdmin)
        mobilePhoneRepository.deleteMobilePhone(retrievedPhone)
    }

    /**
     * This tests linking a phone number to a user
     *
     * @return
     */
    def "Add same phone to a user-admin while MFA disabled"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when: "add for first time"
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)

        then: "phone is added"
        //verify passed in object is updated with id, and phone number still as expected
        phone.getId() != null
        phone.getTelephoneNumber() == canonTelephoneNumber

        when: "add same phone again"
        MobilePhone secondPhone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then: "returned phone is same as before"
        //verify passed in object is updated with id, and phone number still as expected
        secondPhone.getId() == phone.getId()

        and: "user is linked to original phone"
        secondPhone.getId() == phone.getId()
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()

        cleanup:
        if (finalUserAdmin != null) {
            userRepository.deleteUser(finalUserAdmin)
        }
        if (phone != null) {
            mobilePhoneRepository.deleteMobilePhone(phone)
        }
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
        userAdmin.getMultiFactorDevicePin() == Constants.MFA_DEFAULT_PIN
        userAdmin.getMultiFactorDevicePinExpiration().after(testExpirationDate)
        !userAdmin.getMultiFactorDeviceVerified()
        !userAdmin.getMultifactorEnabled()

        cleanup:
        userRepository.deleteUser(userAdmin)
        mobilePhoneRepository.deleteMobilePhone(phone)
    }

    /**
     * Verifies that sending pin to phone sets appropriate information on user.
     *
     * @return
     */
    def "Sending second pin overwrites initial"() {
        setup:
        Pin initialPin = new BasicPin(Constants.MFA_DEFAULT_PIN)
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
        reset(mockMobilePhoneVerification.mock)
        when(mockMobilePhoneVerification.mock.sendPin(Mockito.anyObject())).thenReturn(updatedPin)

        when: "send a second pin code"
        multiFactorService.sendVerificationPin(userAdmin.getId(), retrievedPhone.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then: "old code is overwritten"
        userAdmin.getMultiFactorDevicePin() == updatedPin.getPin()
        userAdmin.getMultiFactorDevicePinExpiration().after(testExpirationDate)
        !userAdmin.getMultiFactorDeviceVerified()
        !userAdmin.getMultifactorEnabled()

        cleanup:
        mockMobilePhoneVerification.reset()
        userRepository.deleteUser(userAdmin)
        mobilePhoneRepository.deleteMobilePhone(phone)
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
        userRepository.updateUser(userAdmin)

        when:
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), retrievedPhone.getId(), new BasicPin(pin))
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        userAdmin.getMultiFactorMobilePhoneRsId() == retrievedPhone.getId()
        userAdmin.getMultiFactorDevicePinExpiration() == null  //date reset
        userAdmin.getMultiFactorDevicePin() == null
        userAdmin.getMultiFactorDeviceVerified()

        cleanup:
        userRepository.deleteUser(userAdmin)
        mobilePhoneRepository.deleteMobilePhone(phone)
    }

    /**
     * Verifies can enable multi-factor on an account. Ultimately this goes through whole set of services to add a phone,
     * send verification pin, verify, and finally enable.
     *
     * @return
     */
    def "Successfully enable multi-factor"() {
        setup:
        Pin verificationCode = new BasicPin(Constants.MFA_DEFAULT_PIN)

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
            userRepository.deleteUser(userAdmin)
        }
    }

    /**
     * Verifies can disable multi-factor on an account. Ultimately this goes through whole set of services to add a phone,
     * send verification pin, verify, enable, and finally disable.
     *
     * @return
     */
    def "Successfully disable multi-factor"() {
        setup:
        Pin verificationCode = new BasicPin(Constants.MFA_DEFAULT_PIN)

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
            userRepository.deleteUser(userAdmin)
        }
    }

    /**
     * Verifies can re-enable multi-factor on an account after disabling.
     *
     * @return
     */
    def "Successfully re-enable multi-factor after disabling"() {
        setup:
        Pin verificationCode = new BasicPin(Constants.MFA_DEFAULT_PIN)

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
            userRepository.deleteUser(userAdmin)
        }
    }

    /**
     * Verifies can remove multi-factor on an account. Ultimately this goes through whole set of services to add a phone,
     * send verification pin, verify, enable, and finally remove.
     *
     * @return
     */
    def "Successfully remove multi-factor"() {
        setup:
        Pin verificationCode = new BasicPin(Constants.MFA_DEFAULT_PIN)

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), verificationCode)
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))

        when:
        multiFactorService.removeMultiFactorForUser(userAdmin.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        StringUtils.isBlank(userAdmin.getExternalMultiFactorUserId())
        userAdmin.getMultiFactorMobilePhoneRsId() == null
        userAdmin.getMultiFactorDevicePinExpiration() == null
        userAdmin.getMultiFactorDevicePin() == null
        !userAdmin.isMultiFactorDeviceVerified()
        !userAdmin.isMultiFactorEnabled()

        cleanup:
        deleteUserQuietly(userAdmin)
    }

    /**
     * Verify removing multi-factor when duo returns error is still successful.
     *
     * @return
     */
    def "Successfully remove multi-factor when duo user does not exist"() {
        setup:
        Pin verificationCode = new BasicPin(Constants.MFA_DEFAULT_PIN)

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();

        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), verificationCode)
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))

        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        //remove the duo profile
        duoUserManagement.deleteUserById(userAdmin.getExternalMultiFactorUserId())

        /*
        change the profile to one that never existed. If you just delete a
        real duo profile two times in a row, the second attempt succeeds just like the first (returns 200 status).
        Setting the external id that never existed within Duo will result in a 404 being returned.
         */
        userAdmin.setExternalMultiFactorUserId(UUID.randomUUID().toString().replaceAll("-", ""))
        userRepository.updateUserAsIs(userAdmin)

        when:
        multiFactorService.removeMultiFactorForUser(userAdmin.getId())
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        StringUtils.isBlank(userAdmin.getExternalMultiFactorUserId())
        userAdmin.getMultiFactorMobilePhoneRsId() == null
        userAdmin.getMultiFactorDevicePinExpiration() == null
        userAdmin.getMultiFactorDevicePin() == null
        !userAdmin.isMultiFactorDeviceVerified()
        !userAdmin.isMultiFactorEnabled()

        cleanup:
        deleteUserQuietly(userAdmin)
    }

    /**
     * This tests linking an OTP device to a user
     *
     * @return
     */
    def "Add an OTP device to a user-admin"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        String name = getNormalizedRandomString()

        when:
        OTPDevice otpDevice = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)
        OTPDevice retrieved = multiFactorService.checkAndGetOTPDeviceFromUserById(finalUserAdmin.id, otpDevice.id)

        then:
        //verify passed in object is updated with id, and name still as expected
        otpDevice.getId() != null
        otpDevice.getName() == name
        otpDevice.getId() == retrieved.getId()
        retrieved.getName() == name

        cleanup:
        userRepository.deleteUser(finalUserAdmin)
    }

    /**
     * This tests linking an OTP device to a user
     *
     * @return
     */
    def "AddOTPDevice enforces max OTP Device limit per user limit"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.MAX_OTP_DEVICE_PER_USER_PROP, 2)

        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        String name = getNormalizedRandomString()

        when: "first device allowed"
        OTPDevice otpDevice1 = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)

        then:
        multiFactorService.checkAndGetOTPDeviceFromUserById(finalUserAdmin.id, otpDevice1.id) != null

        when: "second device allowed"
        OTPDevice otpDevice2 = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)

        then:
        multiFactorService.checkAndGetOTPDeviceFromUserById(finalUserAdmin.id, otpDevice2.id) != null

        when: "attempt to add 3rd device"
        multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)

        then: "get bad request"
        thrown(BadRequestException)

        when: "updated max limit and attempt to add 3rd device"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_OTP_DEVICE_PER_USER_PROP, 3)
        OTPDevice otpDevice3 = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)

        then: "can add"
        notThrown(BadRequestException)
        multiFactorService.checkAndGetOTPDeviceFromUserById(finalUserAdmin.id, otpDevice3.id) != null

        cleanup:
        userRepository.deleteUser(finalUserAdmin)
        reloadableConfiguration.reset()
    }

    def "Delete an unverified OTP device from a user-admin"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        String name = getNormalizedRandomString()
        OTPDevice otpDevice = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, name)

        when: "device not verified"
        multiFactorService.deleteOTPDeviceForUser(finalUserAdmin.id, otpDevice.id)

        then:
        multiFactorService.getOTPDeviceFromUserById(finalUserAdmin.id, otpDevice.id) == null

        cleanup:
        userRepository.deleteUser(finalUserAdmin)
    }

    def "Get a list of OTP devices on a user-admin"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        //create a verified device on user
        String otp1Name = getNormalizedRandomString()
        OTPDevice otpDevice1 = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, otp1Name)
        otpDevice1.multiFactorDeviceVerified = true
        otpDeviceDao.updateOTPDevice(otpDevice1)

        when: "list devices"
        def devices = multiFactorService.getOTPDevicesForUser(finalUserAdmin)

        then: "we find the verified device"
        devices.size() == 1
        devices.find() {it.name == otp1Name} != null

        when: "add an unverified device and list devices again"
        String otp2Name = getNormalizedRandomString()
        OTPDevice otpDevice2 = multiFactorService.addOTPDeviceToUser(finalUserAdmin.id, otp2Name)
        def multipleDevices = multiFactorService.getOTPDevicesForUser(finalUserAdmin)

        then:
        multipleDevices.size() == 2
        multipleDevices.find() {it.name == otp1Name} != null
        multipleDevices.find() {it.name == otp2Name} != null

        cleanup:
        try { otpDeviceDao.deleteOTPDevice(otpDevice1) } catch (Exception e) {}
        try { otpDeviceDao.deleteOTPDevice(otpDevice2) } catch (Exception e) {}
        try { userRepository.deleteUser(finalUserAdmin) } catch (Exception e) {}
    }

}
