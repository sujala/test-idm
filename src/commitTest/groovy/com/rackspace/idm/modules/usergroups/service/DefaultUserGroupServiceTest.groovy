package com.rackspace.idm.modules.usergroups.service

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.RoleLevelEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class DefaultUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        mockTenantAssignmentService(service)
        mockDelegationService(service)
        mockIdentityUserService(service)
        mockAtomHopperClient(service)

        dao = Mock()
        service.userGroupDao = dao
    }

    def "addGroup: validates group name and length through validator standard calls"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", group.name, 64)
        1 * validator20.validateStringMaxLength("description", group.description, 255) >> { throw new BadRequestException() }
        0 * dao.addGroup(_) // Not persisted
        thrown(BadRequestException)
    }

    @Unroll
    def "addGroup: Forbidden when existing number of groups equals or exceeds max group allowed per domain threshold: maxInDomain: #maxInDomain; numExistingGroups:#numExistingGroups"() {
        def domainId = "donmainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * reloadableConfig.getMaxUsersGroupsPerDomain() >> maxInDomain
        1 * dao.countGroupsInDomain(domainId) >> numExistingGroups
        0 * dao.addGroup(_) // Not persisted
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG )

        where:
        maxInDomain | numExistingGroups
        4 | 5
        5 | 5
        0 | 0
        2 | 2
    }

    @Unroll
    def "addGroup: Allowed when existing groups less than max group allowed per domain threshold: maxInDomain: #maxInDomain; numExistingGroups:#numExistingGroups"() {
        def domainId = "donmainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * reloadableConfig.getMaxUsersGroupsPerDomain() >> maxInDomain
        1 * dao.countGroupsInDomain(domainId) >> numExistingGroups
        1 * dao.addGroup(_) // Not persisted
        notThrown(Exception)

        where:
        maxInDomain | numExistingGroups
        2 | 0
        2 | 1
        6 | 5
    }

    def "addGroup: Throws DuplicateException if group with name already exists in domainId without persisting"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * dao.getGroupByNameForDomain(group.name, group.domainId) >> new UserGroup()
        0 * dao.addGroup(_) // Not persisted
        thrown(DuplicateException)
    }

    def "addGroup: Persists object via dao when validation succeeds"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }
        reloadableConfig.getMaxUsersGroupsPerDomain() >> 5 // Set > 0

        when:
        service.addGroup(group)

        then:
        1 * dao.addGroup(group)
    }

    def "deleteGroup: removes object via dao and remove group membership from users"() {
        mockIdentityUserService(service)
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        EndUser user1 = new User().with {
            it.domainId = domainId
            it.id = "id"
            it
        }
        EndUser user2 = new User().with {
            it.domainId = domainId
            it.id = "id2"
            it
        }

        when: "Users belong to user group"
        service.deleteGroup(group)

        then:
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user1, user2].asList()
        1 * identityUserService.removeUserGroupFromUser(group, user1)
        1 * identityUserService.removeUserGroupFromUser(group, user2)
        1 * dao.deleteGroup(group)

        when: "No users belong to user group"
        service.deleteGroup(group)

        then:
        1 * identityUserService.getEndUsersInUserGroup(group) >> [].asList()
        0 * identityUserService.removeUserGroupFromUser(_, _)
        1 * dao.deleteGroup(group)
    }

    def "listGroupsForDomain: gets objects via dao"() {
        given:
        def domainId = "domainId"

        when:
        service.getGroupsForDomain(domainId)

        then:
        1 * dao.getGroupsForDomain(domainId)
    }

    def "getUsersInGroupPaged: gets objects via identityUserService"() {
        given:
        def group = new UserGroup()
        def userSearchCriteria = new UserSearchCriteria(new PaginationParams())
        mockIdentityUserService(service)

        when:
        service.getUsersInGroupPaged(group, userSearchCriteria)

        then:
        1 * identityUserService.getEndUsersInUserGroupPaged(group, userSearchCriteria)
    }

    def "getUsersInGroup: gets objects via identityUserService"() {
        given:
        def group = new UserGroup()
        mockIdentityUserService(service)

        when:
        service.getUsersInGroup(group)

        then:
        1 * identityUserService.getEndUsersInUserGroup(group)
    }

    def "getUsersInGroup: null params"() {
        when:
        service.getUsersInGroup(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "addUserToGroup: creates assignment via identityUserService"() {
        given:
        def domainId = "domainId"
        def userId = "userid"
        def group = new UserGroup().with {
            it.domainId = domainId
            it
        }
        def user = new com.rackspace.idm.domain.entity.User().with {
            it.domainId = domainId
            it
        }
        mockIdentityUserService(service)

        when:
        service.addUserToGroup(userId, group)

        then:
        1 * identityUserService.checkAndGetUserById(userId) >> user
        1 * identityUserService.addUserGroupToUser(group, user)
    }

    def "removeUserFromGroup: removes assignment via identityUserService"() {
        given:
        def userId = "userid"
        def group = new UserGroup()
        def user = new com.rackspace.idm.domain.entity.User().with {
            it.domainId = domainId
            it
        }
        mockIdentityUserService(service)

        when:
        service.removeUserFromGroup(userId, group)

        then:
        1 * identityUserService.checkAndGetUserById(userId) >> user
        1 * identityUserService.removeUserGroupFromUser(group, user)
    }

    def "updateGroup: validates group name and length through validator standard calls"() {
        UserGroup origGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }
        UserGroup groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.description = RandomStringUtils.randomAlphanumeric(8)
            it
        }

        when:
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        1 * validator20.validateStringNotNullWithMaxLength("name", groupUpdates.name, 64)
        1 * validator20.validateStringMaxLength("description", groupUpdates.description, 255) >> { throw new BadRequestException() }
        0 * dao.updateGroup(_) // Not persisted
        thrown(BadRequestException)
    }

    def "updateGroup: group name only validated if provided in request"() {
        UserGroup origGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = "domainId"
            it.name = "name"
            it
        }
        def updatedGroupName = RandomStringUtils.randomAlphanumeric(8)

        when:
        UserGroup groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.name = updatedGroupName
            it
        }
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        1 * validator20.validateStringNotNullWithMaxLength("name", updatedGroupName, 64)
        1 * dao.updateGroup(_)

        when:
        groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.name = null
            it
        }
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        0 * validator20.validateStringNotNullWithMaxLength("name", _, 64)
        1 * dao.updateGroup(_)
    }

    def "updateGroup: group description only validated if provided in request"() {
        UserGroup origGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }
        def updatedGroupDesc = RandomStringUtils.randomAlphanumeric(8)

        when:
        UserGroup groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.description = updatedGroupDesc
            it
        }
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        1 * validator20.validateStringMaxLength("description", updatedGroupDesc, 255)
        1 * dao.updateGroup(_)

        when:
        groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.name = null
            it
        }
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        0 * validator20.validateStringMaxLength("description", updatedGroupDesc, 255)
        1 * dao.updateGroup(_)
    }

    def "updateGroup: duplicate group names throws DuplicateException"() {
        given:
        UserGroup origGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }
        UserGroup groupUpdates = new UserGroup().with {
            it.domainId = origGroup.domainId
            it.id = origGroup.id
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.description = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        UserGroup otherGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.name = groupUpdates.name
            it
        }

        when:
        service.updateGroup(groupUpdates)

        then:
        1 * dao.getGroupById(origGroup.id) >> origGroup
        1 * dao.getGroupByNameForDomain(groupUpdates.name, groupUpdates.domainId) >> otherGroup
        0 * dao.updateGroup(_)
        thrown(DuplicateException)
    }

    def "getGroupByNameForUserInDomain: invalid params"() {
        given:
        def groupName = "groupName"
        def userId = "userId"
        def domainId = "domainId"

        when: "groupName is null"
        service.getGroupByNameForUserInDomain(null, userId, domainId)

        then:
        thrown(IllegalArgumentException)

        when: "userId is null"
        service.getGroupByNameForUserInDomain(groupName, null, domainId)

        then:
        thrown(IllegalArgumentException)

        when: "domainId is null"
        service.getGroupByNameForUserInDomain(groupName, userId, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getGroupByNameForUserInDomain: calls correct dao and service"() {
        given:
        def groupName = "groupName"
        def userId = "userId"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        EndUser user = new User().with {
            it.domainId = domainId
            it.id = userId
            it.username = "username"
            it.userGroupDNs.add(new DN(group.uniqueId))
            it
        }
        mockIdentityUserService(service)

        when:
        UserGroup userGroup = service.getGroupByNameForUserInDomain(groupName, userId, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        1 * identityUserService.getEndUserById(userId) >> user
        userGroup.equals(group)
    }

    def "getGroupsBySearchParamsInDomain: calls correct dao and services"() {
        given:
        mockIdentityUserService(service)
        def domainId = "domainId"
        def groupName = "name"
        def userId = "userId"
        def groupId = "groupId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = groupId
            it.uniqueId = "groupDN=$groupId"
            it
        }
        EndUser user = new User().with {
            it.domainId = domainId
            it.id = userId
            it.userGroupDNs.add(new DN(group.uniqueId))
            it
        }

        when: "Searching by name"
        UserGroupSearchParams userGroupSearchParams = new UserGroupSearchParams(groupName, null)
        List<UserGroup> userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        userGroups.size() == 1
        userGroups.get(0) == group

        when: "Searching by userId"
        userGroupSearchParams = new UserGroupSearchParams(null, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * identityUserService.getEndUserById(userId) >> user
        1 * dao.getGroupById(groupId) >> group
        userGroups.size() == 1
        userGroups.get(0) == group

        when: "Searching by name and userId"
        userGroupSearchParams = new UserGroupSearchParams(groupName, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        1 * identityUserService.getEndUserById(userId) >> user
        userGroups.size() == 1
        userGroups.get(0) == group
    }

    def "getGroupsBySearchParamsInDomain: empty list cases"() {
        given:
        mockIdentityUserService(service)
        def domainId = "domainId"
        def groupName = "name"
        def userId = "userId"
        def groupId = "groupId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = groupId
            it.uniqueId = "groupDN=$groupId"
            it
        }
        EndUser user = new User().with {
            it.domainId = domainId
            it.id = userId
            it.userGroupDNs.add(new DN(group.uniqueId))
            it
        }

        when: "Searching by name"
        UserGroupSearchParams userGroupSearchParams = new UserGroupSearchParams(groupName, null)
        List<UserGroup> userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> null
        userGroups.size() == 0

        when: "Searching by userId - user not found"
        userGroupSearchParams = new UserGroupSearchParams(null, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * identityUserService.getEndUserById(userId) >> null
        0 * dao.getGroupById(groupId)
        userGroups.size() == 0

        when: "Searching by userId - group not found"
        userGroupSearchParams = new UserGroupSearchParams(null, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * identityUserService.getEndUserById(userId) >> user
        1 * dao.getGroupById(groupId) >> null
        userGroups.size() == 0

        when: "Searching by name and userId - user not found"
        userGroupSearchParams = new UserGroupSearchParams(groupName, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        1 * identityUserService.getEndUserById(userId) >> null
        userGroups.size() == 0

        when: "Searching by name and userId - group not found"
        userGroupSearchParams = new UserGroupSearchParams(groupName, userId)
        userGroups = service.getGroupsBySearchParamsInDomain(userGroupSearchParams, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> null
        0 * identityUserService.getEndUserById(userId)
        userGroups.size() == 0
    }

    def "getGroupByNameForUserInDomain: null cases"() {
        given:
        def groupName = "groupName"
        def userId = "userId"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        EndUser user = new User().with {
            it.domainId = domainId
            it.id = userId
            it.username = "username"
            it.userGroupDNs.add(new DN(group.uniqueId))
            it
        }
        mockIdentityUserService(service)

        when: "User group not found"
        UserGroup userGroup = service.getGroupByNameForUserInDomain(groupName, userId, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> null
        userGroup == null

        when: "User not found"
        userGroup = service.getGroupByNameForUserInDomain(groupName, userId, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        1 * identityUserService.getEndUserById(userId) >> null
        userGroup == null

        when: "When user group not part of user's userGroupDNs"
        user.userGroupDNs.clear()
        user.userGroupDNs.add(new DN("otherDN=dn"))
        userGroup = service.getGroupByNameForUserInDomain(groupName, userId, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> group
        1 * identityUserService.getEndUserById(userId) >> user
        userGroup == null
    }

    def "addRoleAssignmentOnGroup: calls correct dao and services"() {
        given:
        // Mocks
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)

        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        def roleId = "roleId"
        ClientRole clientRole = new ClientRole().with {
            it.id = roleId
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it
        }
        def tenantId = "tenantId"
        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds.add("otherTenantId")
            it.roleRsId = roleId
            it
        }
        def user = new User()

        when: "Adding role assignment with existing tenant role"
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        1 * tenantRoleDao.updateRoleAssignmentOnGroup(group, tenantRole)
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)

        when: "Adding new role assignment"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add(tenantId)
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  null
        1 * tenantRoleDao.addRoleAssignmentOnGroup(group, tenantRole)
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "addRoleAssignmentOnGroup: error check"() {
        given:
        // Mocks
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)

        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        def roleId = "roleId"
        ClientRole clientRole = new ClientRole().with {
            it.id = roleId
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it
        }
        def tenantId = "tenantId"
        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds.add("otherTenantId")
            it.roleRsId = roleId
            it
        }

        when: "null group"
        service.addRoleAssignmentOnGroup(null, roleId, tenantId)

        then:
        thrown(IllegalArgumentException)

        when: "null roleId"
        service.addRoleAssignmentOnGroup(group, null, tenantId)

        then:
        thrown(IllegalArgumentException)

        when: "null tenantId"
        service.addRoleAssignmentOnGroup(group, roleId, null)

        then:
        thrown(IllegalArgumentException)

        when: "role not found"
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> null
        thrown(NotFoundException)

        when: "role administrator is not a user-manager/user-admin"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_750.levelAsInt
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        thrown(ForbiddenException)

        when: "role administrator is not a user-manager/user-admin"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_500.levelAsInt
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        thrown(ForbiddenException)

        when: "Tenant does not exist"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId) >> {throw new NotFoundException()}
        thrown(NotFoundException)

        when: "Role assignment exist as global"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add("*")
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        thrown(DuplicateException)

        when: "Role assignment already has provided tenantId"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add(tenantId)
        service.addRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        thrown(DuplicateException)
    }

    def "revokeRoleAssignmentOnGroup: calls correct dao and services"() {
        given:
        // Mocks
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)

        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        def roleId = "roleId"
        ClientRole clientRole = new ClientRole().with {
            it.id = roleId
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it
        }
        def tenantId = "tenantId"
        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds.add(tenantId)
            it.roleRsId = roleId
            it
        }
        def user = new User()

        when: "Removing a role assignment"
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        1 * tenantRoleDao.deleteOrUpdateRoleAssignmentOnGroup(group, tenantRole)
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
        tenantRole.tenantIds.size() == 1
        tenantRole.tenantIds.contains(tenantId)

        when: "Removing a role assignment with other tenants"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add(tenantId)
        tenantRole.tenantIds.add("otherTenantId")
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        1 * tenantRoleDao.deleteOrUpdateRoleAssignmentOnGroup(group, tenantRole)
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
        tenantRole.tenantIds.size() == 1
        tenantRole.tenantIds.contains(tenantId)
    }

    def "revokeRoleAssignmentOnGroup: error check"() {
        given:
        // Mocks
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)

        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }
        def roleId = "roleId"
        ClientRole clientRole = new ClientRole().with {
            it.id = roleId
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it
        }
        def tenantId = "tenantId"
        def otherTenantId = "otherTenantId"
        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds.add("otherTenantId")
            it.roleRsId = roleId
            it
        }

        when: "null group"
        service.revokeRoleAssignmentOnGroup(null, roleId, tenantId)

        then:
        thrown(IllegalArgumentException)

        when: "null roleId"
        service.revokeRoleAssignmentOnGroup(group, null, tenantId)

        then:
        thrown(IllegalArgumentException)

        when: "null tenantId"
        service.revokeRoleAssignmentOnGroup(group, roleId, null)

        then:
        thrown(IllegalArgumentException)

        when: "role not found"
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> null
        thrown(NotFoundException)

        when: "role administrator is not a user-manager/user-admin"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_750.levelAsInt
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        thrown(ForbiddenException)

        when: "role administrator is not a user-manager/user-admin"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_500.levelAsInt
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        thrown(ForbiddenException)

        when: "Tenant does not exist"
        clientRole.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId) >> {throw new NotFoundException()}
        thrown(NotFoundException)

        when: "Role assignment exist as global"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add("*")
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        thrown(BadRequestException)

        when: "Role assignment does not has provided tenantId"
        tenantRole.tenantIds.clear()
        tenantRole.tenantIds.add(otherTenantId)
        service.revokeRoleAssignmentOnGroup(group, roleId, tenantId)

        then:
        1 * applicationService.getClientRoleById(roleId) >> clientRole
        1 * tenantService.checkAndGetTenant(tenantId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleId) >>  tenantRole
        thrown(NotFoundException)
    }

    def "replaceRoleAssignmentsOnGroup: calls correct service"() {
        given:
        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(new TenantAssignment().with {
                it.onRole = "roleId"
                it.forTenants.addAll("tenantId")
                it
            })
            it.tenantAssignments = ta
            it
        }
        def user = new User()

        when:
        service.replaceRoleAssignmentsOnGroup(group, assignments)

        then:
        1 * tenantAssignmentService.replaceTenantAssignmentsOnUserGroup(group, assignments.tenantAssignments.tenantAssignment)
        1 * identityUserService.getEndUsersInUserGroup(group) >> [user]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "replaceRoleAssignmentsOnGroup: error check and invalid cases"() {
        given:
        def groupName = "groupName"
        def domainId = "domainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = groupName
            it.id = "id"
            it.uniqueId = "groupDN=groupId"
            it
        }

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(new TenantAssignment().with {
                it.onRole = "roleId"
                it.forTenants.addAll("tenantId")
                it
            })
            it.tenantAssignments = ta
            it
        }

        when: "group is null"
        service.replaceRoleAssignmentsOnGroup(null, assignments)

        then:
        thrown(IllegalArgumentException)

        when: "group's uniqueId is null"
        UserGroup invalidGroup = new UserGroup()
        service.replaceRoleAssignmentsOnGroup(invalidGroup, assignments)

        then:
        thrown(IllegalArgumentException)

        when: "assignments are null"
        service.replaceRoleAssignmentsOnGroup(group, null)

        then:
        thrown(IllegalArgumentException)

        when: "tenant assignments are null"
        RoleAssignments invalidAssignments = new RoleAssignments()
        List<TenantRole> tenantRoles = service.replaceRoleAssignmentsOnGroup(group, invalidAssignments)

        then:
        tenantRoles.isEmpty()
    }

    def "deleteGroup removes the user group from explicit DA assignments"() {
        given:
        def userGroup = new UserGroup()

        when:
        service.deleteGroup(userGroup)

        then:
        1 * identityUserService.getEndUsersInUserGroup(userGroup) >> []
        1 * delegationService.removeConsumerFromExplicitDelegationAgreementAssignments(userGroup)
    }

}
