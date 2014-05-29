package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;
import com.rsa.cryptoj.c.B;

import java.util.List;

public interface TenantRoleDao {
    void addTenantRoleToApplication(Application application, TenantRole tenantRole);

    void addTenantRoleToUser(BaseUser user, TenantRole tenantRole);
    void addTenantRoleToFederatedToken(FederatedToken token, TenantRole tenantRole);
    Iterable<TenantRole> getTenantRolesForApplication(Application application);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId);
    Iterable<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getTenantRolesForFederatedToken(FederatedToken token);
    Iterable<TenantRole> getTenantRolesForUser(User user, String applicationId);
    Iterable<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    Iterable<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId);
    Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role);
    TenantRole getTenantRoleForApplication(Application application, String roleId);
    TenantRole getTenantRoleForUser(BaseUser user, String roleId);
    TenantRole getTenantRoleForFederatedToken(FederatedToken token, String roleId);
    void updateTenantRole(TenantRole tenantRole);
    void deleteTenantRoleForUser(User user, TenantRole tenantRole);
    void deleteTenantRoleForFederatedToken(FederatedToken token, TenantRole role);
    void deleteTenantRoleForApplication(Application application, TenantRole tenantRole);
    void deleteTenantRole(TenantRole tenantRole);
    List<String> getIdsForUsersWithTenantRole(String roleId);
    List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult);

    Iterable<TenantRole> getTenantRoleForUser(User user, List<ClientRole> clientRoles);
    String getUserIdForParent(TenantRole tenantRole);
}
