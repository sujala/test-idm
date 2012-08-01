package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
    
    void addTenantRoleToParent(String parentUniqueId, TenantRole role);
    void deleteTenantRole(TenantRole role);
    void updateTenantRole(TenantRole role);
    TenantRole getTenantRoleForParentById(String parentUniqueId, String id);
    List<TenantRole> getTenantRolesByParent(String parentUniqueId);
    List<TenantRole> getTenantRolesByParentAndClientId(String parentUniqueId, String clientId);
    List<TenantRole> getTenantRolesForUser(User user);
    List<TenantRole> getTenantRolesForUser(User user, FilterParam[] filters);
    List<TenantRole> getTenantRolesForApplication(Application application, FilterParam[] filters);
    List<TenantRole> getAllTenantRolesForTenant(String tenantId);
    List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    boolean doesScopeAccessHaveTenantRole(ScopeAccess scopeAccess, ClientRole role);
    List<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
}
