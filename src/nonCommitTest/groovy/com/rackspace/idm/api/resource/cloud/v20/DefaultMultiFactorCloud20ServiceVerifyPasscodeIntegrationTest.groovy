package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCloudAuthenticationResponseToken
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
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
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern

import static org.mockito.Mockito.*;

/**
 * Tests verifying multifactor authentication. In lieu of using real Duo provider, which sends SMS messages, it substitutes
 * a simulated mfa phone verification and authentication provider.
 */
class DefaultMultiFactorCloud20ServiceVerifyPasscodeIntegrationTest extends RootConcurrentIntegrationTest {

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

    @Autowired
    TokenFormatSelector tokenFormatSelector

    @Autowired
    private ScopeAccessService scopeAccessService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    DefaultAETokenService tokenService

    @Autowired
    IdentityUserService identityUserService

    org.openstack.docs.identity.api.v2.User userAdmin
    String userAdminToken
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone
    VerificationCode constantVerificationCode
    OTPDevice responseOTP

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        utils.resetServiceAdminToken()
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
    }

    def cleanup() {
        try { multiFactorService.removeMultiFactorForUser(userAdmin.id) } catch (Exception e) {}
        deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "Initial auth returns 401 with encrypted sessionId and factor in in www-authenticate header: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType; factorType=#factorType"() {
        setup:
        setUpAndEnableMultiFactor(factorType)

        when:
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD, requestContentMediaType, acceptMediaType)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        //verify the provided sessionid can be decrypted
        def decryptedSessionId = tokenService.unmarshallToken(encryptedSessionId)
        DateTime createdDatetime = new DateTime(decryptedSessionId.createTimestamp)
        DateTime expiryDatetime = new DateTime(decryptedSessionId.accessTokenExp)
        def expectedFactor = factorType == FactorTypeEnum.OTP ? AuthenticatedByMethodEnum.OTPPASSCODE.getValue() : AuthenticatedByMethodEnum.PASSCODE.getValue()

        then: "sessionId can be decrypted"
        response.getStatus() == HttpStatus.SC_UNAUTHORIZED
        encryptedSessionId != null
        decryptedSessionId.userRsId == userAdmin.id
        decryptedSessionId.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
        Math.abs(Minutes.minutesBetween(createdDatetime, expiryDatetime).getMinutes() - identityConfig.getReloadableConfig().getMfaSessionIdLifetime()) <= 1

        and: "auth header shows appropriate value"
        utils.extractFactorFromWwwAuthenticateHeader(wwwHeader) == expectedFactor

        cleanup:
        reloadableConfiguration.reset()

        where:
        requestContentMediaType | acceptMediaType | factorType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | FactorTypeEnum.OTP
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.OTP
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE | FactorTypeEnum.SMS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.SMS
    }

    @Unroll("Successful passcode authentication: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType; mfaDecisionReason: #mfaDecisionReason; factorType=#factorType; restrictedTokenSessionId=#restrictedTokenSessionId")
    def "Successful passcode authentication"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, restrictedTokenSessionId)
        setUpAndEnableMultiFactor(factorType)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def passcode
        if(factorType == FactorTypeEnum.SMS) {
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
        if (factorType == FactorTypeEnum.SMS) {
            token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSCODE.getValue())
            token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        } else {
            token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.OTPPASSCODE.getValue())
            token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        }

        cleanup:
        reloadableConfiguration.reset()

        where:
        requestContentMediaType         | acceptMediaType                 | mfaDecisionReason                       | factorType          | restrictedTokenSessionId
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.ALLOW   | FactorTypeEnum.SMS  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.ALLOW   | FactorTypeEnum.SMS  | false

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.BYPASS  | FactorTypeEnum.SMS  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.BYPASS  | FactorTypeEnum.SMS  | false

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN | FactorTypeEnum.SMS  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN | FactorTypeEnum.SMS  | false

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | null                                    | FactorTypeEnum.OTP  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | null                                    | FactorTypeEnum.OTP  | false

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | null                                    | FactorTypeEnum.OTP  | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.ALLOW   | FactorTypeEnum.SMS  | true
    }

    @Unroll
    def "Successful OTP passcode authentication when using issueRestrictedTokenSessionId:#issueRestrictedTokenSessionId"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, issueRestrictedTokenSessionId)

        userAdminToken = authenticate(userAdmin.username)
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)

        when: "initially authenticate"
        String sessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        then: "get the proper sessionId"
        if (issueRestrictedTokenSessionId) {
            scopeAccessService.unmarshallScopeAccess(sessionId) != null
        } else {
            encryptedSessionIdReaderWriter.readEncoded(sessionId) != null
        }

        when: "auth w/ passcode"
        def passcode = getOTPCode()
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, passcode)
        Token token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token

        then: "get a 'real' token back"
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        token.id != null
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.OTPPASSCODE.getValue())
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        cleanup:
        reloadableConfiguration.reset()

        where:
        issueRestrictedTokenSessionId << [true, false]
    }

    @Unroll
    def "Successful OTP passcode authentication when 'old' sessionId is used compared to what's currently generated; issueRestrictedTokenSessionId:#issueRestrictedTokenSessionId"() {
        setup:
        //get the sessionId in Format A
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, !sessionIdInLegacyFormat)

        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)

        when: "initially authenticate"
        String sessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        then: "get the proper sessionId"
        if (!sessionIdInLegacyFormat) {
            scopeAccessService.unmarshallScopeAccess(sessionId) != null
        } else {
            encryptedSessionIdReaderWriter.readEncoded(sessionId) != null
        }

        when: "auth w/ passcode switching to new format"
        //set new session ids to be generated in format B
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, sessionIdInLegacyFormat)
        def passcode = getOTPCode()
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, passcode)
        Token token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token

        then: "get a 'real' token back"
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        token.id != null
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.OTPPASSCODE.getValue())
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        cleanup:
        reloadableConfiguration.reset()

        where:
        sessionIdInLegacyFormat | _
        true | _
        false | _
    }

    @Unroll
    def "Can not auth with passcode using regular token as sessionId regardless of sessionIdFormat. issueRestrictedTokenSessionId: #issueRestrictedTokenSessionId"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, issueRestrictedTokenSessionId)
        def apiToken = utils.getTokenFromApiKeyAuth(userAdmin.username, DEFAULT_APIKEY)

        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)

        when: "auth w/ passcode switching to new format"
        def passcode = getOTPCode()
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(apiToken, passcode)

        then: "get error"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(mfaAuthResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, IdmAssert.PATTERN_ALL)

        cleanup:
        reloadableConfiguration.reset()

        where:
        issueRestrictedTokenSessionId | _
        true | _
        false | _
    }

    def "Can not auth with token using restricted token session id"() {
        setup:
        def domainId = utils.createDomain()
        def users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, true)
        userAdminToken = authenticate(userAdmin.username)
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String sessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        assert sessionId != null

        when: "try to auth w/ token using restricted token session id"
        def authResponse = cloud20.authenticateTokenAndTenant(sessionId, userAdmin.domainId)

        then: "get 403"
        assertOpenStackV2FaultResponse(authResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)

        cleanup:
        reloadableConfiguration.reset()
    }

    def "Successful mobile passcode authentication with AE token sets OTP_PASSCODE auth by"() {
        setup:
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        userAdmin.setTokenFormat(TokenFormatEnum.AE)
        utils.updateUser(userAdmin)
        def response = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        when:
        def passcode = getOTPCode()
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, passcode)
        AuthenticateResponse responseEntity = mfaAuthResponse.getEntity(AuthenticateResponse).value
        Token token = responseEntity.token

        then:
        mfaAuthResponse.getStatus() == HttpStatus.SC_OK
        token.id != null
        tokenFormatSelector.formatForExistingToken(token.id) == TokenFormat.AE
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.OTPPASSCODE.getValue())
        token.getAuthenticatedBy().credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
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
        directoryUser.multiFactorFailedAttemptCount == 1

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll("Fail with 401 when sessionid expired: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 401 when sessionid expired"() {
        setup:
        setUpAndEnableMultiFactor()
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        //create an expired sessionId
        def decryptedSessionId = tokenService.unmarshallToken(encryptedSessionId)
        decryptedSessionId.accessTokenExp = new Date() - 1
        def user = identityUserService.getEndUserById(userAdmin.getId())
        def reEncryptedSessionId = tokenService.marshallTokenForUser(user, decryptedSessionId)

        when:
        def response = cloud20.authenticateMFAWithSessionIdAndPasscode(reEncryptedSessionId, "1234", requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG)

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
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

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.TIMEOUT
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.TIMEOUT

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | MfaAuthenticationDecisionReason.UNKNOWN
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | MfaAuthenticationDecisionReason.UNKNOWN
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
        User user = userDao.getUserById(userAdmin.getId())
        user.setEnabled(false);
        userDao.updateUserAsIs(user)

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
    def "Check if local locking is working: requestContentType: #requestContentMediaType; acceptMediaType=#acceptMediaType; factorType=#factorType"() {
        setup:
        def maxAttempts = 3
        def autoUnlockSeconds = 1800
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, maxAttempts)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, autoUnlockSeconds)

        setUpAndEnableMultiFactor(factorType)
        if (factorType == FactorTypeEnum.SMS) {
            //need to mock the response as we don't use Duo for real
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        }

        String encryptedSessionId = getSessionId(userAdmin.username)
        def response, directoryUser

        when:
        for (int i=0; i<maxAttempts; i++) {
            response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        }
        directoryUser = userDao.getUserById(userAdmin.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG)
        directoryUser.multiFactorFailedAttemptCount == maxAttempts
        directoryUser.multiFactorLastFailedTimestamp != null

        when: "unlock user"
        response = cloud20.updateMultiFactorSettings(utils.getIdentityAdminToken(), userAdmin.id, v2Factory.createMultiFactorSettings(null, true))
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "user is unlocked"
        response.status == 204
        directoryUser.multiFactorFailedAttemptCount == null
        directoryUser.multiFactorLastFailedTimestamp == null
        0 * mockUserManagement.unlockUser(userAdmin.id)

        when: "fail passcode auth the number of times takes to cause account to be locked"
        for (int i=0; i<maxAttempts; i++) {
            //last attempt should lock the account
            response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        }
        org.openstack.docs.identity.api.v2.User retrievedUserAdmin = utils.getUserById(userAdmin.id)
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "get 403 and state of user is locked"
        response.status == 403
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.LOCKED
        directoryUser.multiFactorFailedAttemptCount == maxAttempts

        when: "invalid auth again"
        cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong", requestContentMediaType, acceptMediaType)
        retrievedUserAdmin = utils.getUserById(userAdmin.id)
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "still get 403 and state of user is locked"
        response.status == 403
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.LOCKED

        and: "failed attempt count not increased"
        directoryUser.multiFactorFailedAttemptCount == maxAttempts

        when: "list users for domain when user is locked"
        List<org.openstack.docs.identity.api.v2.User> users = utils.getUsersByDomainId(userAdmin.getDomainId())
        retrievedUserAdmin = users.find() {it.id == userAdmin.id}

        then: "list returns user as locked"
        retrievedUserAdmin != null
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.LOCKED

        when: "hack the last failed attempt on local locking to simulate auto-unlocking and get user again"
        directoryUser = userDao.getUserById(userAdmin.id)
        directoryUser.setMultiFactorLastFailedTimestamp(new DateTime().minusSeconds(autoUnlockSeconds + 5).toDate()) // set to less than auto-unlock
        userDao.updateUserAsIs(directoryUser)
        retrievedUserAdmin = utils.getUserById(userAdmin.id)

        then: "user is no longer locked"
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.ACTIVE

        when: "list users for domain when user is active"
        users = utils.getUsersByDomainId(userAdmin.getDomainId())
        retrievedUserAdmin = users.find() {it.id == userAdmin.id}

        then: "list returns user as active"
        retrievedUserAdmin != null
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.ACTIVE

        when: "invalid attempt again"
        cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "invalid attempts is rolled back to 1"
        directoryUser.multiFactorFailedAttemptCount == 1
        directoryUser.multiFactorLastFailedTimestamp != null

        where:
        requestContentMediaType         | acceptMediaType                 | factorType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.SMS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.SMS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.OTP
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.OTP
    }

    @Unroll
    def "initial pwd auth denied if mfa enabled and locally locked: factorType=#factorType"() {
        setup:
        def maxAttempts = 3
        def autoUnlockSeconds = 1800
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, maxAttempts)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, autoUnlockSeconds)

        setUpAndEnableMultiFactor(factorType)
        if (factorType == FactorTypeEnum.SMS) {
            //need to mock the response as we don't use Duo for real
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        }

        String encryptedSessionId = getSessionId(userAdmin.username)
        def response, directoryUser

        //lock the account
        for (int i = 0; i < maxAttempts; i++) {
            response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
        }
        directoryUser = userDao.getUserById(userAdmin.id)
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG)
        directoryUser.multiFactorFailedAttemptCount == maxAttempts
        directoryUser.multiFactorLastFailedTimestamp != null

        when: "perform first factor auth again"
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)

        then: "get 401 without sessionId"
        oneFactorResponse.status == 401
        oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE) == null

        cleanup:
        reloadableConfiguration.reset()

        where:
        factorType | _
        FactorTypeEnum.OTP  | _
        FactorTypeEnum.SMS | _
    }

        @Unroll
    def "Check local locking resets failed counter when ttl of last failure passes: factorType=#factorType"() {
        setup:
        def maxAttempts = 3
        def loginFailureTTL = 1800
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, maxAttempts)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, loginFailureTTL)

        setUpAndEnableMultiFactor(factorType)
        if (factorType == FactorTypeEnum.SMS) {
            //need to mock the response as we don't use Duo for real
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        }

        String encryptedSessionId = getSessionId(userAdmin.username)
        def response, directoryUser

        when: "fail login first time"
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "records single failed login"
        directoryUser.multiFactorFailedAttemptCount == 1

        when: "fail login second time"
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
        directoryUser = userDao.getUserById(userAdmin.id)

        then: "records single failed login"
        directoryUser.multiFactorFailedAttemptCount == 2

        when: "hack the last failed attempt to simulate TTL passing and fail login a third time"
        directoryUser = userDao.getUserById(userAdmin.id)
        directoryUser.setMultiFactorLastFailedTimestamp(new DateTime().minusSeconds(loginFailureTTL).toDate()) //set to now() - TTL
        userDao.updateUserAsIs(directoryUser)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "wrong")
        directoryUser = userDao.getUserById(userAdmin.id)
        org.openstack.docs.identity.api.v2.User retrievedUserAdmin = utils.getUserById(userAdmin.id)

        then: "invalid attempts is rolled back to 1 and user is not locked"
        directoryUser.multiFactorFailedAttemptCount == 1
        retrievedUserAdmin.getMultiFactorState() == MultiFactorStateEnum.ACTIVE

        where:
        factorType | _
        FactorTypeEnum.SMS | _
    }

    def String getSessionId(username, pwd = DEFAULT_PASSWORD) {
        def oneFactorResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)
        String wwwHeader = oneFactorResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        return encryptedSessionId;
    }

    def void setUpAndEnableMultiFactor(def factorType = FactorTypeEnum.SMS) {
        setUpMultiFactorWithoutEnable(factorType)
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
    }

    def void setUpMultiFactorWithoutEnable(def factorType = FactorTypeEnum.SMS) {
        if (factorType == FactorTypeEnum.SMS) {
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
