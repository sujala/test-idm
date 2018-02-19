package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams;
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;

import java.util.List;

/**
 * Business service to manage user groups
 */
public interface UserGroupService {

    /**
     * Adds the specified user group
     *
     * @param group
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified domain for the group doesn't exist
     * @throws com.rackspace.idm.exception.BadRequestException If the domain specified for the group does not exist
     * or request does not meet validation requirements
     * @return
     */
    UserGroup addGroup(UserGroup group);

    /**
     * Update the existing user group with the provided information. Only non-null fields other than the id and domainId
     * will be updated.
     *
     * @param group
     * @throws com.rackspace.idm.exception.DuplicateException If updating the group name and a different group already
     * exists with this name
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist
     * @throws com.rackspace.idm.exception.BadRequestException If the group changes do not meet validation requirements
     * @return
     */
    UserGroup updateGroup(UserGroup group);

    /**
     * Delete the user group
     *
     * @param group
     */
    void deleteGroup(UserGroup group);

    /**
     * Retrieve the specified group by id regardless of what domain it exists within. Return null if the group does
     * not exist.
     *
     * @param groupId
     * @return
     */
    UserGroup getGroupById(String groupId);

    /**
     * Returns the user group with the specified id. If the group doesn't exist, throws a NotFoundException.
     *
     * @param groupId
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist
     * @throws IllegalArgumentException If supplied groupId is null or empty string
     *
     * @return
     */
    UserGroup checkAndGetGroupById(String groupId);

    /**
     * Retrieves the group with the specified groupId under the specified domain. If a group with the specified groupId
     * exists, but lives in a different domain than the one specified, will return null.
     *
     * @param domainId
     * @param groupId
     * @throws IllegalArgumentException If supplied domainId or groupId is null or empty string
     *
     * @return
     */
    UserGroup getGroupByIdForDomain(String groupId, String domainId);

    /**
     * Retrieves the group with the specified groupId under the specified domain. If no such group exists, throws
     * NotFoundException.
     *
     * @param domainId
     * @param groupId
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist in the given domain
     * @throws IllegalArgumentException If supplied domainId or groupId is null or empty string
     * @return
     */
    UserGroup checkAndGetGroupByIdForDomain(String groupId, String domainId);

    /**
     * Retrieves the group with the specified group name under the specified domain. If no such group exists, returns null
     *
     * @param domainId
     * @param groupName
     * @throws IllegalArgumentException If supplied domainId or groupName is null or empty string
     *
     * @return
     */
    UserGroup getGroupByNameForDomain(String groupName, String domainId);

    /**
     * Retrieves the groups for the specified search params under the specified domain. If no such groups exists,
     * returns empty list.
     *
     * @param userGroupSearchParams
     * @param domainId
     * @throws IllegalArgumentException If supplied domainId is null or empty string
     *
     * @return
     */
    List<UserGroup> getGroupsBySearchParamsInDomain(UserGroupSearchParams userGroupSearchParams, String domainId);

    /**
     * Grant role on individual tenant to a user group.
     *
     * @param userGroup
     * @param roleId
     * @param tenantId
     *
     * @throws IllegalArgumentException if userGroup, roleId, or tenantId is null
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.DuplicateException If role is set to global on all tenants or provided tenantId already in the list of tenantIds.
     */

    void addRoleAssignmentOnGroup(UserGroup userGroup, String roleId, String tenantId);

    /**
     * Revoke role on individual tenant to a user group.
     *
     * @param userGroup
     * @param roleId
     * @param tenantId
     *
     * @throws IllegalArgumentException if userGroup, roleId, or tenantId is null
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.DuplicateException If role is set to global on all tenants or provided tenantId already in the list of tenantIds.
     */

    void revokeRoleAssignmentOnGroup(UserGroup userGroup, String roleId, String tenantId);

    /**
     * Retries the specified role on the group, or null if the role does not exist on the group. Does not distinguish
     * between
     *
     * @param userGroup
     * @param roleId
     * @return
     */
    TenantRole getRoleAssignmentOnGroup(UserGroup userGroup, String roleId);

    /**
     * Retrieves the full, unpaginated set of roles assigned to the group as TenantRoles (standard representation of assigned
     * roles in Identity). The retrieved roles are NOT populated with the role name and other attributes from the
     * associated client role.
     *
     * @param userGroupId
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException If a group with the specified id does not exist
     */
    List<TenantRole> getRoleAssignmentsOnGroup(String userGroupId);

    /**
     * Retrieve the set of roles assignments on the user group that match the specified criteria. If no roles match,
     * a context will an empty list of results will be returned.
     *
     * @param userGroup
     * @param userGroupRoleSearchParams
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnGroup(UserGroup userGroup, UserGroupRoleSearchParams userGroupRoleSearchParams);

    /**
     * Assign the specified roles to the group. Validation is performed on all roles prior to persisting any assignment
     * to reduce the likelihood of failure. If any assignment is deemed invalid during the initial validation, none will
     * be saved. If an error is encountered during saving, processing assignments will stop.
     *
     * @param userGroup
     * @param roleAssignments
     *
     * @throws IllegalArgumentException if userGroup, userGroup.getUniqueId(), or tenantAssignments is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the group as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceRoleAssignmentsOnGroup(UserGroup userGroup, RoleAssignments roleAssignments);

    /**
     * Removes the specified role from the group regardless of whether it is assigned as a domain or tenant role.
     *
     * @param userGroup
     * @param roleId
     * @throws IllegalArgumentException if userGroup, userGroup.getUniqueId(), or roleId is null
     * @throws com.rackspace.idm.exception.NotFoundException If role is not assigned to group
     */
    void revokeRoleAssignmentOnGroup(UserGroup userGroup, String roleId);

    /**
     * Retrieves the groups under the specified domain. If no groups exists, returns empty list.
     *
     * @param domainId
     * @throws IllegalArgumentException If supplied domainId is null or empty string
     *
     * @return
     */
    Iterable<UserGroup> getGroupsForDomain(String domainId);

     /**
     * Adds the specified user to group
     *
     * @param userId
     * @param group
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified user does not exist
     * @throws com.rackspace.idm.exception.ForbiddenException If user is not a provisioned user
     * @throws com.rackspace.idm.exception.BadRequestException If user does not belong to domain
     * or group does not belong to domain
     * or user domain does not belong to the user group
     */
    void addUserToGroup(String userId, UserGroup group);

     /**
     * Removes the specified user from group
     *
     * @param userId
     * @param group
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified user does not exist
     * @throws com.rackspace.idm.exception.ForbiddenException If user is not a provisioned user
     * @throws com.rackspace.idm.exception.BadRequestException If user does not belong to domain
     * or group does not belong to domain
     * or user domain does not belong to the user group
     */
    void removeUserFromGroup(String userId, UserGroup group);

    /**
     * Retrieves the users associated with user group. If no users are found, a context with an
     * empty list of results will be returned.
     *
     * @param group
     * @throws IllegalAccessException If user group or user search criteria is null
     * @return
     */
    PaginatorContext<EndUser> getUsersInGroupPaged(UserGroup group, UserSearchCriteria userSearchCriteria);

    /**
     * Retrieves the users associated with user group.
     *
     * @param group
     * @throws IllegalAccessException If user group is null
     * @return
     */
    Iterable<EndUser> getUsersInGroup(UserGroup group);

    /**
     * Retrieves the count of groups with the role, specified by roleId, assigned to the group.
     *
     * @param roleId
     * @throws IllegalArgumentException If supplied roleId is null or empty string
     * @return
     */
    int countGroupsWithRoleAssignment(String roleId);

}
