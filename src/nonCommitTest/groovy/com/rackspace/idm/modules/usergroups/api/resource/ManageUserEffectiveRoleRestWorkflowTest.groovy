package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

/**
 * The purpose of this test is to test various states of removing roles that a user receives both through direct assignment
 * and user group membership.
 */
class ManageUserEffectiveRoleRestWorkflowTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    UserGroupService userGroupService

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared def sharedUserAdminToken

    @Shared String sharedCloudTenantId
    @Shared String sharedFilesTenantId

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        AuthenticateResponse auth = cloud20.authenticatePassword(sharedUserAdmin.username).getEntity(AuthenticateResponse).value
        sharedUserAdminToken = auth.token.id
        sharedCloudTenantId = auth.user.roles.role.find {it.id == MOSSO_ROLE_ID}.tenantId
        sharedFilesTenantId = auth.user.roles.role.find {it.id == NAST_ROLE_ID}.tenantId
    }

    def "Verify multiple role assignment on user/usergroup - can delete global assignment when role also assigned on tenant other way"() {
        setup:

        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def subUser = utils.createUser(sharedUserAdminToken, RandomStringUtils.randomAlphanumeric(20), sharedUserAdmin.domainId)
        def subUserToken = utils.getToken(subUser.username)
        def group = utils.createUserGroup(subUser.domainId)
        utils.addUserToUserGroup(subUser.id, group)

        // assign role1 to user globally and role2 to user on tenant
        utils.addRoleToUser(subUser, ROLE_RBAC1_ID)
        utils.addRoleToUserOnTenantId(subUser, sharedCloudTenantId, ROLE_RBAC2_ID)

        // assign role 1 to user group on tenant, role2 globally
        RoleAssignments ra = v2Factory.createRoleAssignments([v2Factory.createTenantAssignment(ROLE_RBAC1_ID, [sharedCloudTenantId])
                                                            , v2Factory.createTenantAssignment(ROLE_RBAC2_ID, ["*"])])
        utils.grantRoleAssignmentsOnUserGroup(group, ra)

        // Roles are both globally assigned roles
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == null} != null
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == null} != null

        when: "Delete global role 1 assigned on user that is also assigned on tenant to user group"
        utils.deleteRoleOnUser(subUser, ROLE_RBAC1_ID)

        then: "User still has role 1 assigned on tenant"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == sharedCloudTenantId} != null

        and: "trying to delete role 1 off user again results in error"
        cloud20.deleteApplicationRoleOnUser(userAdminToken, ROLE_RBAC1_ID, subUser.id).status == HttpStatus.SC_NOT_FOUND

        when: "Delete tenant role 1 from user group"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC1_ID)

        then: "User no longer has role 1 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID} == null

        when: "Delete global role 2 assigned on user group that is also assigned to user on tenant"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC2_ID)

        then: "User still has role 2 assigned on tenant"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == sharedCloudTenantId} != null

        and: "trying to delete role 2 again results in error"
        cloud20.revokeRoleAssignmentFromUserGroup(userAdminToken, group, ROLE_RBAC2_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Remove tenant role 2 from user"
        utils.deleteRoleFromUserOnTenant(subUser, new Tenant().with {id = sharedCloudTenantId;it}, ROLE_RBAC2_ID)

        then: "User no longer has role 2 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID} == null
    }

    def "Verify multiple role assignment on user/usergroup - can delete tenant assignment when role also assigned globally other way"() {
        setup:

        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def subUser = utils.createUser(sharedUserAdminToken, RandomStringUtils.randomAlphanumeric(20), sharedUserAdmin.domainId)
        def subUserToken = utils.getToken(subUser.username)
        def group = utils.createUserGroup(subUser.domainId)
        utils.addUserToUserGroup(subUser.id, group)

        // assign role1 to user globally and role2 to user on tenant
        utils.addRoleToUser(subUser, ROLE_RBAC1_ID)
        utils.addRoleToUserOnTenantId(subUser, sharedCloudTenantId, ROLE_RBAC2_ID)

        // assign role 1 to user group on tenant, role2 globally
        RoleAssignments ra = v2Factory.createRoleAssignments([v2Factory.createTenantAssignment(ROLE_RBAC1_ID, [sharedCloudTenantId])
                                                              , v2Factory.createTenantAssignment(ROLE_RBAC2_ID, ["*"])])
        utils.grantRoleAssignmentsOnUserGroup(group, ra)

        // Roles are both globally assigned roles
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == null} != null
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == null} != null

        when: "Delete tenant role 1 assigned on user group that is also assigned globally to user"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC1_ID)

        then: "User still has role 1 assigned globally"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == null} != null

        and: "trying to delete role 1 off user group again results in error"
        cloud20.revokeRoleAssignmentFromUserGroup(userAdminToken, group, ROLE_RBAC1_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Delete global role 1 from user"
        utils.deleteRoleOnUser(subUser, ROLE_RBAC1_ID)

        then: "User no longer has role 1 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID} == null

        when: "Delete tenant role 2 assigned on user that is also assigned to user group globally"
        utils.deleteRoleFromUserOnTenant(subUser, new Tenant().with {id = sharedCloudTenantId;it}, ROLE_RBAC2_ID)

        then: "User still has role 2 assigned globally"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == null} != null

        and: "trying to delete role 2 again from user results in error"
        cloud20.deleteRoleFromUserOnTenant(sharedUserAdminToken, sharedCloudTenantId, subUser.id, ROLE_RBAC2_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Remove tenant role 2 from user group"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC2_ID)

        then: "User no longer has role 2 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID} == null
    }

    def "Verify multiple role assignment on user/usergroup - can delete global assignment when role assigned globally both ways"() {
        setup:

        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def subUser = utils.createUser(sharedUserAdminToken, org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(20), sharedUserAdmin.domainId)
        def subUserToken = utils.getToken(subUser.username)
        def group = utils.createUserGroup(subUser.domainId)
        utils.addUserToUserGroup(subUser.id, group)

        // assign role1 and role2 to user globally
        utils.addRoleToUser(subUser, ROLE_RBAC1_ID)
        utils.addRoleToUser(subUser, ROLE_RBAC2_ID)

        // assign role 1/2 to user group on globally
        RoleAssignments ra = v2Factory.createRoleAssignments([v2Factory.createTenantAssignment(ROLE_RBAC1_ID, ["*"])
                                                              ,v2Factory.createTenantAssignment(ROLE_RBAC2_ID, ["*"]) ])
        utils.grantRoleAssignmentsOnUserGroup(group, ra)

        // Roles are both globally assigned roles
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == null} != null
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == null} != null

        when: "Delete global role 1 assigned on user that is also assigned globally to user group"
        utils.deleteRoleOnUser(subUser, ROLE_RBAC1_ID)

        then: "User still has role 1 globally assigned"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == null} != null

        and: "trying to delete role 1 off user again results in error"
        cloud20.deleteApplicationRoleOnUser(userAdminToken, ROLE_RBAC1_ID, subUser.id).status == HttpStatus.SC_NOT_FOUND

        when: "Delete global role 1 from user group"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC1_ID)

        then: "User no longer has role 1 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID} == null

        when: "Delete global role 2 assigned on user group that is also assigned to user globally"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC2_ID)

        then: "User still has role 2 globally assigned"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == null} != null

        and: "trying to delete role 2 again from user group results in error"
        cloud20.revokeRoleAssignmentFromUserGroup(userAdminToken, group, ROLE_RBAC2_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Remove global role 2 from user"
        utils.deleteRoleOnUser(subUser, ROLE_RBAC2_ID)

        then: "User no longer has role 2 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID} == null
    }

    def "Verify multiple role assignment on user/usergroup - can delete tenant assignment when role assigned on tenant both ways"() {
        setup:

        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def subUser = utils.createUser(sharedUserAdminToken, RandomStringUtils.randomAlphanumeric(20), sharedUserAdmin.domainId)
        def subUserToken = utils.getToken(subUser.username)
        def group = utils.createUserGroup(subUser.domainId)
        utils.addUserToUserGroup(subUser.id, group)

        // assign role1/role2 to user on tenant
        utils.addRoleToUserOnTenantId(subUser, sharedCloudTenantId, ROLE_RBAC1_ID)
        utils.addRoleToUserOnTenantId(subUser, sharedCloudTenantId, ROLE_RBAC2_ID)

        // assign role1/role2 on tenant
        RoleAssignments ra = v2Factory.createRoleAssignments([v2Factory.createTenantAssignment(ROLE_RBAC1_ID, [sharedCloudTenantId])
                                                              , v2Factory.createTenantAssignment(ROLE_RBAC2_ID, [sharedCloudTenantId])])
        utils.grantRoleAssignmentsOnUserGroup(group, ra)

        // Roles are both tenant assigned roles
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == sharedCloudTenantId} != null
        assert utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == sharedCloudTenantId} != null

        when: "Delete tenant role 1 assigned on user group that is also assigned on tenant to user"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC1_ID)

        then: "User still has role 1 assigned on tenant"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == sharedCloudTenantId} != null

        and: "trying to delete role 1 off user group again results in error"
        cloud20.revokeRoleAssignmentFromUserGroup(userAdminToken, group, ROLE_RBAC1_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Delete tenant role 1 from user"
        utils.deleteRoleFromUserOnTenant(subUser, new Tenant().with {id = sharedCloudTenantId;it}, ROLE_RBAC1_ID)

        then: "User no longer has role 1 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC1_ID} == null

        when: "Delete tenant role 2 assigned on user that is also assigned to user group on tenant"
        utils.deleteRoleFromUserOnTenant(subUser, new Tenant().with {id = sharedCloudTenantId;it}, ROLE_RBAC2_ID)

        then: "User still has role 2 assigned on tenant"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == sharedCloudTenantId} != null

        and: "trying to delete role 2 again from user results in error"
        cloud20.deleteRoleFromUserOnTenant(sharedUserAdminToken, sharedCloudTenantId, subUser.id, ROLE_RBAC2_ID).status == HttpStatus.SC_NOT_FOUND

        when: "Remove tenant role 2 from user group"
        utils.revokeRoleFromUserGroup(group, ROLE_RBAC2_ID)

        then: "User no longer has role 2 assigned at all"
        utils.validateToken(subUserToken).user.roles.role.find {it.id == ROLE_RBAC2_ID} == null
    }

}
