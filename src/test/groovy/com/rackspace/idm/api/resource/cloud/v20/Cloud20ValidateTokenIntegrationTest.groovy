package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static org.apache.http.HttpStatus.*

class Cloud20ValidateTokenIntegrationTest extends RootIntegrationTest{
    private static final Logger LOG = Logger.getLogger(Cloud20ImpersonationIntegrationTest.class)

    @Shared def defaultUser, users

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    LdapScopeAccessRepository scopeAccessDao

    @Autowired
    AuthorizationService authorizationService

    @Autowired
    TokenFormatSelector tokenFormatSelector

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
        utils.createUserAdmin()
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
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_RACKER_TOKEN_FORMAT, TokenFormatEnum.AE.value())
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

    def "Previously created impersonation tokens are no longer valid once the impersonated user's domain is disabled" () {
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
    def "trying to validate a UUID impersonation token for deleted federated user token returns 404"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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

    def "UUID impersonation tokens created before a federated user's domain is disabled are no longer valid"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
    }

    @Unroll
    def "validate impersonation token for federated user contains user's IDP acceptContentType=#accept"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
    def "validate token only returns contact ID for service and Identity Admins, userType = #userType, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
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
        def returnedContactId = getContactIdFromValidateResponse(validateTokenResponse, accept)
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
        def impersonateContactId = getContactIdFromValidateResponse(impersonateResponse, accept)
        if(attributeSet) {
            assert impersonateContactId == contactId
        } else {
            assert impersonateContactId == null
        }

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)
        utils.deleteUsers(users)

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
        getContactIdFromValidateResponse(response, accept) == null

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

    }

    def getContactIdFromValidateResponse(validateResponse, accept) {
        if(MediaType.APPLICATION_XML_TYPE == accept) {
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
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
