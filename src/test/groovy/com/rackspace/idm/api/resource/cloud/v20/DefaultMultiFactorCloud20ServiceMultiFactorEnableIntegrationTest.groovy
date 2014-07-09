package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.idm.api.resource.cloud.v10.Cloud10VersionResource
import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspacecloud.docs.auth.api.v1.ForbiddenFault
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault
import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.StringUtils
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

/**
 * Tests the multifactor sendVerificationCode REST service
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DefaultMultiFactorCloud20ServiceMultiFactorEnableIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapScopeAccessRepository scopeAccessRepository;

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
     * Override the grizzly start because we want to add another context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        this.resource = startOrRestartGrizzly("classpath:app-config.xml classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
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
        userScopeAccess = (UserScopeAccess)scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id)) { //remove duo profile
                deleteUserQuietly(userAdmin)
            }
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))
        if (defaultUser != null) {
            if (multiFactorService.removeMultiFactorForUser(defaultUser.id)) { //remove duo profile
                deleteUserQuietly(defaultUser)
            }
        }
        if (defaultUserResponsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(defaultUserResponsePhone.getId()))
        if (userManager != null) {
            deleteUserQuietly(userManager)
        }
    }

    /**
     * This tests enabling multi-factor on an account
     *
     * @return
     */
    @Unroll("Successfully enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully enable multifactor"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        def adminToken = utils.getServiceAdminToken()
        def userByIdResponse = cloud20.getUserById(adminToken, userAdmin.id, acceptMediaType)
        def userByUsername = cloud20.getUserByName(adminToken, userAdmin.username, acceptMediaType)
        def usersByEmailResponse = cloud20.getUsersByEmail(adminToken, userAdmin.email, acceptMediaType)
        def usersByDomainResponse = cloud20.getUsersByDomainId(adminToken, userAdmin.domainId, acceptMediaType)
        def usersListResponse = cloud20.listUsers(adminToken, "0", "1000", acceptMediaType)

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

        when: "try to auth via 1.0 with correct API key should be forbidden when mfa enabled"
        def auth10Response = cloud10.authenticate(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())

        then: "receive 403"
        auth10Response.getEntity(String.class) == Cloud10VersionResource.MFA_USER_AUTH_FORBIDDEN_MESSAGE
        auth10Response.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_FORBIDDEN

        when: "try to auth via 1.1 with correct API key should be forbidden when mfa enabled"
        def cred = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())
        def auth11Response403 = cloud11.authenticate(cred, requestContentMediaType, acceptMediaType)

        then: "receive 403"
        IdmAssert.assertV1AuthFaultResponse(auth11Response403, ForbiddenFault.class, com.rackspace.identity.multifactor.util.HttpStatus.SC_FORBIDDEN, DefaultCloud11Service.MFA_USER_AUTH_FORBIDDEN_MESSAGE)

        when: "try to auth via 1.1 with incorrect API key should return 401"
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

    @Unroll("Successfully disable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully disable multifactor after it was enabled"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        resetTokenExpiration()
        User intialUserAdmin = userRepository.getUserById(userAdmin.getId())
        assert intialUserAdmin.isMultiFactorEnabled()
        assert intialUserAdmin.getExternalMultiFactorUserId() != null

        when:
        settings.setEnabled(false)
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        def adminToken = utils.getServiceAdminToken()
        def userByIdResponse = cloud20.getUserById(adminToken, userAdmin.id, acceptMediaType)
        def userByUsername = cloud20.getUserByName(adminToken, userAdmin.username, acceptMediaType)
        def usersByEmailResponse = cloud20.getUsersByEmail(adminToken, userAdmin.email, acceptMediaType)
        def usersByDomainResponse = cloud20.getUsersByDomainId(adminToken, userAdmin.domainId, acceptMediaType)
        def usersListResponse = cloud20.listUsers(adminToken, "0", "1000", acceptMediaType)

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

    @Unroll("Disable multifactor when not enable is no-op: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Disable multifactor when not enable is no-op"() {
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

    @Unroll("Can re-enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Can re-enable multifactor"() {
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

    @Unroll("Fail with 400 when multifactor phone not verified: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when multifactor phone not verified"() {
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

    @Unroll("Fail with 400 when no multifactor device on user account: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when no multifactor device on user account"() {
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

    @Unroll("Enable multifactor when already enabled is no-op: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Enable multifactor when already enabled is no-op"() {
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
    @Unroll("Successfully enable multifactor when external account and phone already exists for user: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully enable multifactor when external account and phone already exists for user"() {
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
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
    }

    def void addDefaultUserPhone() {
        defaultUserResponsePhone = utils.addPhone(defaultUserToken, defaultUser.id)
        utils.sendVerificationCodeToPhone(defaultUserToken, defaultUser.id, defaultUserResponsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin)
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

        when: "user request for himself"
        def response2 = cloud20.getBypassCodes(defaultUserToken, defaultUser.id, request, mediaType, mediaType)

        then: "response should be 403"
        response2.getStatus() == 403

        when: "user admin from another domain request bypass code"
        def anotherAdmin = createUserAdmin()
        def anotherAdminToken = authenticate(anotherAdmin.username)
        def response3 = cloud20.getBypassCodes(anotherAdminToken, defaultUser.id, request, mediaType, mediaType)

        then: "response should be 403"
        response3.getStatus() == 403

        when: "user manager request bypass code"
        def response4 = cloud20.getBypassCodes(userManagerToken, defaultUser.id, request, mediaType, mediaType)

        then: "response should be 403"
        response4.getStatus() == 403

        cleanup:
        deleteUserQuietly(anotherAdmin)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE] * 3 + [MediaType.APPLICATION_JSON_TYPE] * 3
        tokenType << ['service', 'identity', 'admin'] * 2
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
}
