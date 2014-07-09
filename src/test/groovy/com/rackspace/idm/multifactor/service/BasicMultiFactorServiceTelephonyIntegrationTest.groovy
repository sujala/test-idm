package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
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
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.util.Assert
import spock.lang.Shared
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

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
        if (finalPhone != null) mobilePhoneRepository.deleteObject(finalPhone)
    }

    /**
     * Those tests verifies bypass code creating and usage.
     */
    def bypassSetup(userAdminOpenStack) {
        Phonenumber.PhoneNumber telephoneNumber = standardizedTestPhone;
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

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

        //STEP 4: enable multifactor
        MultiFactor enableSettings = v2Factory.createMultiFactorSettings(true, null)
        multiFactorService.updateMultiFactorSettings(userAdminOpenStack.getId(), enableSettings)
        finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        return [finalUserAdmin, finalPhone]
    }

    def bypassTearDown(User finalUserAdmin, MobilePhone finalPhone) {
        if (finalUserAdmin != null) userRepository.deleteObject(finalUserAdmin)
        if (finalPhone != null) mobilePhoneRepository.deleteObject(finalPhone)
    }

    def createBypassRequest(seconds, mediaType) {
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            return new JsonBuilder(["RAX-AUTH:bypassCodes": ["validityDuration": "PT" + seconds + "S"]]).toString()
        } else {
            final BypassCodes request = new BypassCodes()
            DatatypeFactory factory = DatatypeFactory.newInstance();
            request.validityDuration = factory.newDuration(seconds * 1000)
            return request;
        }
    }

    def parseCodes(response, mediaType) {
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            def body = response.getEntity(String.class)
            def slurper = new JsonSlurper().parseText(body)
            return slurper.'RAX-AUTH:bypassCodes'.codes
        } else {
            return response.getEntity(BypassCodes.class).getCodes()
        }
    }

    @Unroll
    def "Get an user-admin bypass code: #mediaType, #tokenType"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        def userToken = utils.getToken(userAdminOpenStack.getUsername())

        def setupResults = bypassSetup(userAdminOpenStack)
        User finalUserAdmin = setupResults[0]
        MobilePhone finalPhone = setupResults[1]

        def request = createBypassRequest(10, mediaType)
        def adminToken = tokenType == 'identity' ? utils.getIdentityAdminToken() : utils.getServiceAdminToken()

        when: "request bypass code"
        def response = cloud20.getBypassCodes(adminToken, userAdminOpenStack.getId(), request, mediaType, mediaType)
        def codes = parseCodes(response, mediaType)

        then: "gets one code, and 200 OK"
        response.getStatus() == 200
        codes.size() == 1

        when: "use bypass code"
        def verify1 = multiFactorService.verifyPasscode(userAdminOpenStack.getId(), codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code twice"
        def verify2 = multiFactorService.verifyPasscode(userAdminOpenStack.getId(), codes[0])

        then: "get denied"
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == "Incorrect passcode. Please try again."

        when: "user tries to create bypass code to himself"
        def selfservice = cloud20.getBypassCodes(userToken, userAdminOpenStack.getId(), request, mediaType, mediaType)

        then: "user get a bad request"
        selfservice.getStatus() == 401

        cleanup:
        bypassTearDown(finalUserAdmin, finalPhone)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE] * 2 + [MediaType.APPLICATION_JSON_TYPE] * 2
        tokenType << ['service', 'identity'] * 2
    }

    /**
     * This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "verify unlock functionality"() {
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

        when:
        //STEP 4: enable multifactor
        MultiFactor enableSettings = v2Factory.createMultiFactorSettings(true, null)
        multiFactorService.updateMultiFactorSettings(userAdminOpenStack.getId(), enableSettings)
        finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        finalUserAdmin.getMultifactorEnabled()
        finalUserAdmin.getExternalMultiFactorUserId() != null

        when:
        //STEP 5: verify unlock on an unlocked user works
        MultiFactor unlockSettings = v2Factory.createMultiFactorSettings(null, true)
        multiFactorService.updateMultiFactorSettings(userAdminOpenStack.getId(), unlockSettings)

        then:
        notThrown(Exception)

        when:
        //STEP 6: lock the user by sending in wrong passcode twice
        def verify1 = multiFactorService.verifyPasscode(userAdminOpenStack.getId(), "0000000")
        def verify2 = multiFactorService.verifyPasscode(userAdminOpenStack.getId(), "0000000")

        then:
        // verify the user is locked
        verify1.decision == MfaAuthenticationDecision.DENY
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == com.rackspace.identity.multifactor.providers.duo.exception.DuoErrorCodes.ACCOUNT_LOCKED_DUE_TO_FAILED_ATTEMPTS

        when:
        //STEP 7: unlock the locked user
        multiFactorService.updateMultiFactorSettings(userAdminOpenStack.getId(), unlockSettings)

        then:
        notThrown(Exception)

        cleanup:
        if (finalUserAdmin != null) userRepository.deleteObject(finalUserAdmin)
        if (finalPhone != null) mobilePhoneRepository.deleteObject(finalPhone)
    }
}
