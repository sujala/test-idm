package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.identity.multifactor.providers.duo.config.apache.ApacheConfigDuoSecurityConfig
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.helpers.ApacheTestConfigLoader
import com.rackspace.idm.helpers.ApacheTestConfigurationWrapper
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.identity.multifactor.domain.BasicPin
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert
import spock.lang.Shared

/**
 * This is an integration test between the idm DefaultMultiFactorService, ldap, and Duo Security that will use Duo
 * telephony credits. It must not be part of any automated run. It expects the appropriate IDM configuration to be
 * loadable via the standard PropertyFileConfiguration. This method of loading the configuration information is used to
 * allow the sensitive integration keys to be encrypted via the standard mechanism in the idm.secrets file.
 *
 * <b>The tests will be ignored unless the property 'test.run.telephony' is set to true (default is false) and the property
 * 'test.phone.number' is set (default - empty string).  It causes a REAL SMS message to be sent to the specified phone which costs money</b>
 */
class BasicMultiFactorServiceTelephonyIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    private ApacheTestConfigurationWrapper configWrapper;

    private String testPhone;

    private Phonenumber.PhoneNumber standardizedTestPhone;

    private boolean originalTelephonyEnabledPropValue = false

    def setup() {
        configWrapper = new ApacheTestConfigurationWrapper(globalConfig)

        //ignore tests unless telephony tests are enabled
        org.junit.Assume.assumeTrue("Telephony tests are ignored. Skipping.", configWrapper.getRunTelephonyTests())

        Assert.hasText(configWrapper.getTestPhoneNumber(), String.format("The test property '%s' must be provided to run telephony tests", ApacheTestConfigurationWrapper.TEST_PHONE_NUMBER_PROP_NAME))

        //enable telephony as these tests require sending SMS
        originalTelephonyEnabledPropValue = configWrapper.getConfiguration().getBoolean(ApacheConfigDuoSecurityConfig.PROP_NAME_DUO_TELEPHONY_ENABLED, false);
        configWrapper.getConfiguration().setProperty(ApacheConfigDuoSecurityConfig.PROP_NAME_DUO_TELEPHONY_ENABLED, true);

        testPhone = configWrapper.getTestPhoneNumber();
        standardizedTestPhone = configWrapper.getStandardizedTestPhoneNumber()
    }

    def cleanup() {
        if (configWrapper.getRunTelephonyTests()) {
            configWrapper.getConfiguration().setProperty(ApacheConfigDuoSecurityConfig.PROP_NAME_DUO_TELEPHONY_ENABLED, originalTelephonyEnabledPropValue);
        }
    }

    /**
     * This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "Add a phone to user-admin and verify"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = standardizedTestPhone;
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
