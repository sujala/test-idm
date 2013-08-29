package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantRoleDao {
    void addTenantRoleToApplication(Application application, TenantRole tenantRole);
    void addTenantRoleToUser(User user, TenantRole tenantRole);
    Iterable<TenantRole> getTenantRolesForApplication(Application application);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForUser(User user);
    Iterable<TenantRole> getTenantRolesForUser(User user, String applicationId);
    Iterable<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId);
    Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
    TenantRole getTenantRoleForApplication(Application application, String roleId);
    TenantRole getTenantRoleForUser(User user, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(User user, TenantRole tenantRole);
    void deleteTenantRoleForApplication(Application application, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    List<String> getIdsForUsersWithTenantRole(String roleId);

    TenantRole getTenantRoleForUser(User user, List<ClientRole> clientRoles);
    String getUserIdForParent(TenantRole tenantRole);
}
