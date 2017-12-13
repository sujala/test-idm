package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang.RandomStringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class AutoAssignRoleIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Unroll
    def "tenants can be excluded from the auto-assign role based on feature flag, excludeTenantType == #excludeTenantType"() {
        given:
        def domainId = utils.createDomain()
        def tenantType = utils.createTenantType()
        if (excludeTenantType) {
            reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, "${tenantType.name}".toString())
        }
        def users, identityAdmin, userAdmin, userManager, defaultUser
        (identityAdmin, userAdmin, userManager, defaultUser) = utils.createUsers(domainId)
        users = [defaultUser, userManager, userAdmin, identityAdmin]
        def tenant = utils.createTenant(testUtils.getRandomUUID("${tenantType.name}:"), true, RandomStringUtils.randomAlphanumeric(8), domainId)

        when: "auth as the users in the domain"
        def userAdminAuthResponse = utils.authenticate(userAdmin.username)
        def userManagerAuthResponse = utils.authenticate(userManager.username)
        def defaultUserAuthResponse = utils.authenticate(defaultUser.username)
        def fedUserAuthResponse = utils.createFederatedUserForAuthResponse(domainId)

        then: "the auto-assigned tenant role is assigned to the users based on the tenant type being in the exclude config"
        assertAutoAssignRoleOnTenantInAuthResponse(userAdminAuthResponse, tenant, false)
        assertAutoAssignRoleOnTenantInAuthResponse(userManagerAuthResponse, tenant, false)
        assertAutoAssignRoleOnTenantInAuthResponse(defaultUserAuthResponse, tenant, excludeTenantType)
        assertAutoAssignRoleOnTenantInAuthResponse(fedUserAuthResponse, tenant, excludeTenantType)

        when: "validate the tokens"
        def userAdminValidateTokenResponse = utils.validateToken(userAdminAuthResponse.token.id)
        def userManagerValidateTokenResponse = utils.validateToken(userManagerAuthResponse.token.id)
        def defaultUserValidateTokenResponse = utils.validateToken(defaultUserAuthResponse.token.id)
        def fedUserValidateTokenResponse = utils.validateToken(fedUserAuthResponse.token.id)

        then: "the auto-assigned tenant role is assigned to the users based on the tenant type being in the exclude config"
        assertAutoAssignRoleOnTenantInAuthResponse(userAdminValidateTokenResponse, tenant, false)
        assertAutoAssignRoleOnTenantInAuthResponse(userManagerValidateTokenResponse, tenant, false)
        assertAutoAssignRoleOnTenantInAuthResponse(defaultUserValidateTokenResponse, tenant, excludeTenantType)
        assertAutoAssignRoleOnTenantInAuthResponse(fedUserValidateTokenResponse, tenant, excludeTenantType)

        when: "list users for tenant"
        def usersWithTenant = utils.listUsersWithTenant(tenant.id)

        then: "only the users who should get the auto-assign role based on the exclude tenant type property are returned"
        usersWithTenant.user.username.contains(userAdmin.username)
        usersWithTenant.user.username.contains(userManager.username)
        usersWithTenant.user.username.contains(defaultUser.username) == !excludeTenantType
        // List users for tenant does not support federated users
        // usersWithTenant.user.username.contains(fedUserAuthResponse.user.name) == !excludeTenantType

        when: "list users for tenant with role"
        def usersForTenantWithRole = utils.listUsersWithTenantAndRole(tenant.id, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID)

        then: "only the users who should get the auto-assign role based on the exclude tenant type property are returned"
        usersForTenantWithRole.user.username.contains(userAdmin.username)
        usersForTenantWithRole.user.username.contains(userManager.username)
        usersForTenantWithRole.user.username.contains(defaultUser.username) == !excludeTenantType
        // List users with role on tenant does not support federated users
        // usersForTenantWithRole.user.username.contains(fedUserAuthResponse.user.name) == !excludeTenantType

        when: "list roles user has on the tenant"
        def userAdminListRoles = utils.listRolesForUserOnTenant(userAdmin, tenant)
        def userManagerListRoles = utils.listRolesForUserOnTenant(userManager, tenant)
        def defaultUserListRoles = utils.listRolesForUserOnTenant(defaultUser, tenant)
        // List roles for user on tenant does not support federated users
        // def fedUserListRoles = utils.listRolesForUserOnTenant(new User().with {it.id = fedUserAuthResponse.user.id; it}, tenant)

        then: "the user has the auto-assign role on the tenant based on the exclude config"
        assertAutoAssignRoleOnTenantInRoleList(userAdminListRoles, tenant, false)
        assertAutoAssignRoleOnTenantInRoleList(userManagerListRoles, tenant, false)
        assertAutoAssignRoleOnTenantInRoleList(defaultUserListRoles, tenant, excludeTenantType)
        // List roles for user on tenant does not support federated users
        // assertAutoAssignRoleOnTenantInRoleList(fedUserListRoles, tenant, excludeTenantType)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)

        where:
        excludeTenantType << [true, false]
    }

    void assertAutoAssignRoleOnTenantInAuthResponse(AuthenticateResponse authenticateResponse, Tenant tenant, boolean tenantExcluded) {
        // first assert that the auto-assign role is not displayed as a global role
        assert authenticateResponse.user.roles.role.find { role -> role.name == IdentityRole.IDENTITY_TENANT_ACCESS.roleName && role.tenantId == null } == null
        def autoRoleOnTenant = authenticateResponse.user.roles.role.find { role -> role.name == IdentityRole.IDENTITY_TENANT_ACCESS.roleName && role.tenantId == tenant.id }
        assert tenantExcluded ? autoRoleOnTenant == null : autoRoleOnTenant != null
    }

    void assertAutoAssignRoleOnTenantInRoleList(RoleList roleList, Tenant tenant, boolean tenantExcluded) {
        // first assert that the auto-assign role is not a global role
        assert roleList.role.find { role -> role.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantId == null } == null
        def autoRoleOnTenant = roleList.role.find { role -> role.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantId == tenant.id}
        assert tenantExcluded ? autoRoleOnTenant == null : autoRoleOnTenant != null
    }

}
