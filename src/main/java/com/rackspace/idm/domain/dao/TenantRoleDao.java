package com.rackspace.idm.domain.dao;

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
    void updateTenantRole(TenantRole tenantRole, String tenantId);
    void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole, String tenantId);
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
}
