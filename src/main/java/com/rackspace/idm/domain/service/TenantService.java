package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    List<Tenant> getTenants();
    
    List<Tenant> getTenantsForParentByTenantRoles(String parentUniqueId);
    
    void addTenantRole(String parentUniqueId, TenantRole role);
    void addTenantRoleToUser(User user, TenantRole role);
    void addTenantRolesToUser(ScopeAccess userAdminScopeAccess, User subUser);
    void addTenantRoleToClient(Application client, TenantRole role);
    void deleteTenantRole(String parentUniqueId, TenantRole role);
    void deleteGlobalRole(TenantRole role);
    void updateTenant(Tenant tenant);
    
    TenantRole getTenantRoleForParentById(String parentUniqueId, String id);
    List<TenantRole> getTenantRolesByParent(String parentUniqueId);
    List<TenantRole> getTenantRolesByParentAndClientId(String parentUniqueId, String clientId);
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getGlobalRolesForUser(User user);
    List<TenantRole> getGlobalRolesForApplication(Application user, FilterParam[] filters);
    List<TenantRole> getGlobalRolesForUser(User user, FilterParam[] filters);
    List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant);
    List<TenantRole> getTenantRolesForUser(User user, FilterParam[] filters);
    List<TenantRole> getTenantRolesForApplication(Application application, FilterParam[] filters);
    List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa);
    boolean hasTenantAccess(ScopeAccess scopeAccess, String tenantId);
    List<User> getUsersForTenant(String tenantId);
    List<User> getUsersWithTenantRole(Tenant tenant, ClientRole role);
    List<TenantRole> getTenantRolesForTenant(String tenantId);
}
