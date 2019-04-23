package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

import static org.mockito.Mockito.*;

/**
 * Tests verifying multifactor authentication. In lieu of using real Duo provider, which sends SMS messages, it subsitutes
 * a simulated mfa phone verification and authentication provider. It also modifies the config to use encryption keys for the sessionid located
 * in the classpath.
 */
class MultiFactorAuthenticationIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    UserDao userRepository

    @Autowired
    private BasicMultiFactorService multiFactorService

    @Autowired
    private DefaultMultiFactorCloud20Service defaultMultiFactorCloud20Service;

    @Autowired
    private OTPHelper otpHelper

    @Autowired
    private ScopeAccessService scopeAccessService

    @Autowired
    private IdentityConfig identityConfig

    def userAdmin, users
    String userAdminToken
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone
    OTPDevice responseOTP
    def constantVerificationCode = new VerificationCode().with {
        it.code = Constants.MFA_DEFAULT_PIN
        it
    }

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        def domainId = utils.createDomain()
        (userAdmin,users) = utils.createUserAdminWithTenants(domainId)
        userAdminToken = authenticate(userAdmin.username)
    }

    def cleanup() {
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    /**
     * This tests authenticating with API key on an mfa enabled account
     *
     * @return
     */
    @Unroll
    def "When enable multifactor appropriate v1.0/v1.1/v2.0 api key auth behavior: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        setUpAndEnableMultiFactor(factorType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "try to auth via 1.0 with correct API key"
        def auth10ResponseApi = cloud10.authenticate(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())

        then: "receive 204"
        auth10ResponseApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_NO_CONTENT

        when: "try to auth via 1.1 with correct API key"
        def cred = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())
        def auth11ResponseCorrectAPI = cloud11.authenticate(cred, requestContentMediaType, acceptMediaType)

        then: "receive 200"
        auth11ResponseCorrectAPI.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_OK

        when: "try to auth via 1.1 with incorrect API key"
        def cred2 = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), "abcd1234")
        def auth11ResponseWrongApi = cloud11.authenticate(cred2, requestContentMediaType, acceptMediaType)

        then: "receive 401"
        IdmAssert.assertV1AuthFaultResponse(auth11ResponseWrongApi, com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault.class, com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_AUTH_INVALID_API_KEY, ErrorCodes.ERROR_CODE_AUTH_INVALID_API_KEY_MSG))

        when: "try to auth via 2.0 with correct API"
        def auth20ResponseCorrectApi = cloud20.authenticateApiKey(finalUserAdmin.getUsername(), finalUserAdmin.apiKey, requestContentMediaType, acceptMediaType)

        then: "Should be allowed"
        auth20ResponseCorrectApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_OK

        when: "try to auth via 2.0 with incorrect API"
        def auth20ResponseWrongApi = cloud20.authenticateApiKey(finalUserAdmin.getUsername(), "abcd1234", requestContentMediaType, acceptMediaType)

        then: "Should not be allowed"
        auth20ResponseWrongApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED

        where:
        requestContentMediaType         | acceptMediaType                 | factorType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.SMS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.SMS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.OTP
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.OTP
    }

    /**
     * This tests authenticating with v2.0 password on an mfa enabled account
     *
     * @return
     */
    @Unroll
    def "test v2.0 auth with MFA: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        setUpAndEnableMultiFactor(factorType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "try to auth via 2.0 with correct pwd"
        def auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD, requestContentMediaType, acceptMediaType)

        then: "Should get 401 w/ www-authenticate header"
        auth20ResponseCorrectPwd.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED
        auth20ResponseCorrectPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE) != null
        auth20ResponseCorrectPwd.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == finalUserAdmin.username

        when: "try to auth via 2.0 with incorrect pwd"
        def auth20ResponseWrongPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD + "wrong", requestContentMediaType, acceptMediaType)

        then: "Should get 401 without www-authenticate header"
        auth20ResponseWrongPwd.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED
        auth20ResponseWrongPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE) == null
        auth20ResponseWrongPwd.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == finalUserAdmin.username

        when: "auth - second step w/ valid passcode"
        def authSecondStepResponse
        def sessionIdHeader = auth20ResponseCorrectPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        def sessionId = testUtils.extractSessionIdFromWwwAuthenticateHeader(sessionIdHeader)
        if (factorType == FactorTypeEnum.OTP) {
            authSecondStepResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, testUtils.getOTPCode(responseOTP.getKeyUri()), requestContentMediaType, acceptMediaType)
        } else {
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
            authSecondStepResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, "1234", requestContentMediaType, acceptMediaType)
        }

        then:
        authSecondStepResponse.status == 200
        authSecondStepResponse.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == finalUserAdmin.username
        authSecondStepResponse.getHeaders().getFirst(GlobalConstants.X_TENANT_ID) == finalUserAdmin.domainId

        when: "auth - second step w/ invalid passcode"
        sessionIdHeader = auth20ResponseCorrectPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        sessionId = testUtils.extractSessionIdFromWwwAuthenticateHeader(sessionIdHeader)
        if (factorType == FactorTypeEnum.OTP) {
            authSecondStepResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, "invalid", requestContentMediaType, acceptMediaType)
        } else {
            def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, null, null)
            when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
            authSecondStepResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, "invalid", requestContentMediaType, acceptMediaType)
        }

        then:
        authSecondStepResponse.status == 401
        authSecondStepResponse.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == finalUserAdmin.username
        authSecondStepResponse.getHeaders().getFirst(GlobalConstants.X_TENANT_ID) == null

        where:
        requestContentMediaType         | acceptMediaType                 | factorType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.SMS
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.SMS
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | FactorTypeEnum.OTP
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | FactorTypeEnum.OTP
    }

    def "AE Restricted Token sessionId generated"() {
        setup:
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "auth via 2.0 with correct pwd"
        def auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD)

        then: "Get an ae token sessionId"
        def sessionId = utils.extractSessionIdFromFirstWwwAuthenticateHeader(auth20ResponseCorrectPwd.getHeaders())
        sessionId != null
        def restrictedToken = scopeAccessService.unmarshallScopeAccess(sessionId)
        restrictedToken != null
        restrictedToken.scope == TokenScopeEnum.MFA_SESSION_ID.scope
        restrictedToken.authenticatedBy.contains(AuthenticatedByMethodEnum.PASSWORD.value)
        restrictedToken.accessTokenExp != null
        //token should expire after configured lifetime. Add a ms to time checking against to make sure it's after what should be the expiration
        new DateTime(restrictedToken.accessTokenExp).isBefore(new DateTime().plusMinutes(identityConfig.getReloadableConfig().getMfaSessionIdLifetime()).plusMillis(1))
    }

    def "AE Restricted Token sessionId can not be validated or used to get endpoints"() {
        setup:
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        def auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD)
        def sessionId = utils.extractSessionIdFromFirstWwwAuthenticateHeader(auth20ResponseCorrectPwd.getHeaders())

        when: "self validate restricted token sessionId"
        def response = cloud20.validateToken(sessionId, sessionId)

        then: "get 403"
        response.status == HttpStatus.SC_FORBIDDEN

        when: "validate restricted token sessionId using authorized token"
        response = cloud20.validateToken(specificationIdentityAdminToken, sessionId)

        then: "get 404"
        response.status == HttpStatus.SC_NOT_FOUND

        when: "self get endpoints w/ restricted token sessionId"
        response = cloud20.getEndpointsForToken(sessionId, sessionId)

        then: "get 403"
        response.status == HttpStatus.SC_FORBIDDEN

        when: "get endpoints for restricted token sessionId using authorized token"
        response = cloud20.getEndpointsForToken(specificationIdentityAdminToken, sessionId)

        then: "get 404"
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "AE Restricted Token sessionId can be revoked"() {
        setup:
        setUpAndEnableMultiFactor(FactorTypeEnum.OTP)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "revoke own sessionId"
        def auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD)
        def sessionId = utils.extractSessionIdFromFirstWwwAuthenticateHeader(auth20ResponseCorrectPwd.getHeaders())
        def response = cloud20.revokeToken(sessionId)

        then: "get 204"
        response.status == HttpStatus.SC_NO_CONTENT

        when: "revoke own sessionId w/ user revocation"
        auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD)
        sessionId = utils.extractSessionIdFromFirstWwwAuthenticateHeader(auth20ResponseCorrectPwd.getHeaders())
        response = cloud20.revokeUserToken(sessionId, sessionId)

        then: "get 204"
        response.status == HttpStatus.SC_NO_CONTENT

        when: "authorized user revokes other user's sessionId"
        auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD)
        sessionId = utils.extractSessionIdFromFirstWwwAuthenticateHeader(auth20ResponseCorrectPwd.getHeaders())
        response = cloud20.revokeUserToken(utils.getServiceAdminToken(), sessionId)

        then: "get 204"
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def setUpAndEnableMultiFactor(def factorType = FactorTypeEnum.SMS) {
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
