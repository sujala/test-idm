package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import com.rackspacecloud.docs.auth.api.v1.ForbiddenFault
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import static com.rackspace.idm.Constants.*

class TerminatorIntegrationTest extends RootIntegrationTest {

    @Autowired IdentityUserService identityUserService
    @Autowired UserService userService
    @Autowired TenantService tenantService
    @Autowired TenantRoleDao tenantRoleDao
    @Autowired FederatedUserDao federatedUserRepository

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired(required = false)
    FederatedUserRepository sqlFederatedUserRepository

    private static final Logger LOG = Logger.getLogger(TerminatorIntegrationTest.class)

    @Unroll
    def "test terminator for provisioned users when all tenants on user are disabled: userType = #tokenType, featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, featureEnabled)
        def domain = utils.createDomain()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", testUtils.getRandomUUID("http://public/"), "cloudFiles", true, "ORD")
        utils.createAndUpdateEndpointTemplate(endpointTemplate, endpointTemplateId)
        def tenant = utils.createTenant()
        def role = utils.createRole()
        addRoleToUserOnTenant(role, tenant, Constants.SERVICE_ADMIN_2_USERNAME)
        cloud20.addEndpoint(utils.getServiceAdminToken(), tenant.id, endpointTemplate)
        def identityAdmin = utils.createUser(utils.getServiceAdminToken(), testUtils.getRandomUUID("identityAdmin"))
        addRoleToUserOnTenant(role, tenant, identityAdmin.username)
        def userAdmin = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domain)
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("defaultUser"), domain)
        def userManage = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("defaultUser"), domain)
        utils.addRoleToUser(userManage, USER_MANAGE_ROLE_ID)
        def mossoTenantId = domain
        def nastTenantId = utils.getNastTenant(domain)
        //disable mosso and nast tenants for user admin and sub users
        utils.updateTenant(nastTenantId, false)
        utils.updateTenant(mossoTenantId, false)
        //disable the tenant on the service admin
        utils.updateTenant(tenant.id, false)
        def username, apiKey, password
        switch (tokenType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                username = Constants.SERVICE_ADMIN_2_USERNAME
                apiKey = Constants.SERVICE_ADMIN_2_API_KEY
                utils.addApiKeyToUser(utils.getUserByName(username), apiKey) // FIXME: fix wherever we override this
                password = Constants.SERVICE_ADMIN_2_PASSWORD
                break;
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                username = identityAdmin.username
                apiKey = identityUserService.getEndUserById(identityAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_ADMIN:
                username = userAdmin.username
                apiKey = identityUserService.getEndUserById(userAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_MANAGER:
                username = userManage.username
                apiKey = identityUserService.getEndUserById(userManage.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.DEFAULT_USER:
                username = defaultUser.username
                apiKey = identityUserService.getEndUserById(defaultUser.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
        }

        when: "authenticate v1.0"
        def v10Response = cloud10.authenticate(username, apiKey)

        then: "403 is returned for user admin and below"
        if(featureEnabled && restricted) {
            assert v10Response.status == 403
            assert v10Response.getEntity(String) == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert v10Response.status == 204
        }

        when: "authenticate v1.1"
        def cred = v1Factory.createUserKeyCredentials(username, apiKey)
        def v11Response = cloud11.authenticate(cred)

        then: "403 is returned for user admin and below"
        if(featureEnabled && restricted) {
            assert v11Response.status == 403
            assert v11Response.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert v11Response.status == 200
        }

        when: "authenticate v2.0"
        def v20Response = cloud20.authenticate(username, password)

        then: "no service catalog is returned for user admin and below"
        v20Response.status == 200
        def v20ResponseData = v20Response.getEntity(AuthenticateResponse).value
        if(featureEnabled && restricted) {
            assert v20ResponseData.serviceCatalog.service.endpoint.flatten().size() == 0
        } else {
            assert v20ResponseData.serviceCatalog.service.endpoint.flatten().size() != 0
        }

        and: "the mosso tenant is in the token scope"
        if(featureEnabled && restricted) {
            assert v20ResponseData.token.tenant.id == mossoTenantId
        }

        when: "list endpoints for token, feature disabled"
        def token = v20ResponseData.token.id
        def listEndpointsForTokenFeatureEnabled = false
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, listEndpointsForTokenFeatureEnabled)
        def listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

        then: "NOT empty if user is restricted by terminator and feature flag is disabled"
        listEndpointsForTokenResponse.status == 200
        def listEndpointsData = listEndpointsForTokenResponse.getEntity(EndpointList).value
        assert listEndpointsData.endpoint.size() > 0

        when: "list endpoints for token, feature enabled"
        listEndpointsForTokenFeatureEnabled = true
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, listEndpointsForTokenFeatureEnabled)
        listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

        then: "empty if user is restricted by terminator and feature flag is enabled"
        listEndpointsForTokenResponse.status == 200
        def listEndpointsData2 = listEndpointsForTokenResponse.getEntity(EndpointList).value
        if(restricted) {
            assert listEndpointsData2.endpoint.size() == 0
        } else {
            assert listEndpointsData2.endpoint.size() > 0
        }

        cleanup:
        utils.deleteUsers(identityAdmin, defaultUser, userManage, userAdmin)
        deleteRoleOnServiceAdmin(role.id, Constants.SERVICE_ADMIN_2_USERNAME)
        utils.deleteTenant(tenant)
        utils.deleteDomain(domain)
        utils.deleteEndpointTemplate(endpointTemplate)
        reloadableConfiguration.reset()

        where:
        tokenType                           | restricted | featureEnabled
        IdentityUserTypeEnum.SERVICE_ADMIN  | false      | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | false      | true
        IdentityUserTypeEnum.USER_ADMIN     | true       | true
        IdentityUserTypeEnum.USER_MANAGER   | true       | true
        IdentityUserTypeEnum.DEFAULT_USER   | true       | true
        IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | true       | false
        IdentityUserTypeEnum.USER_MANAGER   | true       | false
        IdentityUserTypeEnum.DEFAULT_USER   | true       | false
    }

    @Unroll
    def "test terminator for provisioned users when user has disabled tenants but at least one enabled tenant: userType = #tokenType"() {
        given:
        def domain = utils.createDomain()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", testUtils.getRandomUUID("http://public/"), "cloudFiles", true, "ORD")
        utils.createAndUpdateEndpointTemplate(endpointTemplate, endpointTemplateId)
        def tenant1 = utils.createTenant()
        def tenant2 = utils.createTenant()
        def role = utils.createRole()
        addRoleToUserOnTenant(role, tenant1, Constants.SERVICE_ADMIN_2_USERNAME)
        addRoleToUserOnTenant(role, tenant2, Constants.SERVICE_ADMIN_2_USERNAME)
        cloud20.addEndpoint(utils.getServiceAdminToken(), tenant1.id, endpointTemplate)
        cloud20.addEndpoint(utils.getServiceAdminToken(), tenant2.id, endpointTemplate)
        def identityAdmin = utils.createUser(utils.getServiceAdminToken(), testUtils.getRandomUUID("identityAdmin"))
        addRoleToUserOnTenant(role, tenant1, identityAdmin.username)
        addRoleToUserOnTenant(role, tenant2, identityAdmin.username)
        def userAdmin = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domain)
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("defaultUser"), domain)
        def userManage = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("defaultUser"), domain)
        utils.addRoleToUser(userManage, USER_MANAGE_ROLE_ID)
        def mossoTenantId = domain
        //disable mosso tenant
        utils.updateTenant(mossoTenantId, false)
        utils.updateTenant(tenant1.id, false)
        def username, apiKey, password
        switch (tokenType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                username = Constants.SERVICE_ADMIN_2_USERNAME
                apiKey = Constants.SERVICE_ADMIN_2_API_KEY
                password = Constants.SERVICE_ADMIN_2_PASSWORD
                break;
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                username = identityAdmin.username
                apiKey = identityUserService.getEndUserById(identityAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_ADMIN:
                username = userAdmin.username
                apiKey = identityUserService.getEndUserById(userAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_MANAGER:
                username = userManage.username
                apiKey = identityUserService.getEndUserById(userManage.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.DEFAULT_USER:
                username = defaultUser.username
                apiKey = identityUserService.getEndUserById(defaultUser.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
        }

        when: "authenticate v1.0"
        def v10Response = cloud10.authenticate(username, apiKey)

        then: "204 is returned for user"
        v10Response.status == 204

        when: "authenticate v1.1"
        def cred = v1Factory.createUserKeyCredentials(username, apiKey)
        def v11Response = cloud11.authenticate(cred)

        then: "200 is returned for user"
        v11Response.status == 200

        when: "authenticate v2.0"
        def v20Response = cloud20.authenticate(username, password)

        then: "the service catalog is returned for user"
        v20Response.status == 200
        def authData = v20Response.getEntity(AuthenticateResponse).value
        authData.serviceCatalog.service.endpoint.flatten().size() != 0

        when: "enable the feature to filter the endpoints and list endpoints for token"
        def token = authData.token.id
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, true)
        def listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

        then: "there are endpoints in the response"
        listEndpointsForTokenResponse.status == 200
        def listEndpointsData = listEndpointsForTokenResponse.getEntity(EndpointList).value
        listEndpointsData.endpoint.size() > 0

        cleanup:
        try { utils.deleteUsers(identityAdmin, defaultUser, userManage, userAdmin) } catch (Exception e) {}
        try { deleteRoleOnServiceAdmin(role.id, Constants.SERVICE_ADMIN_2_USERNAME) } catch (Exception e) {}
        try { utils.deleteTenant(tenant1) } catch (Exception e) {}
        try { utils.deleteTenant(tenant2) } catch (Exception e) {}
        try { utils.deleteDomain(domain) } catch (Exception e) {}
        try { utils.deleteEndpointTemplate(endpointTemplate) } catch (Exception e) {}
        reloadableConfiguration.reset()

        where:
        tokenType | restricted
        IdentityUserTypeEnum.SERVICE_ADMIN  | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false
        IdentityUserTypeEnum.USER_ADMIN     | true
        IdentityUserTypeEnum.USER_MANAGER   | true
        IdentityUserTypeEnum.DEFAULT_USER   | true
    }

    @Unroll
    def "test terminator for provisioned users when user has no tenants: userType = #tokenType"() {
        given:
        def domain = utils.createDomain()
        def identityAdmin, userAdmin, defaultUser, userManage
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domain)
        def username, apiKey, password
        switch (tokenType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                username = Constants.SERVICE_ADMIN_2_USERNAME
                apiKey = Constants.SERVICE_ADMIN_2_API_KEY
                password = Constants.SERVICE_ADMIN_2_PASSWORD
                break;
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                username = identityAdmin.username
                apiKey = identityUserService.getEndUserById(identityAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_ADMIN:
                username = userAdmin.username
                apiKey = identityUserService.getEndUserById(userAdmin.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.USER_MANAGER:
                username = userManage.username
                apiKey = identityUserService.getEndUserById(userManage.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
            case IdentityUserTypeEnum.DEFAULT_USER:
                username = defaultUser.username
                apiKey = identityUserService.getEndUserById(defaultUser.id).apiKey
                password = Constants.DEFAULT_PASSWORD
                break;
        }

        when: "authenticate v1.0"
        def v10Response = cloud10.authenticate(username, apiKey)

        then: "204 is returned for user"
        v10Response.status == 204

        when: "authenticate v1.1"
        def cred = v1Factory.createUserKeyCredentials(username, apiKey)
        def v11Response = cloud11.authenticate(cred)

        then: "200 is returned for user"
        v11Response.status == 200

        when: "authenticate v2.0"
        def v20Response = cloud20.authenticate(username, password)

        then: "the empty service catalog is returned for the user"
        v20Response.status == 200
        def authData = v20Response.getEntity(AuthenticateResponse).value
        authData.serviceCatalog.service.endpoint.flatten().size() == 0

        when: "enable the feature to filter the endpoints and list endpoints for token"
        def token = authData.token.id
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, true)
        def listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

        then: "there are NO endpoints in the response"
        listEndpointsForTokenResponse.status == 200
        def listEndpointsData = listEndpointsForTokenResponse.getEntity(EndpointList).value
        listEndpointsData.endpoint.size() == 0

        cleanup:
        utils.deleteUsers(identityAdmin, defaultUser, userManage, userAdmin)
        utils.deleteDomain(domain)
        reloadableConfiguration.reset()

        where:
        tokenType                           | _
        IdentityUserTypeEnum.SERVICE_ADMIN  | _
        IdentityUserTypeEnum.IDENTITY_ADMIN | _
        IdentityUserTypeEnum.USER_ADMIN     | _
        IdentityUserTypeEnum.USER_MANAGER   | _
        IdentityUserTypeEnum.DEFAULT_USER   | _
    }

    @Unroll
    def "test terminator for federated users when all tenants on user are disabled, authFeatureEnabled = #authFeatureEnabled, endpoinstFeatureEnabled = #endpointsFeatureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, authFeatureEnabled)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, endpointsFeatureEnabled)
        def domainId = utils.createDomain()
        def mossoTenantId = domainId
        def nastTenantId = utils.getNastTenant(domainId)
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, 500, domainId, [].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "disable tenants in domain and auth as federated user"
        utils.updateTenant(mossoTenantId, false)
        utils.updateTenant(nastTenantId, false)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "successful auth and empty service catalog"
        samlResponse.status == 200
        def responseData = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = responseData.token.id
        def listEndpointsData = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), samlToken).getEntity(EndpointList).value
        if(authFeatureEnabled) {
            assert responseData.serviceCatalog.service.endpoint.flatten().size() == 0
        } else {
            assert responseData.serviceCatalog.service.endpoint.flatten().size() > 0
        }
        if(endpointsFeatureEnabled) {
            assert listEndpointsData.endpoint.size() == 0
        } else {
            assert listEndpointsData.endpoint.size() > 0
        }

        when: "enable one of the tenants and auth again"
        utils.updateTenant(mossoTenantId, true)
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "successful and service catalog is popualted again"
        def responseData2 = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken2 = responseData.token.id
        def listEndpointsData2 = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), samlToken2).getEntity(EndpointList).value
        responseData2.serviceCatalog.service.endpoint.flatten().size() > 0
        assert listEndpointsData2.endpoint.size() > 0

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUserQuietly(username)
        reloadableConfiguration.reset()

        where:
        authFeatureEnabled | endpointsFeatureEnabled
        true               | true
        false              | false
        true               | false
        false              | true
    }

    @Unroll
    def "trying to call v1.1 admin auth call for domain with user admin with all tenants disabled returns error, featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, featureEnabled)
        def domain = utils.createDomain()
        def mossoTenantId = domain
        def nastTenantId = utils.getNastTenant(domain)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domain)
        def userAdminEntity = userService.getUserById(userAdmin.id)
        utils.updateTenant(mossoTenantId, false)
        utils.updateTenant(nastTenantId, false)

        when: "auth with password creds"
        def authResponse = cloud11.adminAuthenticate(v1Factory.createPasswordCredentials(userAdmin.username, Constants.DEFAULT_PASSWORD))

        then:
        if(featureEnabled) {
            assert authResponse.status == 403
            assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert authResponse.status == 200
        }

        when: "auth with mosso creds"
        authResponse = cloud11.adminAuthenticate(v1Factory.createMossoCredentials(Integer.valueOf(mossoTenantId), userAdminEntity.apiKey))

        then:
        if(featureEnabled) {
            assert authResponse.status == 403
            assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert authResponse.status == 200
        }

        when: "auth with nast creds"
        authResponse = cloud11.adminAuthenticate(v1Factory.createNastCredentials(nastTenantId, userAdminEntity.apiKey))

        then:
        if(featureEnabled) {
            assert authResponse.status == 403
            assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert authResponse.status == 200
        }

        cleanup:
        utils.deleteUsers(users)
        reloadableConfiguration.reset()

        where:
        featureEnabled | _
        true           | _
        false          | _
    }

    def addRoleToUserOnTenant(role, tenant, username) {
        def user = userService.checkAndGetUserByName(username)
        TenantRole tenantRole = new TenantRole()
        tenantRole.setName(role.name)
        tenantRole.setClientId(role.serviceId)
        tenantRole.setRoleRsId(role.id)
        tenantRole.setUserId(user.id)
        tenantRole.getTenantIds().add(tenant.id);

        tenantService.addTenantRoleToUser(user, tenantRole);
    }

    def deleteRoleOnServiceAdmin(roleId, serviceAdminUsername) {
        def serviceAdmin = userService.checkAndGetUserByName(serviceAdminUsername)
        def tenantRoleEntity = tenantRoleDao.getTenantRoleForUser(serviceAdmin, roleId)
        tenantRoleDao.deleteTenantRoleForUser(serviceAdmin, tenantRoleEntity)
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                if (RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                    federatedUser = sqlFederatedUserRepository.findOneByUsernameAndFederatedIdpName(username, DEFAULT_IDP_NAME)
                    sqlFederatedUserRepository.delete(federatedUser)
                } else {
                    federatedUserRepository.deleteObject(federatedUser)
                }
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
