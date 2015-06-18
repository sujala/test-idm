package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.service.IdentityUserService
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import static com.rackspace.idm.Constants.*

class AuthenticationIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    EndpointDao endpointDao

    def "authenticate with federated user's token does not modify a federated user's tokens"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, [].asList());
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

    def "authenticate with federated user's token and invalid tenant gives error"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, [].asList());
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
        def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
        ldapFederatedUserRepository.deleteObject(federatedUser)
    }

    def void expireToken(scopeAccessToExpire) {
        def token = scopeAccessDao.getScopeAccessByAccessToken(scopeAccessToExpire)
        Date now = new Date()
        Date past = new Date(now.year - 1, now.month, now.day)
        token.setAccessTokenExp(past)
        scopeAccessDao.updateScopeAccess(token)
    }

}
