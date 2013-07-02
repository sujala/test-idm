package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantRoleDao {
    void addTenantRoleToApplication(Application application, TenantRole tenantRole);
    void addTenantRoleToUser(User user, TenantRole tenantRole);
    List<TenantRole> getTenantRolesForApplication(Application application);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForUser(User user);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getAllTenantRolesForTenant(String tenantId);
    List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    List<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
    TenantRole getTenantRoleForApplication(Application application, String roleId);
    TenantRole getTenantRoleForUser(User user, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(User user, TenantRole tenantRole);
    void deleteTenantRoleForApplication(Application application, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    PaginatorContext<String> getIdsForUsersWithTenantRole(String roleId, int offset, int limit);

    TenantRole getTenantRoleForUser(User user, List<ClientRole> clientRoles);
}
