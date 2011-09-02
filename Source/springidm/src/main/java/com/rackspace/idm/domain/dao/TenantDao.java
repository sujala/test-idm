package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
    
    void addTenantRoleToParent(String parentUniqueId, TenantRole role);
    void deleteTenantRole(TenantRole role);
    void updateTenantRole(TenantRole role);
    TenantRole getTenantRoleForParentByRoleName(String parentUniqueId, String roleName);
    TenantRole getTenantRoleForParentByRoleNameAndClientId(String parentUniqueId, String roleName, String clientId);
    List<TenantRole> getTenantRolesByParent(String parentUniqueId);
    List<TenantRole> getTenantRolesByParentAndClientId(String parentUniqueId, String clientId);
}
