package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant checkAndGetTenant(String tenantId);
    Tenant getTenantByName(String name);
    List<Tenant> getTenants();
    PaginatorContext<Tenant> getTenantsPaged(int offset, int limit);
    
    void addTenantRoleToUser(User user, TenantRole role);
    void addCallerTenantRolesToUser(User caller, User user);
    void addTenantRoleToClient(Application client, TenantRole role);
    void deleteTenantRoleForUser(User user, TenantRole role);
    void deleteTenantRoleForApplication(Application application, TenantRole role);
    void deleteGlobalRole(TenantRole role);
    void updateTenant(Tenant tenant);
    
    TenantRole getTenantRoleForUserById(User user, String roleId);
    boolean doesUserContainTenantRole(User user, String roleId);
    TenantRole getTenantRoleForApplicationById(Application application, String id);
    @Deprecated
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getGlobalRolesForUser(User user);
    List<TenantRole> getGlobalRolesForApplication(Application application);
    List<TenantRole> getGlobalRolesForApplication(Application user, String applicationId);
    List<TenantRole> getGlobalRolesForUser(User user, String applicationId);
    List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant);
    List<TenantRole> getTenantRolesForUser(User user);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    @Deprecated
    List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa);
    List<Tenant> getTenantsForUserByTenantRoles(User user);
    boolean hasTenantAccess(User user, String tenantId);
    List<User> getUsersForTenant(String tenantId);
    List<User> getUsersWithTenantRole(Tenant tenant, ClientRole role);
    List<TenantRole> getTenantRolesForTenant(String tenantId);
    boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles);

    List<Tenant> getTenantsByDomainId(String domainId);

    List<Tenant> getTenantsFromNameList(String[] tenants);

    TenantRole getTenantRoleForUser(User user, List<ClientRole> clientRolesForFilter);
    List<String> getIdsForUsersWithTenantRole(String roleId);
	void setTenantDao(TenantDao tenantDao);
    void setTenantRoleDao(TenantRoleDao tenantRoleDao);

    List<TenantRole> getTenantRolesForClientRole(ClientRole role);

    void deleteTenantRole(TenantRole role);

    void addUserIdToTenantRole(TenantRole tenantRole);
}
