package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.domain.entity.TenantRole;
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
     * Retrieves the current set of roles assigned to the group as TenantRoles (standard representation of assigned
     * roles in Identity)
     *
     * @param userGroupId
     * @return
     */
    List<TenantRole> getRoleAssignmentsOnGroup(String userGroupId);

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
     * @throws com.rackspace.idm.modules.usergroups.exception.FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceRoleAssignmentsOnGroup(UserGroup userGroup, RoleAssignments roleAssignments);

    /**
     * Retrieves the groups under the specified domain. If no groups exists, returns empty list.
     *
     * @param domainId
     * @throws IllegalArgumentException If supplied domainId is null or empty string
     *
     * @return
     */
    Iterable<UserGroup> getGroupsForDomain(String domainId);
}
