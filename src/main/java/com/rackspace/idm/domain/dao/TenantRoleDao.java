package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;

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
    TenantRole getTenantRoleForApplication(Application application, String roleId);
    TenantRole getTenantRoleForUser(User user, String roleId);
    TenantRole getTenantRoleForScopeAccess(ScopeAccess scopeAccess, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(User user, TenantRole tenantRole);
    void deleteTenantRoleForApplication(Application application, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);

}
