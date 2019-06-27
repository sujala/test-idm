package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService

import com.rackspacecloud.docs.auth.api.v1.ForbiddenFault
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.Constants.DEFAULT_BROKER_IDP_URI
import static com.rackspace.idm.Constants.IDP_V2_DOMAIN_URI

class TerminatorIntegrationTest extends RootIntegrationTest {

    @Autowired IdentityUserService identityUserService
    @Autowired UserService userService
    @Autowired TenantService tenantService
    @Autowired TenantRoleDao tenantRoleDao
    @Autowired FederatedUserDao federatedUserRepository

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    private static final Logger LOG = Logger.getLogger(TerminatorIntegrationTest.class)

    @Unroll
    def "test terminator for provisioned users when all tenants on user are disabled: userType = #tokenType"() {
        given:
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
        if(restricted) {
            assert v10Response.status == 403
            assert v10Response.getEntity(String) == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE
        } else {
            assert v10Response.status == 204
        }

        when: "authenticate v1.1"
        def cred = v1Factory.createUserKeyCredentials(username, apiKey)
        def v11Response = cloud11.authenticate(cred)

        then: "403 is returned for user admin and below"
        if(restricted) {
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
        if(restricted) {
            assert v20ResponseData.serviceCatalog.service.endpoint.flatten().size() == 0
        } else {
            assert v20ResponseData.serviceCatalog.service.endpoint.flatten().size() != 0
        }

        and: "the mosso tenant is in the token scope"
        if(restricted) {
            assert v20ResponseData.token.tenant.id == mossoTenantId
        }

        when: "list endpoints for token, feature enabled"
        def token = v20ResponseData.token.id
        def listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

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
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)

        where:
        tokenType                           | restricted
        IdentityUserTypeEnum.SERVICE_ADMIN  | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false
        IdentityUserTypeEnum.USER_ADMIN     | true
        IdentityUserTypeEnum.USER_MANAGER   | true
        IdentityUserTypeEnum.DEFAULT_USER   | true
        IdentityUserTypeEnum.SERVICE_ADMIN  | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false
        IdentityUserTypeEnum.USER_ADMIN     | true
        IdentityUserTypeEnum.USER_MANAGER   | true
        IdentityUserTypeEnum.DEFAULT_USER   | true
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
        try { utils.disableAndDeleteEndpointTemplate(endpointTemplateId) } catch (Exception e) {}

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
        def listEndpointsForTokenResponse = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), token)

        then: "there are NO endpoints in the response"
        listEndpointsForTokenResponse.status == 200
        def listEndpointsData = listEndpointsForTokenResponse.getEntity(EndpointList).value
        listEndpointsData.endpoint.size() == 0

        cleanup:
        utils.deleteUsers(identityAdmin, defaultUser, userManage, userAdmin)
        utils.deleteDomain(domain)

        where:
        tokenType                           | _
        IdentityUserTypeEnum.SERVICE_ADMIN  | _
        IdentityUserTypeEnum.IDENTITY_ADMIN | _
        IdentityUserTypeEnum.USER_ADMIN     | _
        IdentityUserTypeEnum.USER_MANAGER   | _
        IdentityUserTypeEnum.DEFAULT_USER   | _
    }

    def "test terminator for federated users when all tenants on user are disabled"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def mossoTenantId = userAdmin.domainId
        def nastTenantId = utils.getNastTenant(userAdmin.domainId)
        def username = testUtils.getRandomUUID("samlUser")

        when: "disable tenants in domain and auth as federated user"
        utils.updateTenant(mossoTenantId, false)
        utils.updateTenant(nastTenantId, false)
        def samlResponse = utils.authenticateV2FederatedUser(fedRequest)

        then: "successful auth and empty service catalog"
        def samlToken = samlResponse.token.id
        def listEndpointsData = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), samlToken).getEntity(EndpointList).value
        assert samlResponse.serviceCatalog.service.endpoint.flatten().size() == 0
        assert listEndpointsData.endpoint.size() == 0

        when: "enable one of the tenants and auth again"
        utils.updateTenant(mossoTenantId, true)
        samlResponse = utils.authenticateV2FederatedUser(fedRequest)

        then: "successful and service catalog is popualted again"
        def samlToken2 = samlResponse.token.id
        def listEndpointsData2 = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), samlToken2).getEntity(EndpointList).value
        samlResponse.serviceCatalog.service.endpoint.flatten().size() > 0
        assert listEndpointsData2.endpoint.size() > 0

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)
        utils.deleteUserQuietly(userAdmin)
    }

    def "trying to call v1.1 admin auth call for domain with user admin with all tenants disabled returns error"() {
        given:
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
        assert authResponse.status == 403
        assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE

        when: "auth with mosso creds"
        authResponse = cloud11.adminAuthenticate(v1Factory.createMossoCredentials(Integer.valueOf(mossoTenantId), userAdminEntity.apiKey))

        then:
        assert authResponse.status == 403
        assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE

        when: "auth with nast creds"
        authResponse = cloud11.adminAuthenticate(v1Factory.createNastCredentials(nastTenantId, userAdminEntity.apiKey))

        then:
        assert authResponse.status == 403
        assert authResponse.getEntity(ForbiddenFault).message == GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE

        cleanup:
        utils.deleteUsers(users)
    }

    @Unroll
    def "service catalog is filtered for impersonation tokens of suspended users based on feature flag, shouldDisplayFeatureFlag = #shouldDisplay, applyRcn = #applyRcn, domainTenant = #domainTenant"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_PROP, shouldDisplay)
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def role = utils.createRole()
        def tenant = utils.createTenant()
        if (domainTenant) {
            utils.addTenantToDomain(domainId, tenant.id)
        } else {
            utils.addRoleToUserOnTenant(userAdmin, tenant, role.id)
        }
        utils.updateTenant(tenant.id, false)
        String impersonationToken = utils.getImpersonatedTokenWithToken(utils.getServiceAdminToken(), userAdmin)
        String rackerImpersonationToken = utils.getImpersonationTokenWithRacker(userAdmin)
        utils.addEndpointTemplateToTenant(tenant.id, Integer.parseInt(MOSSO_ENDPOINT_TEMPLATE_ID))

        when: "authenticate as the user admin"
        def authResponse = utils.authenticate(userAdmin, Constants.DEFAULT_PASSWORD, applyRcn.toString())

        then: "no service catalog"
        authResponse.serviceCatalog.service.isEmpty()

        when: "list endpoints for token"
        EndpointList endpoints = utils.listEndpointsForToken(authResponse.token.id, utils.getServiceAdminToken(), applyRcn)

        then: "no endpoints"
        endpoints.endpoint.isEmpty()

        when: "auth with impersonation token"
        authResponse = utils.authenticateTokenWithTenant(impersonationToken, tenant.id, applyRcn.toString())

        then: "service catalog populated based on feature flag"
        if (shouldDisplay) {
            assert authResponse.serviceCatalog != null
            assert authResponse.serviceCatalog.service.endpoint.size() > 0
        } else {
            assert authResponse.serviceCatalog.service.isEmpty()
        }

        when: "list endpoints for impersonation token"
        endpoints = utils.listEndpointsForToken(authResponse.token.id, utils.getServiceAdminToken(), applyRcn)

        then: "endpoints populated based on feature flag"
        if (shouldDisplay) {
            assert endpoints.endpoint.size() > 0
        } else {
            assert endpoints.endpoint.isEmpty()
        }

        when: "auth with racker impersonation token"
        authResponse = utils.authenticateTokenWithTenant(rackerImpersonationToken, tenant.id, applyRcn.toString())

        then: "service catalog populated based on feature flag"
        if (shouldDisplay) {
            assert authResponse.serviceCatalog != null
            assert authResponse.serviceCatalog.service.endpoint.size() > 0
        } else {
            assert authResponse.serviceCatalog.service.isEmpty()
        }

        when: "list endpoints for racker impersonation token"
        endpoints = utils.listEndpointsForToken(authResponse.token.id, utils.getServiceAdminToken(), applyRcn)

        then: "endpoints populated based on feature flag"
        if (shouldDisplay) {
            assert endpoints.endpoint.size() > 0
        } else {
            assert endpoints.endpoint.isEmpty()
        }

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteTenant(tenant)
        utils.deleteRole(role)
        reloadableConfiguration.reset()

        where:
        [shouldDisplay, applyRcn, domainTenant] << [[true, false], [true, false], [true, false]].combinations()
    }

    def addRoleToUserOnTenant(role, tenant, username) {
        def user = userService.checkAndGetUserByName(username)
        TenantRole tenantRole = new TenantRole()
        tenantRole.setName(role.name)
        tenantRole.setClientId(role.serviceId)
        tenantRole.setRoleRsId(role.id)
        tenantRole.setUserId(user.id)
        tenantRole.getTenantIds().add(tenant.id);

        tenantService.addTenantRoleToUser(user, tenantRole, false);
    }

    def deleteRoleOnServiceAdmin(roleId, serviceAdminUsername) {
        def serviceAdmin = userService.checkAndGetUserByName(serviceAdminUsername)
        def tenantRoleEntity = tenantRoleDao.getTenantRoleForUser(serviceAdmin, roleId)
        tenantRoleDao.deleteTenantRoleForUser(serviceAdmin, tenantRoleEntity)
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, DEFAULT_IDP_ID)
            if (federatedUser != null) {
                federatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
