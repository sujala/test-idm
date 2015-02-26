package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.mockito.Mockito
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.StringUtils
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

class DefaultMultiFactorCloud20ServiceMultiFactorEnableIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    UserScopeAccess serviceAdminScopeAccess
    org.openstack.docs.identity.api.v2.User serviceAdmin;
    String serviceAdminToken;

    UserScopeAccess identityAdminScopeAccess
    org.openstack.docs.identity.api.v2.User identityAdmin;
    String idenityAdminToken;

    UserScopeAccess userScopeAccess;
    org.openstack.docs.identity.api.v2.User userAdmin;
    String userAdminToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone;
    VerificationCode constantVerificationCode;

    UserScopeAccess userManagerScopeAccess
    org.openstack.docs.identity.api.v2.User userManager;
    String userManagerToken;

    UserScopeAccess defaultUserScopeAccess
    org.openstack.docs.identity.api.v2.User defaultUser;
    String defaultUserToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone defaultUserResponsePhone;

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        userScopeAccess = (UserScopeAccess)scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id)) { //remove duo profile
                deleteUserQuietly(userAdmin)
            }
        }
        if (defaultUser != null) {
            if (multiFactorService.removeMultiFactorForUser(defaultUser.id)) { //remove duo profile
                deleteUserQuietly(defaultUser)
            }
        }
        if (userManager != null) {
            deleteUserQuietly(userManager)
        }
    }

    /**
     * This tests enabling multi-factor on an account
     *
     * @return
     */
    @Unroll
    def "When successfully enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        def userAdminTokenAPI = utils.getTokenFromApiKeyAuth(finalUserAdmin.username, finalUserAdmin.apiKey)
        def adminToken = utils.getServiceAdminToken()
        def userByIdResponse = cloud20.getUserById(adminToken, userAdmin.id, acceptMediaType)
        def userByUsername = cloud20.getUserByName(adminToken, userAdmin.username, acceptMediaType)
        def usersByEmailResponse = cloud20.getUsersByEmail(adminToken, userAdmin.email, acceptMediaType)
        def usersByDomainResponse = cloud20.getUsersByDomainId(adminToken, userAdmin.domainId, acceptMediaType)
        def usersListResponse = cloud20.listUsers(userAdminTokenAPI, "0", "1000", acceptMediaType)

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()
        utils.checkUserMFAFlag(userByIdResponse, true)
        utils.checkUserMFAFlag(userByUsername, true)
        utils.checkUsersMFAFlag(usersByEmailResponse, userAdmin.username, true)
        utils.checkUsersMFAFlag(usersByDomainResponse, userAdmin.username, true)
        utils.checkUsersMFAFlag(usersListResponse, userAdmin.username, true)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def "When successfully enable multifactor api key tokens are not revoked, but password tokens are"() {
        setup:
        addPhone()
        verifyPhone()
        User initialUserAdmin = userRepository.getUserById(userAdmin.getId())

        def userByIdResponse = utils.getUserById(userAdmin.id, specificationIdentityAdminToken)
        AuthenticateResponse apiTokenResponse = utils.authenticateApiKey(userByIdResponse, initialUserAdmin.apiKey)
        String apiToken = apiTokenResponse.token.id
        AuthenticateResponse pwdTokenResponse = utils.authenticate(userByIdResponse)
        String pwdToken = pwdTokenResponse.token.id

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        then:
        cloud20.validateToken(specificationIdentityAdminToken, apiToken).status == HttpStatus.SC_OK
        cloud20.validateToken(specificationIdentityAdminToken, pwdToken).status == HttpStatus.SC_NOT_FOUND
    }

    @Unroll
    def "Successfully disable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        resetTokenExpiration()
        User intialUserAdmin = userRepository.getUserById(userAdmin.getId())
        assert intialUserAdmin.isMultiFactorEnabled()
        assert intialUserAdmin.getExternalMultiFactorUserId() != null

        String userAdminApiToken = utils.getTokenFromApiKeyAuth(intialUserAdmin.username, intialUserAdmin.apiKey)

        when:
        settings.setEnabled(false)
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        def adminToken = utils.getServiceAdminToken()

        def userByIdResponse = cloud20.getUserById(adminToken, userAdmin.id, acceptMediaType)
        def userByUsername = cloud20.getUserByName(adminToken, userAdmin.username, acceptMediaType)
        def usersByEmailResponse = cloud20.getUsersByEmail(adminToken, userAdmin.email, acceptMediaType)
        def usersByDomainResponse = cloud20.getUsersByDomainId(adminToken, userAdmin.domainId, acceptMediaType)
        def usersListResponse = cloud20.listUsers(userAdminApiToken, "0", "1000", acceptMediaType)

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        !finalUserAdmin.isMultiFactorEnabled()
        finalUserAdmin.getExternalMultiFactorUserId() == null
        utils.checkUserMFAFlag(userByIdResponse, false)
        utils.checkUserMFAFlag(userByUsername, false)
        utils.checkUsersMFAFlag(usersByEmailResponse, userAdmin.username, false)
        utils.checkUsersMFAFlag(usersByDomainResponse, userAdmin.username, false)
        utils.checkUsersMFAFlag(usersListResponse, userAdmin.username, false)

        when: "try to auth via 1.0 with correct API key after mfa disabled should now be allowed"
        def auth10Response = cloud10.authenticate(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())

        then: "receive 204"
        auth10Response.status == 204

        when: "try to auth via 1.1 with correct API key after mfa disabled should now be allowed"
        def cred = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())
        def auth11Response200 = cloud11.authenticate(cred, requestContentMediaType, acceptMediaType)

        then: "receive 200"
        auth11Response200.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_OK

        when: "try to auth via 1.1 with incorrect API key"
        def cred2 = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), "abcd1234")
        def auth11Response401 = cloud11.authenticate(cred2, requestContentMediaType, acceptMediaType)

        then: "receive 401"
        IdmAssert.assertV1AuthFaultResponse(auth11Response401, UnauthorizedFault.class, com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED, AuthWithApiKeyCredentials.AUTH_FAILURE_MSG)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Disable multifactor when not enable is no-op: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        !finalUserAdmin.isMultiFactorEnabled()
        finalUserAdmin.getExternalMultiFactorUserId() == null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll()
    def "Can re-enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        resetTokenExpiration()
        settings.setEnabled(false)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User initialUserAdmin = userRepository.getUserById(userAdmin.getId())
        assert !initialUserAdmin.isMultiFactorEnabled()

        when:
        settings.setEnabled(true)
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Fail with 400 when multifactor phone not verified: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_VERIFIED_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.isMultiFactorDeviceVerified()
        !finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Fail with 400 when no multifactor device on user account: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        !finalUserAdmin.isMultiFactorDeviceVerified()
        !finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll()
    def "Enable multifactor when already enabled is no-op: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)
        resetTokenExpiration()

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests a data inconsistency issue. Generally, when multi-factor is disabled on an account, a user profile will not
     * exist in the external provider. However, there is a chance that the state in ldap could be removed successfully, but the call
     * to remove the profile from the external provider fails. This test verifies that in such a situation, multi-factor can
     * still be enabled on the account and the inconsistency is automatically cleared up.
     *
     * @return
     */
    @Unroll
    def "Successfully enable multifactor when external account and phone already exists for user: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)
        resetTokenExpiration()

        //here we're hacking ldap to get the data into an inconsistent state for testing purposes
        User userEntity = userRepository.getUserById(userAdmin.id)
        userEntity.setMultifactorEnabled(false)
        userEntity.setExternalMultiFactorUserId(null)
        userRepository.updateObjectAsIs(userEntity)

        MobilePhone phoneEntity = mobilePhoneRepository.getById(responsePhone.id)
        phoneEntity.setExternalMultiFactorPhoneId(null)
        mobilePhoneRepository.updateObjectAsIs(phoneEntity)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        MobilePhone finalPhoneEntity = mobilePhoneRepository.getById(responsePhone.id)

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()
        StringUtils.hasText(finalUserAdmin.getExternalMultiFactorUserId())

        StringUtils.hasText(finalPhoneEntity.getExternalMultiFactorPhoneId())

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def void addPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
    }

    def void addDefaultUserPhone() {
        defaultUserResponsePhone = utils.addPhone(defaultUserToken, defaultUser.id)
        utils.sendVerificationCodeToPhone(defaultUserToken, defaultUser.id, defaultUserResponsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN)
    }

    def void verifyPhone() {
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
    }

    def void verifyDefaultUserPhone() {
        utils.verifyPhone(defaultUserToken, defaultUser.id, defaultUserResponsePhone.id, constantVerificationCode)
    }

    def void resetTokenExpiration(token = userScopeAccess) {
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        token.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(token)
    }

    def void enableMultiFactor(token, user, scopeAccess) {
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        resetTokenExpiration(scopeAccess)
    }

    def void createMultiFactorDefaultUser() {
        userManager = createDefaultUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        userManagerToken = authenticate(userManager.username)
        userManagerScopeAccess = (UserScopeAccess) scopeAccessRepository.getScopeAccessByAccessToken(userManagerToken)

        defaultUser = createDefaultUser(userManagerToken)
        defaultUserToken = authenticate(defaultUser.username)
        defaultUserScopeAccess = (UserScopeAccess) scopeAccessRepository.getScopeAccessByAccessToken(defaultUserToken)
    }

    @Unroll
    def "Get a bypass code: #mediaType, #tokenType"() {
        setup:
        createMultiFactorDefaultUser()
        addDefaultUserPhone()
        verifyDefaultUserPhone()
        enableMultiFactor(defaultUserToken, defaultUser, defaultUserScopeAccess)

        def adminToken
        switch (tokenType) {
            case 'identity': adminToken = utils.getIdentityAdminToken(); break;
            case 'service':  adminToken = utils.getServiceAdminToken(); break;
            case 'admin': adminToken = userAdminToken; break;
            case 'manager': adminToken = userManagerToken; break;
        }

        def request = createBypassRequest(10, mediaType)

        when: "request bypass code"
        def response = cloud20.getBypassCodes(adminToken, defaultUser.id, request, mediaType, mediaType)
        def codes = parseCodes(response, mediaType)

        then: "gets one code, and 200 OK"
        response.getStatus() == 200
        codes.size() == 1

        when: "use bypass code"
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code twice"
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, "Incorrect passcode. Please try again.", null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify2 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "get denied"
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == "Incorrect passcode. Please try again."

        when: "user admin from another domain request bypass code"
        def anotherAdmin = createUserAdmin()
        def anotherAdminToken = authenticate(anotherAdmin.username)
        def response3 = cloud20.getBypassCodes(anotherAdminToken, defaultUser.id, request, mediaType, mediaType)

        then: "response should be 403"
        response3.getStatus() == 403

        when: "user manager from another domain request bypass code"
        def anotherUserManager = createDefaultUser(anotherAdminToken)
        utils.addRoleToUser(anotherUserManager, USER_MANAGE_ROLE_ID)
        def anotherUserManagerToken = authenticate(anotherUserManager.username)
        def response4 = cloud20.getBypassCodes(anotherUserManagerToken, defaultUser.id, request, mediaType, mediaType)

        then: "response should be 403"
        response4.getStatus() == 403

        cleanup:
        deleteUserQuietly(anotherAdmin)
        deleteUserQuietly(anotherUserManager)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE] * 4 + [MediaType.APPLICATION_JSON_TYPE] * 4
        tokenType << ['service', 'identity', 'admin', 'manager'] * 2
    }

    @Unroll
    def "Get a self-service bypass code: #mediaType, #tokenType"() {
        setup:
        createMultiFactorDefaultUser()
        addDefaultUserPhone()
        verifyDefaultUserPhone()
        enableMultiFactor(defaultUserToken, defaultUser, defaultUserScopeAccess)

        def request = createBypassRequest(10, mediaType, 2)

        when: "user request for himself"
        def response = cloud20.getBypassCodes(defaultUserToken, defaultUser.id, request, mediaType, mediaType)
        def codes = parseCodes(response, mediaType)

        then: "gets two codes, and 200 OK"
        response.getStatus() == 200
        codes.size() == 2

        when: "use bypass code (1)"
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code (1) twice"
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, "Incorrect passcode. Please try again.", null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify2 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "get denied"
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == "Incorrect passcode. Please try again."

        when: "use bypass code (2)"
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, "Incorrect passcode. Please try again.", null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify3 = multiFactorService.verifyPasscode(defaultUser.id, codes[1])

        then: "it allow auth"
        verify3.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code (2) twice"
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, "Incorrect passcode. Please try again.", null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify4 = multiFactorService.verifyPasscode(defaultUser.id, codes[1])

        then: "get denied"
        verify4.decision == MfaAuthenticationDecision.DENY
        verify4.message == "Incorrect passcode. Please try again."

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Get a self-service bypass code with impersonated token"() {
        setup:
        createMultiFactorDefaultUser()
        addDefaultUserPhone()
        verifyDefaultUserPhone()
        enableMultiFactor(defaultUserToken, defaultUser, defaultUserScopeAccess)
        def token = utils.impersonate(utils.getIdentityAdminToken(), defaultUser, 20).token.id

        def request = createBypassRequest(10, MediaType.APPLICATION_XML_TYPE, 2)

        when: "request two bypass codes"
        def response = cloud20.getBypassCodes(token, defaultUser.id, request)
        def codes = parseCodes(response, MediaType.APPLICATION_XML_TYPE)

        then: "gets just one"
        response.getStatus() == 200
        codes.size() == 1

        when: "use bypass code"
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code twice"
        mockMultiFactorAuthenticationService.reset()
        mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.DENY, MfaAuthenticationDecisionReason.DENY, "Incorrect passcode. Please try again.", null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def verify2 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "get denied"
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == "Incorrect passcode. Please try again."
    }

    def "Test concurrency on bypass codes"() {
        setup:
        createMultiFactorDefaultUser()
        addDefaultUserPhone()
        verifyDefaultUserPhone()
        enableMultiFactor(defaultUserToken, defaultUser, defaultUserScopeAccess)
        def adminToken = utils.getIdentityAdminToken()
        def impersonatedToken = utils.impersonate(adminToken, defaultUser, 1000).token.id
        def request = createBypassRequest(10, MediaType.APPLICATION_XML_TYPE, 2)

        when: "create all thread calls"
        def threadCount = 10;
        def List<Boolean> results = new ArrayList<Boolean>();
        for (i in 1..threadCount) {
            final boolean odd = i % 2 == 1;
            Thread.start {
                def response = cloud20.getBypassCodes(odd ? impersonatedToken : defaultUserToken, defaultUser.id, request);
                def codes = parseCodes(response, MediaType.APPLICATION_XML_TYPE)
                results.add(codes.size() == (odd ? 1 : 2))
            }
        }

        while (results.size() < threadCount) {
            sleep(100); // wait to fill the results
        }

        then: "test all results"
        !results.contains(false)
    }

    def parseCodes(response, mediaType = MediaType.APPLICATION_XML_TYPE) {
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            def body = response.getEntity(String.class)
            def slurper = new JsonSlurper().parseText(body)
            return slurper.'RAX-AUTH:bypassCodes'.codes
        } else {
            return response.getEntity(BypassCodes.class).getCodes()
        }
    }

    def createBypassRequest(seconds, mediaType, numberOfCodes = null) {
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            return new JsonBuilder(["RAX-AUTH:bypassCodes": [
                    "validityDuration": "PT" + seconds + "S",
                    "numberOfCodes": numberOfCodes
            ]]).toString()
        } else {
            final BypassCodes request = new BypassCodes()
            DatatypeFactory factory = DatatypeFactory.newInstance();
            request.validityDuration = factory.newDuration(seconds * 1000)
            request.numberOfCodes = numberOfCodes
            return request;
        }
    }

    @Unroll("Successfully delete multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor"() {
        setup:
        createMultiFactorDefaultUser()
        addDefaultUserPhone()
        verifyDefaultUserPhone()
        enableMultiFactor(defaultUserToken, defaultUser, defaultUserScopeAccess)
        resetTokenExpiration()

        def adminToken
        switch (tokenType) {
            case 'identity': adminToken = utils.getIdentityAdminToken(); break;
            case 'service':  adminToken = utils.getServiceAdminToken(); break;
            case 'admin': adminToken = userAdminToken; break;
            case 'manager': adminToken = userManagerToken; break;
        }

        when:
        def response = cloud20.deleteMultiFactor(adminToken, defaultUser.id, requestContentMediaType, acceptMediaType)
        User finalUser = userRepository.getUserById(defaultUser.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUser)

        where:
        requestContentMediaType << [MediaType.APPLICATION_XML_TYPE] * 4 + [MediaType.APPLICATION_JSON_TYPE] * 4
        acceptMediaType = requestContentMediaType
        tokenType << ['service', 'identity', 'admin', 'manager'] * 2
    }

    def void verifyFinalUserState(User finalUserAdmin) {
        assert finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        assert finalUserAdmin.getMultiFactorDevicePin() == null
        assert !finalUserAdmin.isMultiFactorDeviceVerified()
        assert !finalUserAdmin.isMultiFactorEnabled()
        assert finalUserAdmin.getExternalMultiFactorUserId() == null
        assert finalUserAdmin.getMultiFactorMobilePhoneRsId() == null
    }

}
