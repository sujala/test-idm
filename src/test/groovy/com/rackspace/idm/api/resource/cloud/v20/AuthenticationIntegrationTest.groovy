package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.*
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.Constants.*
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.when;

class AuthenticationIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    EndpointDao endpointDao

    @Autowired
    ApplicationDao applicationDao

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired(required = false)
    FederatedUserRepository sqlFederatedUserRepository

    @Autowired
    DomainDao domainDao

    @Autowired
    IdentityConfig identityConfig

    def "authenticate with federated user's token does not modify a federated user's tokens"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, [].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = identityUserService.getEndUserById(userAdmin.id)

        //authenticate as the federated user twice
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        //expire the federate user's second token
        def validTokenId = samlResponse1.token.id
        def expiredTokenId = samlResponse2.token.id
        expireToken(expiredTokenId)

        when: "authenticate as federated user using valid token and MOSSO tenant"
        def authRequest = v2Factory.createTokenAuthenticationRequest(validTokenId, "" + userAdminEntity.mossoId, null)
        def response = cloud20.authenticate(authRequest)
        def responesData = response.getEntity(AuthenticateResponse).value

        then: "success and service catalog contains MOSSO tenant"
        response.status == 200

        and: "expired token not deleted"
        scopeAccessDao.getScopeAccessByAccessToken(expiredTokenId) != null
        verifyTenantContainedInServiceCatalog(responesData, userAdminEntity.mossoId, MOSSO_V1_DEF)

        when: "authenticate as federated user using valid token and NAST tenant"
        authRequest = v2Factory.createTokenAuthenticationRequest(validTokenId, userAdminEntity.nastId, null)
        response = cloud20.authenticate(authRequest)
        responesData = response.getEntity(AuthenticateResponse).value

        then: "success and service catalog contains NAST tenant"
        response.status == 200
        verifyTenantContainedInServiceCatalog(responesData, userAdminEntity.nastId, NAST_V1_DEF)

        and: "expired token not deleted"
        scopeAccessDao.getScopeAccessByAccessToken(expiredTokenId) != null

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
    }

    def "[CIDMDEV-5286:CIDMDEV-5305] Test if the user count limit is reached, we get a 400 error (per domain)"() {
        given:
        reloadableConfiguration.setProperty(String.format(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_REG, DEFAULT_IDP_URI), 1)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def username2 = testUtils.getRandomUUID("userSecondForSaml")
        def username3 = testUtils.getRandomUUID("userAdminForSaml2")
        def username4 = testUtils.getRandomUUID("userSecondForSaml2")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def email2 = "fedIntTest2@invalid.rackspace.com"
        def email3 = "fedIntTest3@invalid.rackspace.com"
        def email4 = "fedIntTest4@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username2, expSecs, domainId, null, email2);
        def samlAssertion3 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username3, expSecs, domainId2, null, email3);
        def samlAssertion4 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username4, expSecs, domainId2, null, email4);

        def userAdmin, users, userAdmin2, users2
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)
        def samlResponse3 = cloud20.samlAuthenticate(samlAssertion3)
        def samlResponse4 = cloud20.samlAuthenticate(samlAssertion4)

        then: "Response contains appropriate status"
        samlResponse.status == HttpServletResponse.SC_OK
        samlResponse2.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse3.status == HttpServletResponse.SC_OK
        samlResponse4.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUser(username)
        deleteFederatedUser(username2)
        deleteFederatedUser(username3)
        deleteFederatedUser(username4)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        reloadableConfiguration.setProperty(String.format(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_REG, DEFAULT_IDP_URI), Integer.MAX_VALUE)
    }

    def "authenticate with federated user's token and invalid tenant gives error"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, [].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def token = samlResponse.token.id
        def userAdminEntity = identityUserService.getEndUserById(userAdmin.id)

        when: "authenticate with federated user token and tenant that does not belong to the user"
        def authRequest = v2Factory.createTokenAuthenticationRequest(token, "" + (userAdminEntity.mossoId - 1), null)
        def response = cloud20.authenticate(authRequest)

        then: "an error is returned"
        response.status == 401

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
    }

    @Unroll
    def "Authentication should not fail when a role assigned exists under application without an openStackType"() {
        given: "A new user"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "Creating a new application and role"
        def clientId = testUtils.getRandomUUID()
        def application = new Application(clientId, clientId).with {
            it.description = "description"
            it.enabled = true
            return it
        }
        applicationDao.addApplication(application)
        def roleName = testUtils.getRandomUUID()
        def role = v2Factory.createRole(roleName, clientId)
        def roleResponse = cloud20.createRole(utils.identityAdminToken, role)
        def roleEntity = roleResponse.getEntity(Role).value

        then: "Assert created role"
        roleResponse.status == HttpStatus.CREATED.value()

        when: "Adding role to user"
        cloud20.addRoleToUserOnTenant(utils.identityAdminToken, domainId, userAdmin.id, roleEntity.id)
        def authResponse = cloud20.authenticatePassword(userAdmin.username)

        then:
        authResponse.status == HttpStatus.OK.value()

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(roleEntity)
        utils.deleteService(v1Factory.createService(application.clientId, application.clientId))
    }

    def "Authentication returns tenant ID in header"() {
        given:
        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdminOneTenant, cloudUserAdmin, users1, users2
        (userAdminOneTenant, users1) = utils.createUserAdmin(domainId1)
        (cloudUserAdmin, users2) = utils.createUserAdminWithTenants(domainId2)
        def tenant = utils.createTenant()
        def role = utils.createRole()

        when: "auth w/o a tenant"
        def response = cloud20.authenticate(userAdminOneTenant.username, Constants.DEFAULT_PASSWORD)

        then: "no X-Tenant-Id in response"
        response.status == 200
        !response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)

        when: "add tenant to user"
        utils.addRoleToUserOnTenant(userAdminOneTenant, tenant, role.id)
        response = cloud20.authenticate(userAdminOneTenant.username, Constants.DEFAULT_PASSWORD)

        then: "returns the single tenant in the response"
        response.status == 200
        response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)
        response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == tenant.id

        when: "disable the only tenant and auth again"
        utils.updateTenant(tenant.id, false)
        response = cloud20.authenticate(userAdminOneTenant.username, Constants.DEFAULT_PASSWORD)

        then: "returns the single tenant in the response"
        response.status == 200
        response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)
        response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == tenant.id

        when: "add another role to the user and auth several times"
        def responses = []
        def newTenant = utils.createTenant()
        utils.addRoleToUserOnTenant(userAdminOneTenant, newTenant, role.id)
        10.times {
            responses << cloud20.authenticate(userAdminOneTenant.username, Constants.DEFAULT_PASSWORD)
        }

        then: "an arbitrary tenant is returned in the header"
        def tenantsReturned = []
        responses.size().times {
            assert response.status == 200
            assert response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)
            assert response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == tenant.id || response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == newTenant.id
            tenantsReturned << response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0]
        }

        and: "they are all the same"
        tenantsReturned.unique().size() == 1

        when: "auth w/ cloud user"
        response = cloud20.authenticate(cloudUserAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "returns the mosso tenant in the headers"
        response.status == 200
        response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)
        response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == domainId2

        when: "add another role to the cloud user & auth specifying that tenant"
        utils.updateTenant(tenant.id, true)
        utils.addRoleToUserOnTenant(cloudUserAdmin, tenant, role.id)
        def authRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(cloudUserAdmin.username, Constants.DEFAULT_PASSWORD, tenant.id)
        response = cloud20.authenticate(authRequest)

        then: "the response contains the X-Tenant-Id set to the specified tenant"
        response.status == 200
        response.getHeaders().containsKey(GlobalConstants.X_TENANT_ID)
        response.getHeaders().get(GlobalConstants.X_TENANT_ID)[0] == tenant.id

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
    }

    /**
     * This tests the use of the client role cache in authentication
     *
     * @return
     */
    def "v1.0/v1.1/v2.0 Authentication uses cached roles"() {
        given:
        def domainId = utils.createDomain()
        def user, users1
        (user, users1) = utils.createUserAdmin(domainId)

        def originalRole = utils.createRole()
        utils.addRoleToUser(user, originalRole.id)

        when: "Auth"
        AuthenticateResponse responseV20 = utils.authenticate(user.username, Constants.DEFAULT_PASSWORD)

        then: "User has role"
        responseV20.user.roles.role.find {it.name == originalRole.name} != null

        when: "Change role and auth again"
        ClientRole updatedRole = applicationRoleDao.getClientRole(originalRole.id)
        updatedRole.setName(RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(updatedRole)
        responseV20 = utils.authenticate(user.username, Constants.DEFAULT_PASSWORD)

        then:
        // The role name should be the old value as the client role was cached during initial auth
        assert responseV20.user.roles.role.find {it.name == originalRole.name} != null
        assert responseV20.user.roles.role.find {it.name == updatedRole.name} == null

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteRoleQuietly(originalRole)
    }

    def "users with a nonexistent domain are able to authenticate"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        domainDao.deleteDomain(domainId)
        utils.resetApiKey(userAdmin)
        def apiKey = utils.getUserApiKey(userAdmin).apiKey

        when: "auth v2.0"
        def response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        authResponse.user.domainId == domainId

        when: "auth v2.0 w/ tenant"
        response = cloud20.authenticateTokenAndTenant(utils.getToken(userAdmin.username), domainId)
        authResponse = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        authResponse.user.domainId == domainId

        when: "auth v1.1 api key"
        response = cloud11.authenticate(v1Factory.createUserKeyCredentials(userAdmin.username, apiKey))

        then:
        response.status == 200

        when: "auth v1.1 password (auth-admin)"
        response = cloud11.adminAuthenticate(v1Factory.createPasswordCredentials(userAdmin.username, Constants.DEFAULT_PASSWORD))

        then:
        response.status == 200

        when: "auth 1.0"
        response = cloud10.authenticate(userAdmin.username, apiKey)

        then:
        response.status == 204

        cleanup:
        utils.deleteUsers(users)
    }

    @Unroll
    def "auth returns default sessionInactivityTimeout if not set on domain: accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "auth w/ password"
        def response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "auth w/ API key"
        def apiKey = utils.getUserApiKey(userAdmin).apiKey
        response = cloud20.authenticateApiKey(userAdmin.username, apiKey, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "auth w/ token"
        def token = utils.getToken(userAdmin.username)
        response = cloud20.authenticateTokenAndTenant(token, domainId, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "auth w/ mfa"
        utils.setUpAndEnableUserForMultiFactorSMS(utils.getToken(userAdmin.username), userAdmin)
        response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD, request, accept)
        def sessionIdHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        def sessionId = testUtils.extractSessionIdFromWwwAuthenticateHeader(sessionIdHeader)
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, "1234", request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "auth w/ impersonation token and tenant"
        def impersonationToken = utils.getImpersonatedTokenWithToken(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), userAdmin)
        response = cloud20.authenticateTokenAndTenant(impersonationToken, domainId, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenantById(domainId)
        utils.deleteDomain(domainId)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "auth returns the sessionInactivityTimeout set on the domain: accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def domain = utils.createDomainEntity(domainId)
        def domainDuration = DatatypeFactory.newInstance().newDuration(
                identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().plusHours(3).toString());
        domain.sessionInactivityTimeout = domainDuration
        utils.updateDomain(domain.id, domain)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "auth w/ password"
        def response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, domainDuration.toString())

        when: "auth w/ API key"
        def apiKey = utils.getUserApiKey(userAdmin).apiKey
        response = cloud20.authenticateApiKey(userAdmin.username, apiKey, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, domainDuration.toString())

        when: "auth w/ token"
        def token = utils.getToken(userAdmin.username)
        response = cloud20.authenticateTokenAndTenant(token, domainId, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, domainDuration.toString())

        when: "auth w/ mfa"
        utils.setUpAndEnableUserForMultiFactorSMS(utils.getToken(userAdmin.username), userAdmin)
        response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD, request, accept)
        def sessionIdHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        def sessionId = testUtils.extractSessionIdFromWwwAuthenticateHeader(sessionIdHeader)
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(anyString(), anyString(), anyString())).thenReturn(mfaServiceResponse)
        response = cloud20.authenticateMFAWithSessionIdAndPasscode(sessionId, "1234", request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, domainDuration.toString())

        when: "auth w/ impersonation token and tenant"
        def impersonationToken = utils.getImpersonatedTokenWithToken(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), userAdmin)
        response = cloud20.authenticateTokenAndTenant(impersonationToken, domainId, request, accept)

        then:
        response.status == 200
        assertSessionInactivityTimeout(response, accept, domainDuration.toString())

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenantById(domainId)
        utils.deleteDomain(domainId)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def verifyTenantContainedInServiceCatalog(def response, def tenantId, def endpointTemplateIds) {

        def responseContainsAllEndpoints = true
        for(def curEndpointTemplateId : endpointTemplateIds) {
            def endpointTemplate = endpointDao.getBaseUrlById(curEndpointTemplateId)
            def endpoint = String.format("%s/%s", endpointTemplate.publicUrl, tenantId)
            def serviceCatalogContainsEndpoint = false
            for (List publicUrls : response.serviceCatalog.service.endpoint.publicURL) {

                if (publicUrls.contains(endpoint)) {
                    serviceCatalogContainsEndpoint = true
                }
            }
            responseContainsAllEndpoints == responseContainsAllEndpoints && serviceCatalogContainsEndpoint
        }

        return responseContainsAllEndpoints
    }

    def deleteFederatedUser(username) {
        if (RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
            def federatedUser = sqlFederatedUserRepository.findOneByUsernameAndFederatedIdpId(username, DEFAULT_IDP_ID)
            if(federatedUser != null) sqlFederatedUserRepository.delete(federatedUser)
        } else {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, DEFAULT_IDP_ID)
            if(federatedUser != null) ldapFederatedUserRepository.deleteObject(federatedUser)
        }
    }

    def void expireToken(scopeAccessToExpire) {
        def token = scopeAccessDao.getScopeAccessByAccessToken(scopeAccessToExpire)
        Date now = new Date()
        Date past = new Date(now.year - 1, now.month, now.day)
        token.setAccessTokenExp(past)
        scopeAccessDao.updateScopeAccess(token)
    }

    def void assertSessionInactivityTimeout(response, contentType, expectedSessionInactivityTimeout) {
        def returnedSessionInactivityTimeout
        if (contentType == MediaType.APPLICATION_XML_TYPE) {
            def parsedResponse = response.getEntity(AuthenticateResponse).value
            returnedSessionInactivityTimeout = parsedResponse.user.sessionInactivityTimeout.toString()
        } else {
            def authResponseData = new JsonSlurper().parseText(response.getEntity(String))
            returnedSessionInactivityTimeout = authResponseData.access.user[JSONConstants.RAX_AUTH_SESSION_INACTIVITY_TIMEOUT]
        }

        assert returnedSessionInactivityTimeout == expectedSessionInactivityTimeout
    }

}
