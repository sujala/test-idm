package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.*

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

    @Autowired(required = false)
    FederatedUserRepository sqlFederatedUserRepository

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
    def "Authentication should not fail when a role assigned exists under application without an openStackType, feature.global.endpoints.for.all.roles.enabled = #flag"() {
        given: "A new user"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, flag)
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

        where:
        flag  | _
        true  | _
        false | _
    }

    @Unroll
    def "Authentication response should not have global endpoints for application without an openStackType, feature.global.endpoints.for.all.roles.enabled = #flag"() {
        given: "A new user"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, flag)
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

        when: "Add new MOSSO global endpoint and update to global"
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def publicUrl = "http://publicUrl.com"
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, publicUrl, null, true, null, clientId, "MOSSO")
        cloud20.addEndpointTemplate(utils.identityAdminToken, endpointTemplate)
        endpointTemplate.enabled = true
        endpointTemplate.global = true
        def updateEndpointTemplateResponse = cloud20.updateEndpointTemplate(utils.serviceAdminToken, endpointTemplateId, endpointTemplate)

        then: "Assert updated global endpoint"
        updateEndpointTemplateResponse.status == HttpStatus.OK.value()

        when: "Adding role to user"
        cloud20.addRoleToUserOnTenant(utils.identityAdminToken, domainId, userAdmin.id, roleEntity.id)
        def auth = utils.authenticate(userAdmin)

        then: "Assert service not in service catalog"
        assert auth.serviceCatalog.service.find({it.name == clientId}) == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(roleEntity)
        utils.deleteService(v1Factory.createService(application.clientId, application.clientId))
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)

        where:
        flag  | _
        true  | _
        false | _
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
            def federatedUser = sqlFederatedUserRepository.findOneByUsernameAndFederatedIdpName(username, DEFAULT_IDP_NAME)
            if(federatedUser != null) sqlFederatedUserRepository.delete(federatedUser)
        } else {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
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

}
