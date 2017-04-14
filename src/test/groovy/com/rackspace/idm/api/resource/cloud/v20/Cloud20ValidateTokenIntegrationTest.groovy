package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
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
import testHelpers.junit.IgnoreByRepositoryProfile
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
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
    AuthorizationService authorizationService

    @Autowired
    TokenFormatSelector tokenFormatSelector

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired(required = false)
    FederatedUserRepository sqlFederatedUserRepository

    @Autowired
    ApplicationRoleDao applicationRoleDao

    def "Validate user token" () {
        def expirationTimeInSeconds = 86400
        def marginOfErrorInSeconds = 1000
        def response = utils.authenticateUser(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
        utils.revokeToken(response.token.id)
        response = utils.authenticateUser(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)

        when: "do not provide token"
        def validateResponse = cloud20.validateToken("", response.token.id)

        then: "get 401"
        validateResponse.status == HttpStatus.SC_UNAUTHORIZED

        when:
        validateResponse = utils.validateToken(response.token.id)

        then:
        validateResponse != null
        validateResponse.token.authenticatedBy.credential.contains("PASSWORD")

        // Validates the expiration time for "User"
        def deltaInSeconds = (validateResponse.token.expires.toGregorianCalendar().timeInMillis - System.currentTimeMillis()) / 1000
        deltaInSeconds < expirationTimeInSeconds + marginOfErrorInSeconds
        deltaInSeconds > expirationTimeInSeconds - marginOfErrorInSeconds
    }

    def "User with validate-token-global role can validate user tokens" () {
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()

        def uaToken = utils.getToken(userAdmin.username)
        def iaToken = utils.getToken(users[0].username)

        def validateRole = authorizationService.getCachedIdentityRoleByName(IdentityRole.VALIDATE_TOKEN_GLOBAL.getRoleName())

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
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        utils.revokeToken(response.token.id)
        response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
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
    def "Validate racker token" () {
        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def token = response.token.id
        AuthenticateResponse valResponse = utils.validateToken(token)

        then:
        token != null
        valResponse.user != null
        valResponse.user.id == RACKER
        valResponse.user.name == RACKER

        cleanup:
        reloadableConfiguration.reset()
    }

    def "Validate Impersonated user's token" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithToken(utils.getIdentityAdminToken(), defaultUser)
        def token = response.token.id
        utils.validateToken(token)

        then:
        token != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
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
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT, true)
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_DECRYPT, true)

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

    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "Previously created impersonation tokens are no longer valid once the impersonated user's domain is disabled" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
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

        when: "Impersonate with user using UUID impersonation tokens"
        identityAdmin.tokenFormat = TokenFormatEnum.UUID
        utils.updateUser(identityAdmin)
        utils.updateDomain(domainId, domainEnable)
        response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), defaultUser)
        token = response.token.id
        utils.updateDomain(domainId, domainDisable)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_NOT_FOUND

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
        def authResponse2 = cloud20.authenticate(defaultUser.username, DEFAULT_PASSWORD)
        def response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id,)

        then:
        authResponse2.status == SC_FORBIDDEN
        response.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    /**
     * Note - AE impersonation tokens dynamically generate a new user token each use so it's impossible to revoke
     * the underlying user token.
     *
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "trying to validate a UUID impersonation token with deleted provisioned user token returns 404"() {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)
        def identityAdmin = users[2]
        identityAdmin.tokenFormat = TokenFormatEnum.UUID
        utils.updateUser(identityAdmin)

        when: "impersonate the user"
        def response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), defaultUser)
        def token = response.token.id
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "response successful"
        token != null
        resp.status == SC_OK

        when: "delete the token being impersonated and validate the impersonation token"
        def impersontatingTokenEntity = scopeAccessService.getScopeAccessByAccessToken(token)
        def impersonatedTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersontatingTokenEntity.impersonatingToken)
        scopeAccessService.deleteScopeAccess(impersonatedTokenEntity)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "validate returns 404"
        resp.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    /**
     * Note - AE impersonation tokens dynamically generate a new user token each use so it's impossible to revoke
     * the underlying user token.
     *
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "trying to validate a UUID impersonation token for deleted federated user token returns 404"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def identityAdmin = users[0]
        identityAdmin.tokenFormat = TokenFormatEnum.UUID
        utils.updateUser(identityAdmin)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the user"
        def response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), federatedUser)
        def token = response.token.id
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "response successful"
        token != null
        resp.status == SC_OK

        when: "delete the token being impersonated and validate the impersonation token"
        def impersontatingTokenEntity = scopeAccessService.getScopeAccessByAccessToken(token)
        def impersonatedTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersontatingTokenEntity.impersonatingToken)
        scopeAccessService.deleteScopeAccess(impersonatedTokenEntity)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "validate returns 404"
        resp.status == SC_NOT_FOUND

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
    }

    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "UUID impersonation tokens created before a federated user's domain is disabled are no longer valid"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        reloadableConfiguration.setProperty(String.format(IdentityConfig.IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, DEFAULT_IDP_URI), "UUID")
        utils.resetServiceAdminToken()
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def identityAdmin = users[0]
        identityAdmin.tokenFormat = TokenFormatEnum.UUID
        utils.updateUser(identityAdmin)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the user"
        def response = utils.impersonateWithToken(utils.getToken(identityAdmin.username), federatedUser)
        def token = response.token.id
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "response successful"
        token != null
        resp.status == SC_OK

        when: "disable the domain and validate the impersonation token"
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        utils.updateDomain(domainId, domain)
        resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then: "validate returns 404"
        resp.status == SC_NOT_FOUND

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
        utils.resetServiceAdminToken()
    }

    @Unroll
    def "validate impersonation token for federated user contains user's IDP acceptContentType=#accept"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
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
        userIdp == DEFAULT_IDP_URI

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
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
    def "validate token returns contact ID for service and Identity Admins, userType = #userType, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }
        def user = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate).getEntity(User).value

        when: "validate the token"
        def token
        switch(userType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break
        }
        def validateTokenResponse = cloud20.validateToken(token, utils.getToken(user.username), accept)

        then:
        validateTokenResponse.status == 200
        def returnedContactId = getContactIdFromValidateResponse(validateTokenResponse)
        if(attributeSet) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "impersonate the user and validate token"
        def impersonateToken = utils.impersonateWithToken(utils.getIdentityAdminToken(), user).token.id
        def impersonateResponse = cloud20.validateToken(token, impersonateToken, accept)

        then:
        impersonateResponse.status == 200
        def impersonateContactId = getContactIdFromValidateResponse(impersonateResponse)
        if(attributeSet) {
            assert impersonateContactId == contactId
        } else {
            assert impersonateContactId == null
        }

        cleanup:
        utils.deleteUser(user)

        where:
        userType                            | attributeSet | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
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
            cloud20.updateUser(utils.getIdentityAdminToken(), user.id, userForUpdate)
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
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }
        def user = cloud20.createUser(utils.getServiceAdminToken(), userForCreate).getEntity(User).value

        when:
        def response = cloud20.authenticate(user.username, DEFAULT_PASSWORD, request, accept)

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
    @Unroll
    def "Validation uses cached roles based on reloadable property feature.use.cached.client.roles.for.validation: #useCachedRoles"() {
        given:
        // If either of these are 0 then cacheing is disabled altogether and this test would be pointless
        assert identityConfig.getStaticConfig().getClientRoleByIdCacheTtl().toMillis() > 0
        assert identityConfig.getStaticConfig().getClientRoleByIdCacheSize() > 0

        //disable performant catalog so authentication won't populate the cache
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_PROP, useCachedRoles)

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
        if (useCachedRoles) {
            // The role name should be the old value as the client role was cached during initial auth
            assert responseV20.user.roles.role.find {it.name == originalRole.name} != null
            assert responseV20.user.roles.role.find {it.name == updatedRole.name} == null
        } else {
            // The role name should be the new value as the client role is always retrieved from backend
            assert responseV20.user.roles.role.find {it.name == originalRole.name} == null
            assert responseV20.user.roles.role.find {it.name == updatedRole.name} != null
        }

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteRoleQuietly(originalRole)

        where:
        useCachedRoles | _
        true | _
        false | _
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

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, DEFAULT_IDP_ID)
            if (federatedUser != null) {
                if(RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                    sqlFederatedUserRepository.delete(federatedUser)
                } else {
                    ldapFederatedUserRepository.deleteObject(federatedUser)
                }
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
