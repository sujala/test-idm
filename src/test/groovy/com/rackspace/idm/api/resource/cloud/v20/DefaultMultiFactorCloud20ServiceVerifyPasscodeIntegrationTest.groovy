package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCloudAuthenticationResponseToken
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionId
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatedMultiFactorAuthenticationService
import com.rackspace.idm.multifactor.providers.simulator.SimulatedMultiFactorAuthenticationService.SimulatedPasscode
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.joda.time.Minutes
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.Token
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.openstack.docs.identity.api.v2.UserDisabledFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import java.util.regex.Pattern

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern

/**
 * Tests verifying multifactor authentication. In lieu of using real Duo provider, which sends SMS messages, it subsitutes
 * a simulated mfa phone versificatino and authentication provider. It also modifies the config to use encryption keys for the sessionid located
 * in the classpath.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"
, "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatedMultiFactorAuthenticationService-context.xml"])
class DefaultMultiFactorCloud20ServiceVerifyPasscodeIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Pattern NON_STANDARD_MFA_PROVIDER_RESPONSE_ERROR_MSG_PATTERN = Pattern.compile(String.format(DefaultMultiFactorCloud20Service.NON_STANDARD_MFA_DENY_ERROR_MSG_FORMAT, "(.*)", "(.*)", "(.*)"))

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository

    @Autowired
    private LdapUserRepository userRepository

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification

    @Autowired
    private SimulatedMultiFactorAuthenticationService simulatedMultiFactorAuthenticationService

    @Autowired
    private BasicMultiFactorService multiFactorService

    @Autowired
    private EncryptedSessionIdReaderWriter encryptedSessionIdReaderWriter;

    @Autowired
    private DefaultMultiFactorCloud20Service defaultMultiFactorCloud20Service;

    @Autowired
    private UserDao userDao

    org.openstack.docs.identity.api.v2.User userAdmin
    String userAdminToken
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone
    VerificationCode constantVerificationCode

    /**
     * Override the grizzly start because we want to add additional context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        ClassPathResource resource = new ClassPathResource("/com/rackspace/idm/api/resource/cloud/v20/keys");
        resource.exists()
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatedMultiFactorAuthenticationService-context.xml " +
                "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml")
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        simulatedMultiFactorAuthenticationService.clearSmsPasscodeLog()
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id))  //remove duo profile
            deleteUserQuietly(userAdmin)
        }
    }

    @Unroll("Initial auth returns 401 with encrypted sessionId in www-authenticate header: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Initial auth returns 401 with encrypted sessionId in www-authenticate header"() {
        setup:
        setUpAndEnableMultiFactor()

        when:
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD, requestContentMediaType, acceptMediaType)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        //verify the provided sessionid can be decrypted
        SessionId plaintextSessionId = encryptedSessionIdReaderWriter.readEncoded(encryptedSessionId)

        then:
        response.getStatus() == HttpStatus.SC_UNAUTHORIZED
        encryptedSessionId != null
        //verify the provided sessionid can be decrypted and contains appropriate info
        plaintextSessionId.userId == userAdmin.id
        plaintextSessionId.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
        Minutes.minutesBetween(plaintextSessionId.getCreatedDate(), plaintextSessionId.getExpirationDate()).getMinutes() == defaultMultiFactorCloud20Service.getSessionIdLifetime()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Successful passcode authentication: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successful passcode authentication"() {
        setup:
        setUpAndEnableMultiFactor()
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, simulatedPasscode.ALLOW_ALLOW.passcode, requestContentMediaType, acceptMediaType)
        Token token
        if (acceptMediaType == MediaType.APPLICATION_XML_TYPE) {
            token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token

        } else {
            token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(mfaAuthResponse.getEntity(String))
        }

        then:
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        token.id != null
        token.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
        token.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)

        where:
        requestContentMediaType | acceptMediaType | simulatedPasscode
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_ALLOW
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_ALLOW
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_ALLOW
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_ALLOW
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_BYPASS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_BYPASS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_BYPASS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_BYPASS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_UNKNOWN
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_UNKNOWN
    }

    @Unroll("Fail with 401 when wrong passcode: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 401 when wrong passcode"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, simulatedPasscode.passcode, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, expectedFailureMessage)

        where:
        requestContentMediaType | acceptMediaType | simulatedPasscode | expectedFailureMessage
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_DENY |  DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_DENY | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_DENY | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_DENY | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG
    }

    @Unroll("Fail with 403 when account locked: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 403 when account locked"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, simulatedPasscode.passcode, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, expectedFailureMessage)
        def directoryUser = userDao.getUserById(userAdmin.id)
        directoryUser.multiFactorState == 'LOCKED'

        where:
        requestContentMediaType | acceptMediaType | simulatedPasscode | expectedFailureMessage
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_LOCKEDOUT | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_LOCKEDOUT | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_LOCKEDOUT | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_LOCKEDOUT | DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG
    }

    @Unroll("Fail with 401 when sessionid expired: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 401 when sessionid expired"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        //create an expired sessionId
        SessionId sessionId = encryptedSessionIdReaderWriter.readEncoded(encryptedSessionId)
        sessionId.expirationDate = new DateTime().minusMinutes(5)
        encryptedSessionId = encryptedSessionIdReaderWriter.writeEncoded(sessionId)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, SimulatedPasscode.ALLOW_ALLOW.passcode, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 500 when passcode denied for nonstandard reason: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 500 when passcode denied for nonstandard reason"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, simulatedPasscode.passcode, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponseWithMessagePattern(response, IdentityFault, HttpStatus.SC_INTERNAL_SERVER_ERROR, null)

        where:
        requestContentMediaType | acceptMediaType | simulatedPasscode
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_PROVIDER_FAILURE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_PROVIDER_FAILURE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_PROVIDER_FAILURE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_PROVIDER_FAILURE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_TIMEOUT
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_TIMEOUT
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_TIMEOUT
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_TIMEOUT
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_UNKNOWN
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.DENY_UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_UNKNOWN
    }

    /**
     * Returns 403 regardless of whether or not the provided passcode is valid
     * @return
     */
    @Unroll("Fail with 403 when user is disabled and valid sessionId provided: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 403 when user is disabled and valid sessionId provided"() {
        setup:
        String failureMessage = "User '" + userAdmin.username + "' is disabled.";
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        //disable the user
        User user = userRepository.getUserById(userAdmin.getId())
        user.setEnabled(false);
        userRepository.updateUserAsIs(user)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, simulatedPasscode.passcode, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UserDisabledFault, HttpStatus.SC_FORBIDDEN, failureMessage)

        where:
        requestContentMediaType | acceptMediaType | simulatedPasscode
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.DENY_DENY
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_ALLOW
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | SimulatedPasscode.ALLOW_BYPASS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE | SimulatedPasscode.ALLOW_UNKNOWN
    }

    def void setUpAndEnableMultiFactor() {
        setUpMultiFactorWithoutEnable()
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
    }

    def void setUpMultiFactorWithoutEnable() {
        setUpMultiFactorWithUnverifiedPhone()
        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
    }

    def void setUpMultiFactorWithUnverifiedPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
    }
}
