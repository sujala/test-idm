package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;
import com.rsa.cryptoj.c.B;

import java.util.List;

public interface TenantRoleDao {
    void addTenantRoleToApplication(Application application, TenantRole tenantRole);

    void addTenantRoleToUser(BaseUser user, TenantRole tenantRole);
    Iterable<TenantRole> getTenantRolesForApplication(Application application);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getTenantRolesForUser(EndUser user, String applicationId);
    Iterable<TenantRole> getTenantRolesForUser(EndUser user, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId);
    Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
    TenantRole getTenantRoleForApplication(Application application, String roleId);
    TenantRole getTenantRoleForUser(BaseUser user, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole);
    void deleteTenantRoleForApplication(Application application, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    List<String> getIdsForUsersWithTenantRole(String roleId);
    List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult);

    Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles);
    String getUserIdForParent(TenantRole tenantRole);
}
