package com.rackspace.idm.modules.usergroups.service

import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import static com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN

class RevokeRoleAssignmentsFromUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        mockApplicationService(service)
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockIdentityUserService(service)
        mockAtomHopperClient(service)

        dao = Mock()
        service.userGroupDao = dao
    }

    def "revokeRoleAssignmentFromGroup: Throws IllegalArgumentException if supplied user group is invalid"() {
        when: "group arg is null"
        service.revokeRoleAssignmentOnGroup(null, "roleId")

        then:
        thrown(IllegalArgumentException)

        when: "group arg has no unique id"
        service.revokeRoleAssignmentOnGroup(new UserGroup(), "roleId")

        then:
        thrown(IllegalArgumentException)
    }

    def "revokeRoleAssignmentFromGroup: Throws IllegalArgumentException if roleId arg is null"() {
        when:
        service.revokeRoleAssignmentOnGroup(new UserGroup().with {it.uniqueId = "uniqueId";it}, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "revokeRoleAssignmentFromGroup: Throws NotFoundException if role is not assigned on group"() {
        def groupId = "groupId"
        def domainId = "domainId"
        def roleId = "roleId"

        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        when:
        service.revokeRoleAssignmentOnGroup(group, roleId)

        then:
        // Tries to retrieve role
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >> null

        // Don't call delete role if role not found
        0 * tenantRoleDao.deleteTenantRole(_)

        and:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotFoundException, "GEN-004", String.format(ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN, roleId))
    }

    def "revokeRoleAssignmentFromGroup: When role assigned, deletes role from backend"() {
        def groupId = "groupId"
        def domainId = "domainId"
        def roleId = "roleId"

        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        def tenantRoleAssignment = new TenantRole()

        def user = entityFactory.createUser()

        when:
        service.revokeRoleAssignmentOnGroup(group, roleId)

        then:
        // Tries to retrieve role
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >> tenantRoleAssignment

        // Calls delete role dao service
        1 * tenantRoleDao.deleteTenantRole(tenantRoleAssignment)

        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost((EndUser) user, FeedsUserStatusEnum.ROLE)
    }
}
