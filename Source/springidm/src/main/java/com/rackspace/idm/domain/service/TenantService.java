package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
    
    List<Tenant> getTenantsForParentByTenantRoles(String parentUniqueId);
    
    void addTenantRole(String parentUniqueId, TenantRole role);
    void addTenantRoleToUser(User user, TenantRole role);
    void addTenantRoleToClient(Client client, TenantRole role);
    void deleteTenantRole(String parentUniqueId, TenantRole role);
    TenantRole getTenantRoleForParentById(String parentUniqueId, String id);
    List<TenantRole> getTenantRolesByParent(String parentUniqueId);
    List<TenantRole> getTenantRolesByParentAndClientId(String parentUniqueId, String clientId);
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getGlobalRolesForUser(User user);
    List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant);
    List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa);
    void deleteGlobalRole(TenantRole role);
    List<User> getUsersForTenant(String tenantId);
}
