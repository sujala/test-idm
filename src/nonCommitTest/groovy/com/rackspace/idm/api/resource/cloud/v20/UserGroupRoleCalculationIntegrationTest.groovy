package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.*
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID

class UserGroupRoleCalculationIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    EndpointDao endpointDao

    @Autowired
    ApplicationDao applicationDao

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    DomainDao domainDao

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared
    def sharedUserWithNoGroups

    @Shared
    def sharedUserOnGroupNoRoles

    @Shared
    def sharedUserOnGroupWithRoles

    @Shared
    def sharedCloudTenant

    @Shared
    def sharedFilesTenant

    @Shared
    def groupNoRoles

    @Shared
    def groupWithRoles

    /**
     * Creates a set of users where:
     * 1. sharedUserWithNoGroups - a user that belong to no groups
     * 2. sharedUserOnGroupNoRoles - a user that belongs to a group that is not assigned any roles
     * 3. sharedUserOnGroupWithRoles - a user belong to a group that has a few roles
     */
    void doSetupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a cloud account
        sharedUserWithNoGroups = cloud20.createCloudAccount(sharedIdentityAdminToken)
        def userAdminToken = cloud20.authenticatePassword(sharedUserWithNoGroups.username).getEntity(AuthenticateResponse).value.token.id
        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserWithNoGroups.domainId).getEntity(Tenants).value
        sharedCloudTenant = tenants.tenant.find {
            it.id == sharedUserWithNoGroups.domainId
        }
        sharedFilesTenant = tenants.tenant.find() {
            it.id != sharedUserWithNoGroups.domainId
        }

        // Create the group w/o roles
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserWithNoGroups.domainId
            it.name = "noRoles_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        groupNoRoles = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        // Create the group w roles
        group = new UserGroup().with {
            it.domainId = sharedUserWithNoGroups.domainId
            it.name = "withRoles_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [sharedFilesTenant.id]))
                    tas
            }
            it
        }
        groupWithRoles = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)
        assert cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, groupWithRoles, assignments).status == HttpStatus.SC_OK

        // Create a subuser as member of noRoles group
        User userForCreate = v2Factory.createUserForCreate("memberNoRolesGroup_" + RandomStringUtils.randomAlphanumeric(10), "display", "email@email.com", true, null, null, "Password1")
        sharedUserOnGroupNoRoles = cloud20.createUser(userAdminToken, userForCreate).getEntity(User).value
        assert cloud20.addUserToUserGroup(sharedIdentityAdminToken, sharedUserOnGroupNoRoles.domainId, groupNoRoles.id, sharedUserOnGroupNoRoles.id).status == HttpStatus.SC_NO_CONTENT

        // Create a subuser as member of withRoles group
        userForCreate = v2Factory.createUserForCreate("memberRolesGroup_" + RandomStringUtils.randomAlphanumeric(10), "display", "email@email.com", true, null, null, "Password1")
        sharedUserOnGroupWithRoles = cloud20.createUser(userAdminToken, userForCreate).getEntity(User).value
        assert cloud20.addUserToUserGroup(sharedIdentityAdminToken, sharedUserOnGroupWithRoles.domainId, groupWithRoles.id, sharedUserOnGroupWithRoles.id).status == HttpStatus.SC_NO_CONTENT
    }

    @Unroll
    def "Auth/Validate: cloud user w/o group does not contain rbac role when group role calculation is enabled: #useGroupMembership"() {
        when:
        AuthenticateResponse authResponse = utils.authenticate(sharedUserWithNoGroups)
        AuthenticateResponse valResponse = utils.validateToken(authResponse.token.id)

        then:
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)

        when: "applyRCNRoles"
        authResponse = utils.authenticateApplyRcnRoles(sharedUserWithNoGroups)
        valResponse = utils.validateTokenApplyRcnRoles(authResponse.token.id)

        then:
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)

        where:
        useGroupMembership << [true, false]
    }

    @Unroll
    def "Auth/Validate: user w/o role on group when group role calculation is enabled: #useGroupMembership"() {
        when:
        AuthenticateResponse authResponse = utils.authenticate(sharedUserOnGroupNoRoles)
        AuthenticateResponse valResponse = utils.validateToken(authResponse.token.id)

        then:
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)

        when: "applyRcnRoles"
        authResponse = utils.authenticateApplyRcnRoles(sharedUserOnGroupNoRoles)
        valResponse = utils.validateTokenApplyRcnRoles(authResponse.token.id)

        then:
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)

        where:
        useGroupMembership << [true, false]
    }

    @Unroll
    def "Auth/Validate: user w/ membership on group with roles when group role calculation is enabled: #useGroupMembership"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, useGroupMembership)

        when:
        def authResponse = utils.authenticate(sharedUserOnGroupWithRoles)
        def valResponse = utils.validateToken(authResponse.token.id)

        then:
        if (useGroupMembership) {
            verifyAuthenticateResponseContainsAssignment(authResponse, ROLE_RBAC1_ID, [])
            verifyAuthenticateResponseContainsAssignment(authResponse, ROLE_RBAC2_ID, [sharedFilesTenant.id])
            verifyAuthenticateResponseContainsAssignment(valResponse, ROLE_RBAC1_ID, [])
            verifyAuthenticateResponseContainsAssignment(valResponse, ROLE_RBAC2_ID, [sharedFilesTenant.id])
        } else {
            verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
            verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)
        }

        when: "applyRcnRoles"
        authResponse = utils.authenticateApplyRcnRoles(sharedUserOnGroupWithRoles)
        valResponse = utils.validateTokenApplyRcnRoles(authResponse.token.id)

        then:
        if (useGroupMembership) {
            // rcn logic expands the domain assigned roles to all tenants
            verifyAuthenticateResponseContainsAssignment(authResponse, ROLE_RBAC1_ID, [sharedCloudTenant.id, sharedFilesTenant.id])
            verifyAuthenticateResponseContainsAssignment(authResponse, ROLE_RBAC2_ID, [sharedFilesTenant.id])
            verifyAuthenticateResponseContainsAssignment(valResponse, ROLE_RBAC1_ID, [sharedCloudTenant.id, sharedFilesTenant.id])
            verifyAuthenticateResponseContainsAssignment(valResponse, ROLE_RBAC2_ID, [sharedFilesTenant.id])
        } else {
            verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(authResponse.user.roles, ROLE_RBAC2_ID)
            verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(valResponse.user.roles, ROLE_RBAC2_ID)
        }

        where:
        useGroupMembership << [true, false]
    }

    @Unroll
    def "List global roles for user: w/ membership on group with roles. group role calculation is enabled: #useGroupMembership"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, useGroupMembership)

        when:
        RoleList roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [])
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        when: "applyRcnRoles"
        roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id, null, true).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [])
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        where:
        useGroupMembership << [true, false]
    }

    @Unroll
    def "List global roles for user on service id: w/ membership on group with roles. group role calculation is enabled: #useGroupMembership"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, useGroupMembership)

        when: "identity roles"
        RoleList roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id, Constants.IDENTITY_SERVICE_ID).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [])
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        when: "server roles"
        roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id, Constants.SERVERS_SERVICE_ID).getEntity(RoleList).value

        then:
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)

        when: "applyRcnRoles w/ identity roles"
        roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id, Constants.IDENTITY_SERVICE_ID, true).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [])
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        when: "applyRcnRoles w/ server roles"
        roleList = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), sharedUserOnGroupWithRoles.id, Constants.SERVERS_SERVICE_ID, true).getEntity(RoleList).value

        then:
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)

        where:
        useGroupMembership << [true, false]
    }

    @Unroll
    def "List roles for user on tenant: w/ membership on group with roles. group role calculation is enabled: #useGroupMembership"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, useGroupMembership)

        when: "query cloud tenant"
        RoleList roleList = cloud20.listRolesForUserOnTenant(utils.getIdentityAdminToken(), sharedCloudTenant.id, sharedUserOnGroupWithRoles.id).getEntity(RoleList).value

        then:
        // Neither role is explicitly assigned on the cloud tenant
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)

        when: "query cloud tenant with applyRcnRoles"
        roleList = cloud20.listRolesForUserOnTenant(utils.getIdentityAdminToken(), sharedCloudTenant.id, sharedUserOnGroupWithRoles.id, true).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [sharedCloudTenant.id])
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        when: "query files tenant"
        roleList = cloud20.listRolesForUserOnTenant(utils.getIdentityAdminToken(), sharedFilesTenant.id, sharedUserOnGroupWithRoles.id).getEntity(RoleList).value

        then:
        verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC2_ID, [sharedFilesTenant.id])
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        when: "query files tenant with applyRcnRoles"
        roleList = cloud20.listRolesForUserOnTenant(utils.getIdentityAdminToken(), sharedFilesTenant.id, sharedUserOnGroupWithRoles.id, true).getEntity(RoleList).value

        then:
        if (useGroupMembership) {
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC1_ID, [sharedFilesTenant.id])
            verifyRoleListContainsAssignment(roleList, ROLE_RBAC2_ID, [sharedFilesTenant.id])
        } else {
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC1_ID)
            verifyRoleListDoesNotContainRole(roleList, ROLE_RBAC2_ID)
        }

        where:
        useGroupMembership << [true, false]
    }



    /**
     * Verifies the authenticate response contains the assignment. An empty list of tenantIds means a "global" assignment
     * where the role appears once, with no tenantId value.
     *
     * If a non-empty tenantIds collection is provided it is expected that a distinct role entry will be returned for
     * each tenant provided.
     *
     * @param authenticateResponse
     * @param roleId
     * @param tenantIds
     */
    void verifyAuthenticateResponseContainsAssignment(AuthenticateResponse authenticateResponse, String roleId, List<String> tenantIds) {
        verifyRoleListContainsAssignment(authenticateResponse.user.roles, roleId, tenantIds)
    }

    /**
     * Verifies the authenticate response contains the assignment. An empty list of tenantIds means a "global" assignment
     * where the role appears once, with no tenantId value.
     *
     * If a non-empty tenantIds collection is provided it is expected that a distinct role entry will be returned for
     * each tenant provided.
     *
     * @param authenticateResponse
     * @param roleId
     * @param tenantIds
     */
    void verifyRoleListContainsAssignment(RoleList roleList, String roleId, List<String> tenantIds) {
        if (CollectionUtils.isEmpty(tenantIds)) {
            List<Role> roles = roleList.role.findAll() {it.id == roleId}
            assert roles != null
            assert roles.size() == 1
            assert roles[0].tenantId == null
        } else {
            List<Role> roles = roleList.role.findAll() {it.id == roleId}
            assert roles != null
            assert roles.size() == tenantIds.size()

            // Search for each tenantId individually
            for (String tenantId : tenantIds) {
                def role = roleList.role.find() {it.id == roleId && tenantId.equalsIgnoreCase(it.tenantId)}
                assert role != null
            }
        }
    }

    void verifyRoleListDoesNotContainRole(RoleList roleList, String roleId) {
        assert roleList.role.findAll() {it.id == roleId}.size() == 0
    }

    TenantAssignment createTenantAssignment(String roleId, List<String> tenants) {
        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.forTenants.addAll(tenants)
                ta
        }
        return assignment
    }

}
