package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCloudAuthenticationResponseToken
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionId
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
import org.joda.time.DateTime
import org.joda.time.Minutes
import org.openstack.docs.identity.api.v2.AuthenticateResponse
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

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern

import static org.mockito.Mockito.*;

/**
 * Tests verifying multifactor authentication. In lieu of using real Duo provider, which sends SMS messages, it subsitutes
 * a simulated mfa phone versificatino and authentication provider. It also modifies the config to use encryption keys for the sessionid located
 * in the classpath.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"
, "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml"])
class DefaultMultiFactorCloud20ServiceVerifyPasscodeIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    private LdapUserRepository userRepository

    @Autowired
    private BasicMultiFactorService multiFactorService

    @Autowired
    private EncryptedSessionIdReaderWriter encryptedSessionIdReaderWriter;

    @Autowired
    private DefaultMultiFactorCloud20Service defaultMultiFactorCloud20Service;

    @Autowired
    private UserDao userDao

    @Autowired
    private OTPHelper otpHelper

    org.openstack.docs.identity.api.v2.User userAdmin
    String userAdminToken
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone
    VerificationCode constantVerificationCode
    OTPDevice responseOTP

    /**
     * Override the grizzly start because we want to add additional context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        ClassPathResource resource = new ClassPathResource("/com/rackspace/idm/api/resource/cloud/v20/keys");
        resource.exists()
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
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
    }

    def cleanup() {
        try { multiFactorService.removeMultiFactorForUser(userAdmin.id) } catch (Exception e) {}
        deleteUserQuietly(userAdmin)
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
        setUpAndEnableMultiFactor(phone)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def passcode
        if(phone) {
            passcode = "1234"
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, mfaDecisionReason, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        } else {
            passcode = getOTPCode()
        }
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, passcode, requestContentMediaType, acceptMediaType)
        Token token
        if (acceptMediaType == MediaType.APPLICATION_XML_TYPE) {
            token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token
        } else {
            token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(mfaAuthResponse.getEntity(String))
        }

        then:
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        token.id != null
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSCODE.toString())
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.toString())

        where:
        requestContentMediaType         | acceptMediaType                 | mfaDecisionReason                       | phone
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.ALLOW   | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.ALLOW   | true
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.ALLOW   | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.ALLOW   | true

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.BYPASS  | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.BYPASS  | true
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.BYPASS  | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.BYPASS  | true

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN | true
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN | true

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | null                                    | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | null                                    | false
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | null                                    | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | null                                    | false
    }

    @Unroll("Fail with 401 when wrong passcode: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 401 when wrong passcode"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG)

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 403 when account locked: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 403 when account locked"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.LOCKEDOUT, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG)
        def directoryUser = userDao.getUserById(userAdmin.id)
        directoryUser.multiFactorState == 'LOCKED'

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
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
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG)

        where:
        requestContentMediaType         | acceptMediaType
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
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, mfaDecisionReason, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponseWithMessagePattern(response, IdentityFault, HttpStatus.SC_INTERNAL_SERVER_ERROR, null)

        where:
        requestContentMediaType         | acceptMediaType                 | mfaDecisionReason
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.PROVIDER_FAILURE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.PROVIDER_FAILURE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.PROVIDER_FAILURE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.PROVIDER_FAILURE

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.TIMEOUT
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.TIMEOUT
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.TIMEOUT
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.TIMEOUT

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN
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
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(mfaDecision, mfaDecisionReason, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UserDisabledFault, HttpStatus.SC_FORBIDDEN, failureMessage)

        where:
        requestContentMediaType         | acceptMediaType                 | mfaDecision                     | mfaDecisionReason
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.DENY
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.ALLOW
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.BYPASS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.UNKNOWN
    }

    def "Verify that after disabling multifactor that new auth token does NOT have AUTHENTICATED_BY_PASSCODE"() {
        setup:
        setUpAndEnableMultiFactor()
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234")
        Token mfaToken = mfaAuthResponse.getEntity(AuthenticateResponse).value.token

        then:
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        mfaToken.id != null
        mfaToken.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
        mfaToken.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)

        when: "Removing multifactor for user and re-authenticating"
        multiFactorService.removeMultiFactorForUser(userAdmin.id)
        def tokenResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        Token regularToken = tokenResponse.getEntity(AuthenticateResponse).value.token

        then:
        tokenResponse.status == HttpStatus.SC_OK
        regularToken.id != null
        !regularToken.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
        regularToken.getAuthenticatedBy().credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
        mfaToken.id != regularToken.id
    }

    @Unroll
    def "Check if local locking is working: requestContentType: #requestContentMediaType; acceptMediaType=#acceptMediaType; sms=#isSMS"() {
        setup:
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty("feature.multifactor.locking.enabled", true)
        reloadableConfiguration.setProperty("feature.multifactor.locking.attempts.maximumNumber", 3)
        reloadableConfiguration.setProperty("feature.multifactor.locking.expirationInSeconds", 1800)

        setUpAndEnableMultiFactor(isSMS)
        if (isSMS) {
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        }

        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def response, directoryUser

        when:
        for (int i=0; i<4; i++) {
            response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        }
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG)
        directoryUser.multiFactorState == 'LOCKED'
        directoryUser.multiFactorFailedAttemptCount != null
        directoryUser.multiFactorLastFailedTimestamp != null

        when:
        response = cloud20.updateMultiFactorSettings(utils.getIdentityAdminToken(), userAdmin.id, v2Factory.createMultiFactorSettings(null, true))
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        response.status == 204
        directoryUser.multiFactorState == 'ACTIVE'
        directoryUser.multiFactorFailedAttemptCount == null
        directoryUser.multiFactorLastFailedTimestamp == null
        0 * mockUserManagement.unlockUser(userAdmin.id)

        // TODO: Uncomment for "[CIDMDEV-4952] Multi-Factor Mobile Passcode :: Automatic Account Unlock"
//        when:
//        for (int i=0; i<4; i++) {
//            response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
//        }
//
//        then:
//        response.status == 403
//
//        when: // wait for the expiration on local locking
//        directoryUser = userDao.getUserById(userAdmin.id)
//        directoryUser.setMultiFactorLastFailedTimestamp(new Date(0)) // force expiration
//        userDao.updateUserAsIs(directoryUser)
//
//        cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
//        directoryUser = userDao.getUserById(userAdmin.id)
//
//        then: // check auto-unlock (roll back the counter to 1)
//        directoryUser.multiFactorFailedAttemptCount == 1
//        directoryUser.multiFactorLastFailedTimestamp != null

        where:
        requestContentMediaType         | acceptMediaType                 | isSMS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | true
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | false
    }

    @Unroll
    def "Test local locking feature flag: #requestContentMediaType; acceptMediaType=#acceptMediaType"() {
        setup:
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty("feature.multifactor.locking.enabled", false)

        setUpAndEnableMultiFactor()

        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def response, directoryUser, mfaServiceResponse

        when:
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG)
        directoryUser.multiFactorFailedAttemptCount == null
        directoryUser.multiFactorLastFailedTimestamp == null

        when:
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.LOCKEDOUT, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG)
        directoryUser.multiFactorFailedAttemptCount == null
        directoryUser.multiFactorLastFailedTimestamp == null
        directoryUser.multiFactorState == 'LOCKED'

        when:
        cloud20.updateMultiFactorSettings(utils.getIdentityAdminToken(), userAdmin.id, v2Factory.createMultiFactorSettings(null, true))
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        reloadableConfiguration.setProperty("feature.multifactor.locking.enabled", true)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_GENERIC_ERROR_MSG)
        directoryUser.multiFactorFailedAttemptCount != null
        directoryUser.multiFactorLastFailedTimestamp != null

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def void setUpAndEnableMultiFactor(def phone = true) {
        setUpMultiFactorWithoutEnable(phone)
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
    }

    def void setUpMultiFactorWithoutEnable(def phone = true) {
        if (phone) {
            setUpMultiFactorWithUnverifiedPhone()
            utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
            constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
            utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
        } else {
            setUpOTPDevice()
            utils.verifyOTPDevice(userAdminToken, userAdmin.id, responseOTP.id, getOTPVerificationCode())
        }
    }

    def void setUpMultiFactorWithUnverifiedPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
    }

    def void setUpOTPDevice() {
        OTPDevice device = new OTPDevice()
        device.setName("test-" + UUID.randomUUID().toString().replaceAll("-", ""))
        responseOTP = utils.addOTPDevice(userAdminToken, userAdmin.id, device)
    }

    def VerificationCode getOTPVerificationCode() {
        final VerificationCode verificationCode = new VerificationCode()
        verificationCode.code = getOTPCode()
        return verificationCode
    }

    def getOTPCode() {
        def secret = Base32.decode(URLEncodedUtils.parse(new URI(responseOTP.getKeyUri()), "UTF-8").find { it.name == 'secret' }.value)
        return otpHelper.TOTP(secret)
    }
}
