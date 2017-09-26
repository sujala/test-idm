package com.rackspace.idm.modules.usergroups.service

import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import static com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN

class GetRoleAssignmentsOnUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        mockApplicationService(service)
        mockTenantService(service)
        mockTenantRoleDao(service)

        dao = Mock()
        service.userGroupDao = dao
    }

    def "getRoleAssignmentsOnGroup: Throws IllegalArgumentException if supplied user group is invalid"() {
        when: "group arg is null"
        service.getRoleAssignmentsOnGroup(null, new UserGroupRoleSearchParams())

        then:
        thrown(IllegalArgumentException)

        when: "group arg has no unique id"
        service.getRoleAssignmentsOnGroup(new UserGroup(), new UserGroupRoleSearchParams())

        then:
        thrown(IllegalArgumentException)
    }

    def "getRoleAssignmentsOnGroup: Throws IllegalArgumentException if search params arg is null"() {
        when:
        service.getRoleAssignmentsOnGroup(new UserGroup().with {it.uniqueId = "uniqueId";it}, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getRoleAssignmentsOnGroup: Pass through to dao search with supplied params"() {
        def groupId = "groupId"
        def domainId = "domainId"

        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        def userGroupRoleSearchParams = new UserGroupRoleSearchParams()
        def daoResult = new PaginatorContext<>()

        when:
        def result = service.getRoleAssignmentsOnGroup(group, userGroupRoleSearchParams)

        then:
        1 * tenantRoleDao.getRoleAssignmentsOnGroup(group, userGroupRoleSearchParams) >> daoResult

        and: "Simply returns result of dao call"
        result == daoResult

        when: "backend dao throws exception"
        service.getRoleAssignmentsOnGroup(group, userGroupRoleSearchParams)

        then: "service bubbles up exception from dao"
        1 * tenantRoleDao.getRoleAssignmentsOnGroup(group, userGroupRoleSearchParams) >> {throw new NullPointerException()}
        thrown(NullPointerException)
    }
}
