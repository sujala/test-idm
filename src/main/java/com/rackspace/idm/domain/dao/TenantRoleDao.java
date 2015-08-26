package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

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

    Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles);
    String getUserIdForParent(TenantRole tenantRole);
}
