package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
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

    /**
     * Tests whether the rcn role logic is applied to the user's roles based on query param. Does this by using
     * the user's Identity Classification role is denormalized and set on individual tenants as the canary
     * @return
     */
    @Unroll
    def "auth/validate: apply_rcn_roles logic only used when apply_rcn_roles = true (case insensitive): value: #param"() {
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

        and: "validate returns same"
        AuthenticateResponse valResponse = utils.validateTokenApplyRcnRoles(response2.token.id, param)
        def foundRole = valResponse.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        foundRole != null
        if (applied) {
            assert foundRole.getTenantId() != null
        } else {
            assert foundRole.getTenantId() == null
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

    def "auth/validate: apply_rcn_roles returns no global roles and user's domain"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature enabled"
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(user.username)

        then: "All resultant roles are tenant based roles"
        response.user.roles.role.each {
            assert it.tenantId != null
        }

        and: "Users domain is returned"
        response.user.domainId == user.domainId

        and: "validate shows same"
        AuthenticateResponse valResponse = utils.validateTokenApplyRcnRoles(response.token.id)
        valResponse.user.roles.role.each {
            assert it.tenantId != null
        }
        valResponse.user.domainId == user.domainId

        and: "validate impersonation shows same"
        AuthenticateResponse valImpResponse = utils.validateTokenApplyRcnRoles(utils.getImpersonatedTokenWithToken(utils.getIdentityAdminToken(), user))
        valImpResponse.user.roles.role.each {
            assert it.tenantId != null
        }
        valResponse.user.domainId == user.domainId

        cleanup:
        utils.deleteUserQuietly(user)
    }

    def "auth/validate: apply_rcn_roles logic returns global roles as tenant assigned roles on all tenants in domain for user-admin"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        def tenantId = "faws:" + testUtils.getRandomUUID()
        def protectedTenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, [Constants.TENANT_TYPE_PROTECTED_PREFIX]).with {it.domainId = user.domainId; it})

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

        and: "validate shows same"
        AuthenticateResponse valResponse = utils.validateTokenApplyRcnRoles(response2.token.id)
        tenants.tenant.each {innerTenant ->
            assert valResponse.user.roles.role.find { it.name == newRole.name && it.tenantId == innerTenant.id != null}
        }

        and: "validate impersonation token shows same"
        AuthenticateResponse valImpResponse = utils.validateTokenApplyRcnRoles(utils.getImpersonatedTokenWithToken(utils.getIdentityAdminToken(), user))
        tenants.tenant.each {innerTenant ->
            assert valImpResponse.user.roles.role.find { it.name == newRole.name && it.tenantId == innerTenant.id != null}
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

        then: "User has files rcn role on both files tenants, but not cloud tenants"
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        response.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null

        and: "validate shows same"
        AuthenticateResponse valResponse = utils.validateTokenApplyRcnRoles(response.token.id)
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        valResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null

        and: "validate impersonation token shows same"
        AuthenticateResponse valImpResponse = utils.validateTokenApplyRcnRoles(utils.getImpersonatedTokenWithToken(utils.getIdentityAdminToken(), userInDomain1))
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} == null
        valImpResponse.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} == null

        when: "Now assign RCN cloud role to user as well and re-auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User has files rcn role on both files tenants, and cloud rcn role on both cloud tenants"
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} != null
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} != null

        and: "validate shows same"
        AuthenticateResponse valResponse2 = utils.validateTokenApplyRcnRoles(response2.token.id)
        valResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        valResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        valResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} != null
        valResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} != null

        and: "validate impersonation token shows same"
        AuthenticateResponse valImpResponse2 = utils.validateTokenApplyRcnRoles(utils.getImpersonatedTokenWithToken(utils.getIdentityAdminToken(), userInDomain1))
        valImpResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant1.id} != null
        valImpResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_FILES_TENANT_ROLE_ID && it.tenantId == filesTenant2.id} != null
        valImpResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant1.id} != null
        valImpResponse2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID && it.tenantId == cloudTenant2.id} != null

        when: "Now assign RCN managed_hosting role to user as well and re-auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID)
        AuthenticateResponse response3 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "User roles should not list this role since no tenants will match"
        response2.user.roles.role.find {it.id == Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID} == null

        and: "validate shows same"
        AuthenticateResponse valResponse3 = utils.validateTokenApplyRcnRoles(response3.token.id)
        valResponse3.user.roles.role.find {it.id == Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID} == null

        and: "validate impersonation token shows same"
        AuthenticateResponse valImpResponse3 = utils.validateTokenApplyRcnRoles(utils.getImpersonatedTokenWithToken(utils.getIdentityAdminToken(), userInDomain1))
        valImpResponse3.user.roles.role.find {it.id == Constants.IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID} == null

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
        AuthenticateResponse authResponse = utils.authenticateApplyRcnRoles(userInDomain1.username)
        def token = authResponse.token.id
        EndpointList listEndpointsReponse = utils.listEndpointsForToken(token, token, true)

        then: "User receives all explicitly assigned endpoints on files tenant in external domain and files/cloud in local domain"
        filesTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse.endpoint.find { it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        filesTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse.endpoint.find { it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } == null
            assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } == null
        }

        when: "Assign RCN cloud role to user and auth"
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse authResponse2 = utils.authenticateApplyRcnRoles(userInDomain1.username)
        EndpointList listEndpointsReponse2 = utils.listEndpointsForToken(token, token, true)

        then: "User receives all explicitly assigned endpoints on files/cloud tenants in both domains"
        filesTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse2.endpoint.find { it.tenantId == filesTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        filesTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse2.endpoint.find { it.tenantId == filesTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse2.endpoint.find { it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }
        cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert authResponse2.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } != null
            assert listEndpointsReponse2.endpoint.find { it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }

        cleanup:
        utils.deleteUserQuietly(userInDomain1)
        utils.deleteUserQuietly(userInDomain2)
    }

    @Unroll
    def "test apply rcn query peram works for list endpoints for token - applyRcnRoles = #applyRcnRoles"() {
        given:
        def userInDomain1
        def userInDomain2
        (userInDomain1, userInDomain2) = createCloudAccountsInRcn()
        def domain1 = userInDomain1.domainId
        def domain2 = userInDomain2.domainId
        Tenants domain1Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain1).getEntity(Tenants).value
        Tenants domain2Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain2).getEntity(Tenants).value
        Tenant cloudTenant1 = domain1Tenants.tenant.find {it.id == domain1}
        Tenant cloudTenant2 = domain2Tenants.tenant.find {it.id == domain2}
        OpenstackEndpoint cloudTenant1Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant1.id))
        OpenstackEndpoint cloudTenant2Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant2.id))
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse authResponse = utils.authenticateApplyRcnRoles(userInDomain1.username)
        def token = authResponse.token.id

        when: "list endpoints for token"
        EndpointList listEndpointsReponse = utils.listEndpointsForToken(token, token, applyRcnRoles)

        then: "the user always has the endpoints in their domain"
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }

        and: "but only shows endpoints in the other domain when applying RCN roles"
        if (applyRcnRoles) {
            cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
                assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } != null
            }
        } else {
            cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
                assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } == null
            }
        }

        cleanup:
        utils.deleteUserQuietly(userInDomain1)
        utils.deleteUserQuietly(userInDomain2)

        where:
        applyRcnRoles << [true, false]
    }

    @Unroll
    def "test list endpoints for token only returns RCN endpoints"() {
        given:
        def userInDomain1
        def userInDomain2
        (userInDomain1, userInDomain2) = createCloudAccountsInRcn()
        def domain1 = userInDomain1.domainId
        def domain2 = userInDomain2.domainId
        Tenants domain1Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain1).getEntity(Tenants).value
        Tenants domain2Tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), domain2).getEntity(Tenants).value
        Tenant cloudTenant1 = domain1Tenants.tenant.find {it.id == domain1}
        Tenant cloudTenant2 = domain2Tenants.tenant.find {it.id == domain2}
        OpenstackEndpoint cloudTenant1Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant1.id))
        OpenstackEndpoint cloudTenant2Endpoint = endpointService.getOpenStackEndpointForTenant(tenantService.getTenant(cloudTenant2.id))
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        AuthenticateResponse authResponse = utils.authenticateApplyRcnRoles(userInDomain1.username)
        def token = authResponse.token.id

        when: "list endpoints for token"
        EndpointList listEndpointsReponse = utils.listEndpointsForToken(token, token, true)

        then: "the user always has the endpoints in their domain"
        cloudTenant1Endpoint.getBaseUrls().each {baseUrl ->
            assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant1.id && it.publicURL == baseUrl.publicUrl } != null
        }

        and: "but only shows endpoints in the other domain when applying RCN roles and performant service catalog enabled"
        cloudTenant2Endpoint.getBaseUrls().each {baseUrl ->
            assert listEndpointsReponse.endpoint.find { it.tenantId == cloudTenant2.id && it.publicURL == baseUrl.publicUrl } != null
        }

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
    def "User can authenticate against a tenant from RCN domain and limit service catalog to that tenant's endpoints"() {
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

        // Add uber RCN role to user so receives role on all tenants in all RCN's domains
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID)

        def token = utils.getToken(userInDomain1.username)

        when: "Auth w/ apply_rcn_roles and specify cloud tenant in external domain"
        def response = cloud20.authenticateTokenAndTenantApplyRcn(token, cloudTenant2.id, "true")

        then: "Auth is successful"
        response.status == HttpStatus.SC_OK

        and: "Endpoints are limited to those on the specified tenant"
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value
        assert authResponse.serviceCatalog.service.endpoint.flatten().find {it.tenantId != cloudTenant2.id} == null

        and: "access.token.tenant is the specified tenant from external domain"
        authResponse.token.tenant.id == cloudTenant2.id

        when: "Auth w/ apply_rcn_roles and specify files tenant in external domain"
        def response2 = cloud20.authenticateTokenAndTenantApplyRcn(token, filesTenant2.id, "true")

        then: "Auth is successful"
        response2.status == HttpStatus.SC_OK

        and: "Endpoints are limited to those on the specified tenant"
        AuthenticateResponse authResponse2 = response2.getEntity(AuthenticateResponse).value
        assert authResponse2.serviceCatalog.service.endpoint.flatten().find {it.tenantId != filesTenant2.id} == null

        and: "access.token.tenant is the files tenant in external domain"
        authResponse2.token.tenant.id == filesTenant2.id

        when: "Auth w/ apply_rcn_roles and specify own domain's cloud tenant"
        def response3 = cloud20.authenticateTokenAndTenantApplyRcn(token, cloudTenant1.id, "true")

        then: "Auth is successful"
        response3.status == HttpStatus.SC_OK

        and: "access.token.tenant is the local cloud tenant"
        AuthenticateResponse authResponse3 = response3.getEntity(AuthenticateResponse).value
        authResponse3.token.tenant != null
        authResponse3.token.tenant.id == cloudTenant1.id

        and: "Endpoints for all tenants are returned"
        assert authResponse3.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id} != null
        assert authResponse3.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id} != null
        assert authResponse3.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id} != null
        assert authResponse3.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id} != null

        when: "Auth w/ apply_rcn_roles and don't specify a tenant"
        AuthenticateResponse authResponse4 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "access.token.tenant is the local cloud tenant since have compute:default role on it"
        authResponse4.token.tenant != null
        authResponse4.token.tenant.id == cloudTenant1.id

        and: "Endpoints for all tenants are returned"
        assert authResponse4.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id} != null
        assert authResponse4.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id} != null
        assert authResponse4.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id} != null
        assert authResponse4.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id} != null

        when: "Remove compute:default role from tenant1 and don't specify tenant"
        utils.deleteRoleFromUserOnTenant(userInDomain1, cloudTenant1, Constants.DEFAULT_COMPUTE_ROLE_ID)
        AuthenticateResponse authResponse5 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "access.token.tenant is null since when RCN applied, only based on compute:default role"
        authResponse5.token.tenant == null

        and: "Endpoints for all tenants are returned"
        assert authResponse5.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant1.id} != null
        assert authResponse5.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id} != null
        assert authResponse5.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id} != null
        assert authResponse5.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id} != null

        when: "Delete tenant1 and don't specify tenant"
        utils.deleteTenant(cloudTenant1)
        AuthenticateResponse authResponse6 = utils.authenticateApplyRcnRoles(userInDomain1.username)

        then: "access.token.tenant is null since no compute:default role on tenant or all numeric tenant in domain1"
        authResponse6.token.tenant == null

        and: "Endpoints for all remaining tenants are returned"
        assert authResponse6.serviceCatalog.service.endpoint.flatten().find {it.tenantId == cloudTenant2.id} != null
        assert authResponse6.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant1.id} != null
        assert authResponse6.serviceCatalog.service.endpoint.flatten().find {it.tenantId == filesTenant2.id} != null

        cleanup:
        utils.deleteUserQuietly(userInDomain1)
        utils.deleteUserQuietly(userInDomain2)
    }

    /**
     * Access token tenant returned from validate always returns the local domain's compute:default tenant or none
     * when apply_rcn_roles applied. When not applied, returns the compute:default tenant or the all numeric tenant
     *
     * @return
     */
    def "Verify validate returns appropriate access.token.tenant under various circumstances "() {
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

        // Add uber RCN role to user so receives role on all tenants in all RCN's domains
        utils.addRoleToUser(userInDomain1, Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID)
        def token = utils.getToken(userInDomain1.username)

        when: "Validate w/ apply_rcn_roles w/ access to all tenants in both domains"
        AuthenticateResponse valResponse = utils.validateTokenApplyRcnRoles(token, "true")
        AuthenticateResponse valResponseLegacy = utils.validateTokenApplyRcnRoles(token, "false")

        then: "tenant is the local domain's cloud tenant when applyign RCN"
        valResponse.token.tenant != null
        valResponse.token.tenant.id == cloudTenant1.id

        and: "tenant is not null when not applying RCN and set to local cloud tenant"
        valResponseLegacy.token.tenant != null
        valResponseLegacy.token.tenant.id == cloudTenant1.id

        when: "Remove compute:default role from cloudTenant1; leaving on cloudTenant2 when rcn applied"
        utils.deleteRoleFromUserOnTenant(userInDomain1, cloudTenant1, Constants.DEFAULT_COMPUTE_ROLE_ID)
        AuthenticateResponse valResponse2 = utils.validateTokenApplyRcnRoles(token, "true")
        AuthenticateResponse valResponseLegacy2 = utils.validateTokenApplyRcnRoles(token, "false")

        then: "tenant is null when applying RCN"
        valResponse2.token.tenant == null

        and: "tenant is not null when not applying RCN and is set to cloudTenant1 - an all numeric domain"
        valResponseLegacy2.token.tenant != null
        valResponseLegacy2.token.tenant.id == cloudTenant1.id

        when: "Delete tenant1"
        utils.deleteTenant(cloudTenant1)
        AuthenticateResponse valResponse3 = utils.validateTokenApplyRcnRoles(token, "true")
        AuthenticateResponse valResponseLegacy3 = utils.validateTokenApplyRcnRoles(token, "false")

        then: "access.token.tenant is null when rcn applied since no compute:default role on tenant in domain1"
        valResponse3.token.tenant == null

        and: "access.token.tenant is null when rcn not applied since no compute:default role on tenant or all numeric tenant in domain1"
        valResponseLegacy3.token.tenant == null

        when: "Give user explicit access to compute tenant in domain2 via compute default role"
        utils.addRoleToUserOnTenant(userInDomain1, cloudTenant2, Constants.DEFAULT_COMPUTE_ROLE_ID)
        AuthenticateResponse valResponse4 = utils.validateTokenApplyRcnRoles(token, "true")
        AuthenticateResponse valResponseLegacy4 = utils.validateTokenApplyRcnRoles(token, "false")

        then: "access.token.tenant is null when rcn applied since no compute:default role on tenant in domain1"
        valResponse4.token.tenant == null

        and: "access.token.tenant is cloud2 tenant since have compute:default role on it"
        valResponseLegacy4.token.tenant != null
        valResponseLegacy4.token.tenant.id == cloudTenant2.id

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
        utils.domainRcnSwitch(domain1, rcn)
        utils.domainRcnSwitch(domain2, rcn)

        return [userInDomain1, userInDomain2]
    }
}
