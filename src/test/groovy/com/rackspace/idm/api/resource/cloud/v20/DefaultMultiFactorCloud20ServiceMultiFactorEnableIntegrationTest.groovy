package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.util.StringUtils
import org.subethamail.wiser.WiserMessage
import spock.lang.Unroll
import testHelpers.EmailUtils
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

class DefaultMultiFactorCloud20ServiceMultiFactorEnableIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private MobilePhoneDao mobilePhoneRepository;

    @Autowired
    private UserDao userRepository;

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
        utils.addApiKeyToUser(userAdmin)
        userAdminToken = utils.authenticateApiKey(userAdmin.username).token.id
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

    def "When successfully enable multifactor api key and password reset tokens are not revoked, but password tokens are"() {
        setup:
        addPhone()
        verifyPhone()
        User initialUserAdmin = userRepository.getUserById(userAdmin.getId())

        def userByIdResponse = utils.getUserById(userAdmin.id, specificationIdentityAdminToken)
        AuthenticateResponse apiTokenResponse = utils.authenticateApiKey(userByIdResponse, initialUserAdmin.apiKey)
        String apiToken = apiTokenResponse.token.id
        AuthenticateResponse pwdTokenResponse = utils.authenticate(userByIdResponse)
        String pwdToken = pwdTokenResponse.token.id
        cloud20.forgotPassword(v2Factory.createForgotPasswordCredentials(initialUserAdmin.username, null))

        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        def resetToken = EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        then:
        cloud20.validateToken(specificationIdentityAdminToken, apiToken).status == HttpStatus.SC_OK

        //401 would be token invalid. 400 just that request is invalid
        cloud20.resetPassword(resetToken, v2Factory.createPasswordReset("abc")).status == 400
        //sleep for a second to avoid race condition with docker image
        sleep(1000)
        cloud20.validateToken(specificationIdentityAdminToken, pwdToken).status == HttpStatus.SC_NOT_FOUND
    }

    def "When domain enforcement is applied, ensure password reset tokens are not revoked"() {
        setup:
        addPhone()
        verifyPhone()
        createMultiFactorDefaultUser()

        // Get reset token for userAdmin
        def userAdminToken = utils.getToken(userAdmin.username)
        cloud20.forgotPassword(v2Factory.createForgotPasswordCredentials(userAdmin.username, null))
        List<WiserMessage> messages = wiserWrapper.wiserServer.getMessages()
        WiserMessage message = messages.get(messages.size() - 1)
        def resetUserAdminToken = EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())
        // Get reset token for defaultUser
        def defaultUserToken = utils.getToken(defaultUser.username)
        cloud20.forgotPassword(v2Factory.createForgotPasswordCredentials(defaultUser.username, null))
        messages = wiserWrapper.wiserServer.getMessages()
        message = messages.get(messages.size() - 1)
        def resetDefaultUserToken = EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())

        when: "Update MFA domain settings"
        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()
        settings.domainMultiFactorEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.REQUIRED
        def response = cloud20.updateMultiFactorDomainSettings(utils.identityAdminToken, userAdmin.domainId, settings)

        then:
        response.status == 204
        cloud20.validateToken(utils.identityAdminToken, userAdminToken).status == 404
        cloud20.validateToken(utils.identityAdminToken, defaultUserToken).status == 404
        cloud20.validateToken(resetUserAdminToken, resetUserAdminToken).status == 403
        cloud20.validateToken(resetDefaultUserToken, resetDefaultUserToken).status == 403
    }

    def "Enabling MFA sends cloud feed event for enabling MFA, and one for disabling only Password tokens"() {
        setup:
        addPhone()
        verifyPhone()
        User initialUserAdmin = userRepository.getUserById(userAdmin.getId())
        def userById = utils.getUserById(userAdmin.id, specificationIdentityAdminToken)
        resetCloudFeedsMock()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        and: "verify that 2 events were posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(2)
        )

        and: "verify that an UPDATE event was posted for the user"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(userById, EventType.UPDATE.name()),
                VerificationTimes.exactly(1)
        )

        and: "verify that the USER TRR event was posted for PASSWORD "
        cloudFeedsMock.verify(
                testUtils.createUserTrrFeedsRequest(userById, AuthenticatedByMethodGroup.PASSWORD),
                VerificationTimes.exactly(1)
        )

        and: "verify that a USER TRR event was NOT posted for EMAIL "
        cloudFeedsMock.verify(
                testUtils.createUserTrrFeedsRequest(userById, AuthenticatedByMethodGroup.EMAIL),
                VerificationTimes.exactly(0)
        )
    }


    @Unroll
    def "Successfully disable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
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

    def "Fail with 400 when phone not verified when specify SMS"() {
        setup:
        addPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true).with {
            it.factorType = FactorTypeEnum.SMS
            it
        }

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_VERIFIED_PHONE)
    }

    def "Fail with 400 when OTP not verified when specify SMS"() {
        setup:
        addOTP()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true).with {
            it.factorType = FactorTypeEnum.OTP
            it
        }

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_VERIFIED_OTP_DEVICE)
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

        //here we're hacking ldap to get the data into an inconsistent state for testing purposes
        User userEntity = userRepository.getUserById(userAdmin.id)
        userEntity.setMultifactorEnabled(false)
        userEntity.setExternalMultiFactorUserId(null)
        userRepository.updateUserAsIs(userEntity)

        MobilePhone phoneEntity = mobilePhoneRepository.getById(responsePhone.id)
        phoneEntity.setExternalMultiFactorPhoneId(null)
        mobilePhoneRepository.updateMobilePhone(phoneEntity)

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

    def void addOTP() {
        OTPDevice request = new OTPDevice()
        request.setName("test")
        def device = utils.addOTPDevice(userAdminToken, userAdmin.id, request)
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

    def void enableMultiFactor(token, user, scopeAccess) {
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
    }

    def void createMultiFactorDefaultUser() {
        userManager = createDefaultUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        utils.addApiKeyToUser(userManager)
        userManagerToken = utils.authenticateApiKey(userManager.username).token.id
        userManagerScopeAccess = (UserScopeAccess) scopeAccessRepository.getScopeAccessByAccessToken(userManagerToken)

        defaultUser = createDefaultUser(userManagerToken)
        utils.addApiKeyToUser(defaultUser)
        defaultUserToken = utils.authenticateApiKey(defaultUser.username).token.id
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
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code twice"
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

        def request = createBypassRequest(120, mediaType, 2)

        when: "user request for himself"
        def response = cloud20.getBypassCodes(defaultUserToken, defaultUser.id, request, mediaType, mediaType)
        def codes = parseCodes(response, mediaType)

        then: "gets two codes, and 200 OK"
        response.getStatus() == 200
        codes.size() == 2

        when: "use bypass code (1)"
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code (1) twice"
        def verify2 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "get denied"
        verify2.decision == MfaAuthenticationDecision.DENY
        verify2.message == "Incorrect passcode. Please try again."

        when: "use bypass code (2)"
        def verify3 = multiFactorService.verifyPasscode(defaultUser.id, codes[1])

        then: "it allow auth"
        verify3.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code (2) twice"
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

        def request = createBypassRequest(120, MediaType.APPLICATION_XML_TYPE, 2)

        when: "request two bypass codes"
        def response = cloud20.getBypassCodes(token, defaultUser.id, request)
        def codes = parseCodes(response, MediaType.APPLICATION_XML_TYPE)

        then: "gets just one"
        response.getStatus() == 200
        codes.size() == 1

        when: "use bypass code"
        def verify1 = multiFactorService.verifyPasscode(defaultUser.id, codes[0])

        then: "it allow auth"
        verify1.decision == MfaAuthenticationDecision.ALLOW

        when: "use bypass code twice"
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
        def request = createBypassRequest(120, MediaType.APPLICATION_XML_TYPE, 2)

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

        long time = System.currentTimeMillis()
        while (results.size() < threadCount && (System.currentTimeMillis() - time) < 30000) {
            sleep(100); // wait to fill the results (waits for a maximum of 15sec)
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
