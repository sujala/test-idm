package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;

import java.util.Collection;
import java.util.List;

public interface TenantRoleDao {
    void addTenantRoleToUser(BaseUser user, TenantRole tenantRole);
    Iterable<TenantRole> getTenantRolesForUser(BaseUser user, String applicationId);
    Iterable<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId);
    Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
    TenantRole getTenantRoleForUser(BaseUser user, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult);
    int getCountOfTenantRolesByRoleIdForProvisionedUsers(String roleId);
    int getCountOfTenantRolesByRoleIdForFederatedUsers(String roleId);
    Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles);
    String getUserIdForParent(TenantRole tenantRole);
    Iterable<TenantRole> getTenantRolesForUserWithId(User user, Collection<String> roleIds);


    /**
     * Assign the new tenant role to the group
     *
     * @param group
     * @param tenantRole
     */
    void addRoleAssignmentOnGroup(UserGroup group, TenantRole tenantRole);

    /**
     * Update an existing tenant role assignment on a group
     *
     * @param group
     * @param tenantRole
     */
    void updateRoleAssignmentOnGroup(UserGroup group, TenantRole tenantRole);

    /**
     * Delete an existing tenant role assignment on a group.
     * This method will remove the tenantId from the tenantRole or delete the tenantRole
     * if the tenantRole contains no tenants after removing the tenantId.
     *
     * @param group
     * @param tenantRole
     */
    void deleteOrUpdateRoleAssignmentOnGroup(UserGroup group, TenantRole tenantRole);

    /**
     * Retrieve the specified tenant role associated with the specified group. Returns null if doesn't exist.
     *
     * @param group
     * @param roleId
     * @return
     */
    TenantRole getRoleAssignmentOnGroup(UserGroup group, String roleId);

    /**
     * Retrieve the tenant roles associated with the specified group. Returns empty iterable if group has no assignments
     *
     * @param group
     * @return
     */
    Iterable<TenantRole> getRoleAssignmentsOnGroup(UserGroup group);

    /**
     * Retrieve the tenant roles associated with the specified group in pagination form.
     *
     * @param group
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnGroup(UserGroup group, UserGroupRoleSearchParams searchParams);

    /**
     * Retrieve the count of groups with the role, specified by role id, assigned.
     *
     * @param roleId
     * @return
     */
    int countGroupsWithRoleAssignment(String roleId);

    /**
     * Retrieve the tenant roles associated with the specified user in pagination form.
     *
     * @param user
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnUser(User user, PaginationParams paginationParams);

    /**
     * Assign the new tenant role to the delegation agreement.
     *
     * @param delegationAgreement
     * @param tenantRole
     */
    void addRoleAssignmentOnDelegationAgreement(DelegationAgreement delegationAgreement, TenantRole tenantRole);

    /**
     * Retrieve the specified tenant role associated with the specified delegation agreement. Returns null if doesn't
     * exist.
     *
     * @param delegationAgreement
     * @param roleId
     * @return
     */
    TenantRole getRoleAssignmentOnDelegationAgreement(DelegationAgreement delegationAgreement, String roleId);

    /**
     * Retrieve the tenant roles associated with the specified delegation agreement in pagination form.
     *
     * @param delegationAgreement
     * @param paginationParams
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnDelegationAgreement(DelegationAgreement delegationAgreement, PaginationParams paginationParams);

    /**
     * Retrieves all the role assignments associated with a delegation agreement. This should only be used by internal
     * services to prevent abuse.
     *
     * @param delegationAgreement
     * @return
     */
    Iterable<TenantRole> getAllRoleAssignmentsOnDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * Returns all the tenant roles explicitly assigned to the given tenant ID on a delegation agreement.
     * Note: This does not return tenant roles assigned to the delegation agreement at the domain level.
     *
     * @param tenantId
     * @return
     */
    Iterable<TenantRole> getTenantRolesForDelegationAgreementsForTenant(String tenantId);

}
