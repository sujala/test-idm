package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService

import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonSlurper
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class Cloud20ValidateTokenIntegrationTest extends RootIntegrationTest{
    private static final Logger LOG = Logger.getLogger(Cloud20ImpersonationIntegrationTest.class)

    @Shared def defaultUser, users

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired
    ApplicationService applicationService

    @Autowired
    TokenFormatSelector tokenFormatSelector

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    DefaultAETokenService tokenService

    @Unroll
    def "Validate user token: accept = #accept, request = #request" () {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP, issued)
        def expirationTimeInSeconds = 86400
        def marginOfErrorInSeconds = 1000
        def response = utils.authenticateUser(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        utils.revokeToken(response.token.id)
        def authenticateResponse = utils.authenticateUser(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        def token = authenticateResponse.token.id

        when: "do not provide token"
        def validateResponse = cloud20.validateToken("", token)

        then: "get 401"
        validateResponse.status == HttpStatus.SC_UNAUTHORIZED

        when:
        response = cloud20.validateToken(utils.getServiceAdminToken(), token, accept)
        assert (response.status == SC_OK)

        AuthenticateResponse validateResponseEntity = getEntity(response, AuthenticateResponse)

        then:
        validateResponseEntity != null
        validateResponseEntity.token.authenticatedBy.credential.contains("PASSWORD")
        validateResponseEntity.user.phonePinState != null

        validateIssued(token, authenticateResponse, issued)
        validateIssued(token, validateResponseEntity, issued)

        // Validates the expiration time for "User"
        def deltaInSeconds = (validateResponseEntity.token.expires.toGregorianCalendar().timeInMillis - System.currentTimeMillis()) / 1000
        deltaInSeconds < expirationTimeInSeconds + marginOfErrorInSeconds
        deltaInSeconds > expirationTimeInSeconds - marginOfErrorInSeconds

        cleanup:
        reloadableConfiguration.reset()

        where:
        accept                          | request                           | issued
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true
    }

    def "User with validate-token-global role can validate user tokens" () {
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()

        def uaToken = utils.getToken(userAdmin.username)
        def iaToken = utils.getToken(users[0].username)

        def validateRole = applicationService.getCachedClientRoleByName(IdentityRole.VALIDATE_TOKEN_GLOBAL.getRoleName())

        when: "when useradmin does not have validate role, and validate identity admin token"
        def validateResponse = cloud20.validateToken(uaToken, iaToken)

        then: "get 403"
        validateResponse.status == HttpStatus.SC_FORBIDDEN

        when: "give user role"
        utils.addRoleToUser(userAdmin, validateRole.id)
        validateResponse = utils.validateToken(uaToken, iaToken)

        then: "a user-admin can validate an identity admin token"
        validateResponse != null
        validateResponse.token.authenticatedBy.credential.contains("PASSWORD")

        cleanup:
        try {utils.deleteUsers(users)} catch (Exception ex) {/*ignore*/}
    }

    def "AuthenticatedBy should be displayed for racker token" () {
        when:
        def expirationTimeInSeconds = 43200
        def marginOfErrorInSeconds = 1000
        def response = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD)
        utils.revokeToken(response.token.id)
        response = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD)
        def validateResponse = utils.validateToken(response.token.id)

        then:
        validateResponse != null
        validateResponse.token.authenticatedBy.credential.contains("PASSWORD")

        // Validates the expiration time for "Racker"
        def deltaInSeconds = (validateResponse.token.expires.toGregorianCalendar().timeInMillis - System.currentTimeMillis()) / 1000
        deltaInSeconds < expirationTimeInSeconds + marginOfErrorInSeconds
        deltaInSeconds > expirationTimeInSeconds - marginOfErrorInSeconds
    }

    @Unroll
    def "Validate racker token: accept = #accept, request = #request" () {
        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP, issued)
        def authenticateResponse = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD)
        def token = authenticateResponse.token.id

        def response = cloud20.validateToken(utils.getServiceAdminToken(), token, accept)
        assert (response.status == SC_OK)

        AuthenticateResponse validateResponse = getEntity(response, AuthenticateResponse)

        then:
        token != null
        validateResponse.user != null
        validateResponse.user.id == Constants.RACKER
        validateResponse.user.name == Constants.RACKER

        validateIssued(token, authenticateResponse, issued)
        validateIssued(token, validateResponse, issued)

        and: "No phone pin state"
        validateResponse.user.phonePinState == null

        cleanup:
        reloadableConfiguration.reset()

        where:
        accept                          | request                           | issued
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true
    }

    private void validateIssued(token, validateResponse, issued) {
        def created = tokenService.unmarshallToken(token).createTimestamp
        if (issued) {
            validateResponse.token.issued.toGregorianCalendar().getTime() == created
        } else {
            validateResponse.token.issued== null
        }
    }

    @Unroll
    def "Validate Impersonated user's token: mediaType = #mediaType, issuedFlag = #issued" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP, issued)
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        /**
         * Force the impersonation request to use JSON due to a defect in how ImpersonationResponses are marshalled to
         * XML. See https://jira.rax.io/browse/CID-2110
         */
        def impersonateResponse = utils.impersonate(utils.getIdentityAdminToken(), defaultUser, 10800, MediaType.APPLICATION_JSON_TYPE)
        def token = impersonateResponse.token.id

        def response = cloud20.validateToken(utils.getServiceAdminToken(), token, mediaType)
        assert (response.status == SC_OK)

        AuthenticateResponse validateResponse = getEntity(response, AuthenticateResponse)

        then:
        token != null

        validateIssued(token, impersonateResponse, issued)
        validateIssued(token, validateResponse, issued)

        and: "Phone pin state for impersonated user is returned"
        validateResponse.user.phonePinState == PhonePinStateEnum.ACTIVE

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        reloadableConfiguration.reset()

        where:
        [mediaType, issued] << [[MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE], [true, false]].combinations()
    }

    def "Validate Impersonated user's token using a racker" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.validateToken(token)

        then:
        token != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validate Impersonated user's token using a racker where racker uses AE Tokens" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        assert tokenFormatSelector.formatForExistingToken(token) == TokenFormat.AE
        utils.validateToken(token)

        then:
        token != null

        cleanup:
        staticIdmConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validate Impersonated token should return 200 even if user is in a disabled domain" () {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        utils.updateDomain(domainId, domain)
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Previously created impersonation tokens are no longer valid once the impersonated user's domain is disabled - AE tokens only" () {
        given:
        def domainId = utils.createDomain()
        def domainDisable = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        def domainEnable = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = true
            it
        }

        (defaultUser, users) = utils.createDefaultUser(domainId)
        def identityAdmin = users[2]

        when: "Impersonate with Racker using AE impersonation tokens"
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.updateDomain(domainId, domainDisable)
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        when: "Impersonate with provisioned user using AE impersonation tokens"
        identityAdmin.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(identityAdmin)
        utils.updateDomain(domainId, domainEnable)
        response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), defaultUser)
        token = response.token.id
        utils.updateDomain(domainId, domainDisable)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Previously created impersonation tokens are no longer valid once the impersonated user's domain is disabled" () {
        given:
        utils.resetServiceAdminToken()
        def domainId = utils.createDomain()
        def domainDisable = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        def domainEnable = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = true
            it
        }

        (defaultUser, users) = utils.createDefaultUser(domainId)
        def identityAdmin = users[2]

        when: "Impersonate with Racker using AE impersonation tokens"
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.updateDomain(domainId, domainDisable)
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        when: "Impersonate with provisioned user using AE impersonation tokens"
        identityAdmin.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(identityAdmin)
        utils.updateDomain(domainId, domainEnable)
        response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), defaultUser)
        token = response.token.id
        utils.updateDomain(domainId, domainDisable)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
        utils.resetServiceAdminToken()
    }

    def "Validating user's token within a disabled domain should return 404"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def authResponse = utils.authenticate(defaultUser)
        utils.updateDomain(domainId, domain)
        def authResponse2 = cloud20.authenticate(defaultUser.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id,)

        then:
        authResponse2.status == SC_FORBIDDEN
        response.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "validate impersonation token for federated user contains user's IDP acceptContentType=#accept"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def authResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the user"
        def response = utils.impersonateWithRacker(federatedUser)
        def token = response.token.id
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), token, accept)

        then: "response successful"
        validateResponse.status == SC_OK

        and: "contains the user's IDP"
        def userIdp
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            userIdp = validateResponse.getEntity(AuthenticateResponse).value.user.federatedIdp
        } else {
            userIdp = new JsonSlurper().parseText(validateResponse.getEntity(String))['access'].'user'.'RAX-AUTH:federatedIdp'
        }
        userIdp == Constants.IDP_V2_DOMAIN_URI

        cleanup:
        utils.deleteFederatedUserQuietly(authResponse.user.name)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteDomain(userAdmin.domainId)
        staticIdmConfiguration.reset()

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "validate token with userid populated"() {
        def saToken = utils.getServiceAdminToken()
        def (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)

        when:
        def rawResponse = cloud20.validateToken(saToken, token.accessTokenString, type)
        assert rawResponse.status == HttpStatus.SC_OK
        AuthenticateResponse valResponse = getResponseEntity(AuthenticateResponse, type, rawResponse)

        then:
        valResponse.user.id == userAdmin.id
        valResponse.user.name == userAdmin.username
        valResponse.token.id == token.accessTokenString

        where:
        type | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "validate token returns contact ID: mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }
        def user = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate).getEntity(User).value

        def userToken =  utils.getToken(user.username)
        def tokenSa = utils.getServiceAdminToken()
        def tokenIa = utils.getIdentityAdminToken()
        def impTokenIA = utils.impersonateWithToken(tokenIa, user).token.id
        def impTokenRacker = utils.impersonateWithRacker(user).token.id

        when: "user validates own token"
        def validateTokenResponse = cloud20.validateToken(userToken, userToken, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "validate the user token using service-admin"
        validateTokenResponse = cloud20.validateToken(tokenSa, userToken, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "validate the user token using identity-admin"
        validateTokenResponse = cloud20.validateToken(tokenIa, userToken, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "validate impersonation token of the user token using identity-admin"
        validateTokenResponse = cloud20.validateToken(tokenIa, impTokenIA, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "self validate impersonation token of the user token using identity-admin"
        validateTokenResponse = cloud20.validateToken(impTokenIA, impTokenIA, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "validate impersonation token of the user token using racker"
        validateTokenResponse = cloud20.validateToken(tokenIa, impTokenRacker, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        when: "self validate impersonation token of the user token using racker"
        validateTokenResponse = cloud20.validateToken(impTokenRacker, impTokenRacker, mediaType)

        then:
        validateTokenResponse.status == 200
        getContactIdFromValidateResponse(validateTokenResponse) == contactId

        cleanup:
        utils.deleteUser(user)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "self validating a token returns contact ID, accept = #acceptContentType"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when: "Update users contactId"
        for(def user : users) {
            def userForUpdate = new User().with {
                it.contactId = contactId
                it
            }
            cloud20.updateUser(utils.getServiceAdminToken(), user.id, userForUpdate)
        }

        then: "Self validate tokens"
        for (def user : users) {
            def token = utils.getToken(user.username)
            def validateTokenResponse = cloud20.validateToken(token, token, acceptContentType)
            assert validateTokenResponse.status == 200
            def returnedContactId = getContactIdFromValidateResponse(validateTokenResponse)
            assert returnedContactId == contactId
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "contact ID is removed from auth response, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }
        def user = cloud20.createUser(utils.getServiceAdminToken(), userForCreate).getEntity(User).value

        when:
        def response = cloud20.authenticate(user.username, Constants.DEFAULT_PASSWORD, request, accept)

        then:
        response.status == 200
        getContactIdFromValidateResponse(response) == null

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

    }

    def "validate token for a deleted user does not expose user information"() {
        given:
        def user = utils.createIdentityAdmin()
        def token = utils.getToken(user.username)
        utils.deleteUser(user)

        when:
        def response = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        response.status == 404
        def fault = response.getEntity(IdentityFault).value
        fault.message == DefaultUserService.ERROR_MSG_TOKEN_NOT_FOUND
    }

    def "validate should return correct response 404 for invalid token" () {
        when:
        def token = "AAD1PcpjnPTXSIzlUEK2APeEvfxoa33wQqCIU_fIcHjUO5ON70NPXQ2-pMmLp2usnv6VRakMJ3T5o-WmR7UGyIpHSczYMFhK2bYjCPYn24hqQpkUgn_97Lx%20"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        response.status == SC_NOT_FOUND
    }

    def "validate should return 404 when valid token is modified" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.authenticate(defaultUser)
        def token = response.token.id + "%20"
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with PWD: Returns auto-assigned role and allows auth against tenant w/o role based on feature enabled" () {
        given:
        def saToken = utils.getServiceAdminToken()

        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdminWithTenants()

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(saToken, userAdmin.domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == userAdmin.domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != userAdmin.domainId
        }

        // Create a "faws" tenant w/ in domain
        def fawsTenantId = RandomStringUtils.randomAlphanumeric(9)
        def fawsTenantCreate = v2Factory.createTenant(fawsTenantId, fawsTenantId).with {
            it.domainId = mossoTenant.domainId
            it
        }
        def fawsTenant = utils.createTenant(fawsTenantCreate);

        def token = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)

        when: "validate token w/ auto assigned enabled"
        AuthenticateResponse valResponse3 = utils.validateToken(saToken, token)

        then: "validate response include tenant access"
        def roles2 = valResponse3.user.roles.role
        roles2.size() == 6
        roles2.find {it.id == Constants.MOSSO_ROLE_ID} != null
        roles2.find {it.id == Constants.NAST_ROLE_ID} != null
        roles2.find {it.id == Constants.USER_ADMIN_ROLE_ID} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoTenant.id} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastTenant.id} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == fawsTenant.id} != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenantQuietly(mossoTenant)
        utils.deleteTenantQuietly(nastTenant)
        utils.deleteTenantQuietly(fawsTenant)
        utils.deleteDomain(userAdmin.domainId)
    }

    /**
     * This tests the use of the client role cache in authentication
     *
     * @return
     */
    def "Validation uses cached roles"() {
        given:
        def domainId = utils.createDomain()
        def user, users1
        (user, users1) = utils.createUserAdmin(domainId)

        def originalRole = utils.createRole()
        utils.addRoleToUser(user, originalRole.id)

        def token = utils.getToken(user.username)

        when: "validate"
        AuthenticateResponse responseV20 = utils.validateToken(token)

        then: "User has role"
        responseV20.user.roles.role.find {it.name == originalRole.name} != null

        when: "Change role and auth again"
        ClientRole updatedRole = applicationRoleDao.getClientRole(originalRole.id)
        updatedRole.setName(org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(updatedRole)
        responseV20 = utils.validateToken(token)

        then:
        // The role name should be the old value as the client role was cached during initial auth
        assert responseV20.user.roles.role.find {it.name == originalRole.name} != null
        assert responseV20.user.roles.role.find {it.name == updatedRole.name} == null

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteRoleQuietly(originalRole)
    }

    def getContactIdFromValidateResponse(validateResponse) {
        if(validateResponse.getType() == MediaType.APPLICATION_XML_TYPE) {
            def user = validateResponse.getEntity(AuthenticateResponse).value.user
            return user.contactId
        } else {
            def user = new JsonSlurper().parseText(validateResponse.getEntity(String)).access.user
            if(user.keySet().contains('RAX-AUTH:contactId')) assert user['RAX-AUTH:contactId'] != null
            return user['RAX-AUTH:contactId']
        }
    }

    def getResponseEntity(Class type, MediaType responseFormat, ClientResponse response) {
        def result = response.getEntity(type)
        if (responseFormat == MediaType.APPLICATION_XML_TYPE) {
            return result.value
        } else {
            return result;
        }
    }
}
