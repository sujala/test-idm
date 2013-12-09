package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest

import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.idm.multifactor.domain.BasicPin
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared

/**
 * This is a manual integration test between the idm DefaultMultiFactorService, ldap, and Duo Security that must not be part of any automated run. It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 *
 * <b>The test is a Manual Test (MT) with the @Ignore annotation because it causes a REAL SMS message to be sent to a REAL phone which costs REAL money for rackspace.</b>
 */
@Ignore
class BasicMultiFactorServiceMT extends RootConcurrentIntegrationTest {

    /**
     * This property must be populated with a valid telephone number (e.g. 5126667777) before running this test.
     */
    private static final String TEST_PHONE = "5127759583";

    @Shared Phonenumber.PhoneNumber phoneToText = PhoneNumberUtil.getInstance().parse(TEST_PHONE, "US")

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    /**
     * This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "Add a phone to user-admin and verify"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = phoneToText;
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        //STEP 1: Add phone to user
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)

        //STEP 2: Send PIN
        multiFactorService.sendVerificationPin(userAdminOpenStack.getId(), phone.getId())

        //STEP 3: Verify PIN
        //need to get the user to get the pin
        User tempUser = userRepository.getUserById(userAdminOpenStack.getId())
        assert tempUser.getMultiFactorDevicePin() != null

        multiFactorService.verifyPhoneForUser(userAdminOpenStack.getId(), phone.getId(), new BasicPin(tempUser.getMultiFactorDevicePin()))

        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify phone
        finalPhone.getId() != null
        finalPhone.getTelephoneNumber() == canonTelephoneNumber

        //verify user state
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == finalPhone.getId()
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:
        if (finalUserAdmin != null) userRepository.deleteObject(finalUserAdmin)
        if (phone != null) mobilePhoneRepository.deleteObject(phone)
    }

}
