package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.RDN;

import java.util.List;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
    
    void deleteTenantRole(TenantRole role);
    void updateTenantRole(TenantRole role);
    List<TenantRole> getTenantRolesForUser(User user);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForApplication(Application application);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    List<TenantRole> getAllTenantRolesForTenant(String tenantId);
    List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId);
    boolean doesScopeAccessHaveTenantRole(ScopeAccess scopeAccess, ClientRole role);
    boolean doesUserHaveTenantRole(String uniqueId, ClientRole role);
    List<TenantRole> getAllTenantRolesForClientRole(ClientRole role);

    PaginatorContext<String> getIdsForUsersWithTenantRole(String roleId, int offset, int limit);
}
