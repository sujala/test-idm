package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class ApplyRcnRolesRestIntegrationTest extends RootIntegrationTest {

    @Shared def sharedServiceAdminToken
    @Shared def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared def sharedUserAdminToken

    @Shared User sharedSubUser
    @Shared def sharedSubUserToken

    @Shared String tenant_type_x = RandomStringUtils.randomAlphabetic(15).toLowerCase()
    @Shared String tenant_type_y = RandomStringUtils.randomAlphabetic(15).toLowerCase()

    @Shared Tenant tenantX
    @Shared Tenant tenantY

    @Shared EndpointTemplate endpointTemplate

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Add the tenant types for this test
        TenantType tenantTypeX = v2Factory.createTenantType(tenant_type_x, "description")
        TenantType tenantTypeY = v2Factory.createTenantType(tenant_type_y, "description")
        assert cloud20.addTenantType(sharedServiceAdminToken, tenantTypeX).status == HttpStatus.SC_CREATED
        assert cloud20.addTenantType(sharedServiceAdminToken, tenantTypeY).status == HttpStatus.SC_CREATED

        // Create a domain that has one tenant for each tenant type
        sharedUserAdmin = cloud20.createGenericAccount(sharedIdentityAdminToken)
        def tenantXId = tenant_type_x + ":" + RandomStringUtils.randomAlphabetic(20)
        tenantX = cloud20.addTenant(sharedIdentityAdminToken, v2Factory.createTenant(tenantXId, tenantXId, [tenant_type_x]).with {it.domainId = sharedUserAdmin.domainId; it}).getEntity(Tenant).value
        def tenantYId = tenant_type_y + ":" +  RandomStringUtils.randomAlphabetic(20)
        tenantY = cloud20.addTenant(sharedIdentityAdminToken, v2Factory.createTenant(tenantXId, tenantYId, [tenant_type_y]).with {it.domainId = sharedUserAdmin.domainId; it}).getEntity(Tenant).value

        // Create endpoint assignment rules so user with access to tenant gets endpoint
        def endpointRuleX = v2Factory.createTenantTypeEndpointRule(tenant_type_x, [Constants.MOSSO_ENDPOINT_TEMPLATE_ID])
        cloud20.addEndpointAssignmentRule(sharedIdentityAdminToken, endpointRuleX).status == HttpStatus.SC_CREATED
        def endpointRuleY = v2Factory.createTenantTypeEndpointRule(tenant_type_y, [Constants.MOSSO_ENDPOINT_TEMPLATE_ID])
        cloud20.addEndpointAssignmentRule(sharedIdentityAdminToken, endpointRuleY).status == HttpStatus.SC_CREATED

        authResponse = cloud20.authenticatePassword(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedUserAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser = cloud20.createSubUser(sharedUserAdminToken)
        authResponse = cloud20.authenticatePassword(sharedSubUser.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedSubUserToken = authResponse.getEntity(AuthenticateResponse).value.token.id
    }

    def setup() {
        // We use rules to verify catalog created correctly
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, true)
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "auth displays hidden tenants appropriately"() {
        when: "Neither tenant hidden"
        AuthenticateResponse uaAuthResponse = utils.authenticateApplyRcnRoles(sharedUserAdmin.username)
        AuthenticateResponse subAuthResponse = utils.authenticateApplyRcnRoles(sharedSubUser.username)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role on both tenants"
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x])
        uaAuthResponse = utils.authenticateApplyRcnRoles(sharedUserAdmin.username)
        subAuthResponse = utils.authenticateApplyRcnRoles(sharedSubUser.username)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_y])
        uaAuthResponse = utils.authenticateApplyRcnRoles(sharedUserAdmin.username)
        subAuthResponse = utils.authenticateApplyRcnRoles(sharedSubUser.username)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X & Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x, tenant_type_y])
        uaAuthResponse = utils.authenticateApplyRcnRoles(sharedUserAdmin.username)
        subAuthResponse = utils.authenticateApplyRcnRoles(sharedSubUser.username)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
    }

    def "validate displays hidden tenants appropriately"() {
        def impUserAdminToken = utils.getImpersonatedTokenWithToken(sharedIdentityAdminToken, sharedUserAdmin)
        def impSubUserToken = utils.getImpersonatedTokenWithToken(sharedIdentityAdminToken, sharedSubUser)

        when: "Neither tenant hidden"
        AuthenticateResponse uaAuthResponse = utils.validateTokenApplyRcnRoles(sharedUserAdminToken)
        AuthenticateResponse subAuthResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)
        AuthenticateResponse iuaAuthResponse = utils.validateTokenApplyRcnRoles(impUserAdminToken)
        AuthenticateResponse isubAuthResponse = utils.validateTokenApplyRcnRoles(impSubUserToken)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role on both tenants"
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
        assertHasRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x])
        uaAuthResponse = utils.validateTokenApplyRcnRoles(sharedUserAdminToken)
        subAuthResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)
        iuaAuthResponse = utils.validateTokenApplyRcnRoles(impUserAdminToken)
        isubAuthResponse = utils.validateTokenApplyRcnRoles(impSubUserToken)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
        assertMissingRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_y])
        uaAuthResponse = utils.validateTokenApplyRcnRoles(sharedUserAdminToken)
        subAuthResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)
        iuaAuthResponse = utils.validateTokenApplyRcnRoles(impUserAdminToken)
        isubAuthResponse = utils.validateTokenApplyRcnRoles(impSubUserToken)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertHasRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
        assertHasRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X & Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x, tenant_type_y])
        uaAuthResponse = utils.validateTokenApplyRcnRoles(sharedUserAdminToken)
        subAuthResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)
        iuaAuthResponse = utils.validateTokenApplyRcnRoles(impUserAdminToken)
        isubAuthResponse = utils.validateTokenApplyRcnRoles(impSubUserToken)

        then: "user-admin returned as a tenant role on both tenants"
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(iuaAuthResponse, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default returned as a tenant role only on non-hidden tenants"
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
        assertMissingRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(isubAuthResponse, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
    }

    def "listEndpointsForToken returns endpoints from hidden tenants appropriately"() {
        when: "Neither tenant hidden"
        EndpointList uaEndpointList = utils.listEndpointsForToken(sharedUserAdminToken, sharedUserAdminToken, true)
        EndpointList subEndpointList = utils.listEndpointsForToken(sharedSubUserToken, sharedSubUserToken, true)

        then: "user-admin has endpoint on both tenants"
        assertHasMossoEndpointForTenant(uaEndpointList, tenantX)
        assertHasMossoEndpointForTenant(uaEndpointList, tenantY)

        and: "identity:default has endpoint on both tenants"
        assertHasMossoEndpointForTenant(subEndpointList, tenantX)
        assertHasMossoEndpointForTenant(subEndpointList, tenantY)

        when: "Tenant X Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x])
        uaEndpointList = utils.listEndpointsForToken(sharedUserAdminToken, sharedUserAdminToken, true)
        subEndpointList = utils.listEndpointsForToken(sharedSubUserToken, sharedSubUserToken, true)

        then: "user-admin has endpoint on both tenants"
        assertHasMossoEndpointForTenant(uaEndpointList, tenantX)
        assertHasMossoEndpointForTenant(uaEndpointList, tenantY)

        and: "identity:default has endpoint only on non-hidden tenant"
        assertMissingMossoEndpointForTenant(subEndpointList, tenantX)
        assertHasMossoEndpointForTenant(subEndpointList, tenantY)

        when: "Tenant Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_y])
        uaEndpointList = utils.listEndpointsForToken(sharedUserAdminToken, sharedUserAdminToken, true)
        subEndpointList = utils.listEndpointsForToken(sharedSubUserToken, sharedSubUserToken, true)

        then: "user-admin has endpoint on both tenants"
        assertHasMossoEndpointForTenant(uaEndpointList, tenantX)
        assertHasMossoEndpointForTenant(uaEndpointList, tenantY)

        and: "identity:default has endpoint only on non-hidden tenant"
        assertHasMossoEndpointForTenant(subEndpointList, tenantX)
        assertMissingMossoEndpointForTenant(subEndpointList, tenantY)

        when: "Tenant X & Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x, tenant_type_y])
        uaEndpointList = utils.listEndpointsForToken(sharedUserAdminToken, sharedUserAdminToken, true)
        subEndpointList = utils.listEndpointsForToken(sharedSubUserToken, sharedSubUserToken, true)

        then: "user-admin has endpoint on both tenants"
        assertHasMossoEndpointForTenant(uaEndpointList, tenantX)
        assertHasMossoEndpointForTenant(uaEndpointList, tenantY)

        and: "identity:default does not have endpoint on either tenant"
        assertMissingMossoEndpointForTenant(subEndpointList, tenantX)
        assertMissingMossoEndpointForTenant(subEndpointList, tenantY)
    }

    def "listTenantsForToken returns hidden tenants appropriately"() {
        when: "Neither tenant hidden"
        Tenants uaTenants = utils.listTenantsForToken(sharedUserAdminToken,  true)
        Tenants subTenants = utils.listTenantsForToken(sharedSubUserToken, true)

        then: "user-admin has both tenants"
        assertHasTenant(uaTenants, tenantX)
        assertHasTenant(uaTenants, tenantY)

        and: "identity:default has both tenants"
        assertHasTenant(subTenants, tenantX)
        assertHasTenant(subTenants, tenantY)

        when: "Tenant X Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x])
        uaTenants = utils.listTenantsForToken(sharedUserAdminToken,  true)
        subTenants = utils.listTenantsForToken(sharedSubUserToken, true)

        then: "user-admin has both tenants"
        assertHasTenant(uaTenants, tenantX)
        assertHasTenant(uaTenants, tenantY)

        and: "identity:default has only on non-hidden tenant"
        assertMissingTenant(subTenants, tenantX)
        assertHasTenant(subTenants, tenantY)

        when: "Tenant Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_y])
        uaTenants = utils.listTenantsForToken(sharedUserAdminToken,  true)
        subTenants = utils.listTenantsForToken(sharedSubUserToken, true)

        then: "user-admin has both tenants"
        assertHasTenant(uaTenants, tenantX)
        assertHasTenant(uaTenants, tenantY)

        and: "identity:default has only non-hidden tenant"
        assertHasTenant(subTenants, tenantX)
        assertMissingTenant(subTenants, tenantY)

        when: "Tenant X & Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x, tenant_type_y])
        uaTenants = utils.listTenantsForToken(sharedUserAdminToken,  true)
        subTenants = utils.listTenantsForToken(sharedSubUserToken, true)

        then: "user-admin has both tenants"
        assertHasTenant(uaTenants, tenantX)
        assertHasTenant(uaTenants, tenantY)

        and: "identity:default does not have either tenant"
        assertMissingTenant(subTenants, tenantX)
        assertMissingTenant(subTenants, tenantY)
    }

    def "listRolesForUserOnTenant returns hidden tenants appropriately"() {
        when: "Neither tenant hidden"
        RoleList uaXRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantX, sharedIdentityAdminToken,  true)
        RoleList uaYRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantY, sharedIdentityAdminToken,  true)
        RoleList subXRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantX, sharedIdentityAdminToken,  true)
        RoleList subYRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantY, sharedIdentityAdminToken,  true)

        then: "user-admin has role on both tenants"
        assertHasRoleOnTenant(uaXRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaYRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default has role on both tenants"
        assertHasRoleOnTenant(subXRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subYRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x])
        uaXRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantX, sharedIdentityAdminToken,  true)
        uaYRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantY, sharedIdentityAdminToken,  true)
        subXRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantX, sharedIdentityAdminToken,  true)
        subYRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantY, sharedIdentityAdminToken,  true)

        then: "user-admin has role on both tenants"
        assertHasRoleOnTenant(uaXRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaYRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default has role on only non-hidden tenant"
        assertMissingRoleOnTenant(subXRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertHasRoleOnTenant(subYRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_y])
        uaXRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantX, sharedIdentityAdminToken,  true)
        uaYRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantY, sharedIdentityAdminToken,  true)
        subXRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantX, sharedIdentityAdminToken,  true)
        subYRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantY, sharedIdentityAdminToken,  true)

        then: "user-admin has role on both tenants"
        assertHasRoleOnTenant(uaXRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaYRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default has role on only non-hidden tenant"
        assertHasRoleOnTenant(subXRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subYRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantY)

        when: "Tenant X & Y Hidden"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, [tenant_type_x, tenant_type_y])
        uaXRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantX, sharedIdentityAdminToken,  true)
        uaYRoleList = utils.listRolesForUserOnTenant(sharedUserAdmin, tenantY, sharedIdentityAdminToken,  true)
        subXRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantX, sharedIdentityAdminToken,  true)
        subYRoleList = utils.listRolesForUserOnTenant(sharedSubUser, tenantY, sharedIdentityAdminToken,  true)

        then: "user-admin has role on both tenants"
        assertHasRoleOnTenant(uaXRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantX)
        assertHasRoleOnTenant(uaYRoleList, IdentityUserTypeEnum.USER_ADMIN, tenantY)

        and: "identity:default does not have role on either hidden tenant"
        assertMissingRoleOnTenant(subXRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantX)
        assertMissingRoleOnTenant(subYRoleList, IdentityUserTypeEnum.DEFAULT_USER, tenantY)
    }

    void assertHasRoleOnTenant(RoleList roleList, IdentityUserTypeEnum role, Tenant tenant) {
        roleList.role.find {it.name == role.roleName && it.tenantId == tenant.id} != null
    }

    void assertMissingRoleOnTenant(RoleList roleList, IdentityUserTypeEnum role, Tenant tenant) {
        roleList.role.find {it.name == role.roleName && it.tenantId == tenant.id} == null
    }

    void assertHasRoleOnTenant(AuthenticateResponse response, IdentityUserTypeEnum role, Tenant tenant) {
        response.user.roles.role.find {it.name == role.roleName && it.tenantId == tenant.id} != null
    }

    void assertMissingRoleOnTenant(AuthenticateResponse response, IdentityUserTypeEnum role, Tenant tenant) {
        response.user.roles.role.find {it.name == role.roleName && it.tenantId == tenant.id} == null
    }

    void assertHasMossoEndpointForTenant(EndpointList list, Tenant tenant) {
        assert list.endpoint.find {it.id == Integer.parseInt(Constants.MOSSO_ENDPOINT_TEMPLATE_ID) && it.tenantId == tenant.id} != null
    }

    void assertMissingMossoEndpointForTenant(EndpointList list, Tenant tenant) {
        assert list.endpoint.find {it.id == Integer.parseInt(Constants.MOSSO_ENDPOINT_TEMPLATE_ID) && it.tenantId == tenant.id} == null
    }

    void assertHasTenant(Tenants list, Tenant tenant) {
        assert list.tenant.find {it.id == tenant.id} != null
    }

    void assertMissingTenant(Tenants list, Tenant tenant) {
        assert list.tenant.find {it.id == tenant.id} == null
    }
}
