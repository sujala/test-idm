package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
    
    void addTenantRole(String parentUniqueId, TenantRole role);
    void deleteTenantRole(String parentUniqueId, TenantRole role);
    void updateTenantRole(TenantRole role);
    TenantRole getTenantRoleForParentByRoleName(String parentUniqueId, String roleName);
    TenantRole getTenantRoleForParentByRoleNameAndClientId(String parentUniqueId, String roleName, String clientId);
    List<TenantRole> getTenantRolesByParent(String parentUniqueId);
    List<TenantRole> getTenantRolesByParentAndClientId(String parentUniqueId, String clientId);
}
