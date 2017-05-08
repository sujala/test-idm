package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class ApplyRcnRolesAuthenticationRestIntegrationTests extends RootIntegrationTest {

    @Autowired
    TenantService tenantService

    @Autowired
    EndpointService endpointService

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "apply_rcn_roles logic only used when feature.performant.service.catalog is enabled"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, false)
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(user.username)

        then: "identity:admin role is a global role"
        def idAdminRole = response.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole != null
        idAdminRole.getTenantId() == null

        when: "auth as standard user-admin with feature enabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(user.username)

        then: "identity:user-admin role is not a global role"
        def idAdminRole2 = response2.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole2 != null
        idAdminRole2.getTenantId() != null

        cleanup:
        utils.deleteUserQuietly(user)
    }

    /**
     * Tests whether the rcn role logic is applied to the user's roles based on query param. Does this by using
     * the user's Identity Classification role is denormalized and set on individual tenants as the canary
     * @return
     */
    @Unroll
    def "apply_rcn_roles logic only used when apply_rcn_roles = true (case insensitive): value: #param"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature enabled"
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(user.username, param)

        then: "applied"
        def idAdminRole2 = response2.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole2 != null
        if (applied) {
            assert idAdminRole2.getTenantId() != null
        } else {
            assert idAdminRole2.getTenantId() == null
        }

        cleanup:
        utils.deleteUserQuietly(user)

        where:
        param | applied
        "true"  | true
        "TRUE"  | true
        "truE"  | true
        "true "  | false
        "false"  | false
        "norunme"  | false
        " true"  | false
        "1"  | false
        null  | false
    }

    def "apply_rcn_roles causes no global roles to be returned"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature enabled"
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(user.username)

        then: "All resultant roles are tenant based roles"
        response.user.roles.role.each {
            assert it.tenantId != null
        }

        cleanup:
        utils.deleteUserQuietly(user)
    }

    def "apply_rcn_roles logic causes global roles to be returned as tenant assigned roles on all tenants in domain"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())
        Tenants tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), user.domainId).getEntity(Tenants).value

        Role role = new Role().with {
            it.name = UUID.randomUUID().toString()
            it.roleType = RoleTypeEnum.STANDARD
            it.description = "Random test role"
            it
        }
        def newRole = utils.createRole(role)

        // Globally assign new role to user
        utils.addRoleToUser(user, newRole.id)

        when: "auth"
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(user.username)

        then: "role is returned as a tenant role on both cloud/files tenants"
        tenants.tenant.each {innerTenant ->
            assert response2.user.roles.role.find { it.name == newRole.name && it.tenantId == innerTenant.id != null}
        }

        cleanup:
        utils.deleteUserQuietly(user)
    }

    /**
     * Test creates 2 cloud accounts under distinct domains, but under same RCN. A cloud account has 2 standard tenants
     * created for it (mosso/nast). Use these to verify roles are properly applied. This test doesn't specifically test
     * the inferred matching. That's left to lower level integration/unit tests.
     */
    def "apply_rcn_roles logic causes rcn roles to be returned as tenant assigned roles on all matching tenants in rcn"() {
        given:
        def userInDomain1
        def userInDomain2
        (userInDomain1, userInDomain2) = createCloudAccountsInRcn()

        def domain1 = userInDomain1.domainId
        def domain2 = userInDomain2.domainId

        Tenants domain1Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain1).getEntity(Tenants).value
        Tenants domain2Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain2).getEntity(Tenants).value
        Tenant cloudTenant1 = domain1Tenants.tenant.find {it.id == domain1}
        Tenant filesTenant1 = domain1Tenants.tenant.find() {it.id != domain1}
        Tenant cloudTenant2 = domain2Tenants.tenant.find {it.id == domain2}
        Tenant filesTenant2 = domain2Tenants.tenant.find() {it.id != domain2}

        when: "Assign RCN files role to user and auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID)
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "Use has files rcn role on both files tenants, but not cloud tenants"
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null

        when: "Now assign RCN cloud role to user as well and re-auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User has files rcn role on both files tenants, and cloud rcn role on both cloud tenants"
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} != null

        when: "Now assign RCN managed_hosting role to user as well and re-auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID)
        AuthenticateResponse response3 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User roles should not list this role since no tenants will match"
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID} == null

        cleanup:
        utils.deleteUserQuietly(userInDomain1)
        utils.deleteUserQuietly(userInDomain2)
    }

    /**
     * A user has endpoints on a tenant in one of 3 ways - explicit assignment, global assignment, and tenant type
     * endpoint assignment rules. This test verifies the user receives endpoints on tenants in external domains for
     * explicit assignment. A user receives these endpoints by having _any_ role on that tenant.
     *
     * @return
     */
    def "User receives explicit endpoints in external domains when receive rcn role on them"() {
        given:
        def userInDomain1
        def userInDomain2
        (userInDomain1, userInDomain2) = createCloudAccountsInRcn()

        def domain1 = userInDomain1.domainId
        def domain2 = userInDomain2.domainId

        Tenants domain1Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain1).getEntity(Tenants).value
        Tenants domain2Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain2).getEntity(Tenants).value
        Tenant cloudTenant1 = domain1Tenants.tenant.find {it.id == domain1}
        Tenant filesTenant1 = domain1Tenants.tenant.find() {it.id != domain1}
        Tenant cloudTenant2 = domain2Tenants.tenant.find {it.id == domain2}
        Tenant filesTenant2 = domain2Tenants.tenant.find() {it.id != domain2}

        // Get list of endpoints explicitly assigned to tenants. Not possible via API so hit service
        OpenstackEndpoint filesTenant2Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(filesTenant2.id))
        OpenstackEndpoint filesTenant1Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(filesTenant1.id))
        OpenstackEndpoint cloudTenant1Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant1.id))
        OpenstackEndpoint cloudTenant2Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant2.id))

        when: "Assign RCN files role to user and auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID)
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User receives all explicitly assigned endpoints on files tenant in external domain and files/cloud in local domain"
        filesTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert response.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        filesTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert response.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert response.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert response.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } == null
        }

        when: "Assign RCN cloud role to user and auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User receives all explicitly assigned endpoints on files/cloud tenants in both domains"
        filesTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert response2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        filesTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert response2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert response2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert response2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }

        cleanup:
        utils.deleteUserQuietly(userInDomain1)
        utils.deleteUserQuietly(userInDomain2)
    }

    def createCloudAccountsInRcn() {
        def rcn = UUID.randomUUID().toString().replaceAll("-", "")
        def userInDomain1 = utils.createCloudAccount(utils.getIdentityAdminToken())
        def userInDomain2 = utils.createCloudAccount(utils.getIdentityAdminToken())

        def domain1 = userInDomain1.domainId
        def domain2 = userInDomain2.domainId

        //update domains to share an RCN
        utils.updateDomain(domain1, v2Factory.createDomain().with {it.rackspaceCustomerNumber = rcn; it})
        utils.updateDomain(domain2, v2Factory.createDomain().with {it.rackspaceCustomerNumber = rcn; it})

        return [userInDomain1, userInDomain2]
    }
}
