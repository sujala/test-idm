package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant checkAndGetTenant(String tenantId);
    Tenant getTenantByName(String name);
    Iterable<Tenant> getTenants();
    PaginatorContext<Tenant> getTenantsPaged(int offset, int limit);

    
    void addTenantRoleToUser(BaseUser user, TenantRole role);
    void addTenantRoleToFederatedToken(FederatedToken token, TenantRole role);
    void addTenantRolesToFederatedToken(FederatedToken token, List<TenantRole> tenantRoles);
    void addCallerTenantRolesToUser(User caller, User user);
    void addTenantRoleToClient(Application client, TenantRole role);
    void deleteTenantRoleForUser(User user, TenantRole role);
    void deleteTenantRoleForApplication(Application application, TenantRole role);
    void deleteGlobalRole(TenantRole role);
    void updateTenant(Tenant tenant);

    void deleteRbacRolesForUser(User user);
    
    TenantRole getTenantRoleForUserById(User user, String roleId);
    boolean doesUserContainTenantRole(BaseUser user, String roleId);
    TenantRole checkAndGetTenantRoleForUserById(User user, String roleId);

    TenantRole getTenantRoleForApplicationById(Application application, String id);
    @Deprecated
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getGlobalRolesForUser(BaseUser user);
    List<TenantRole> getGlobalRolesForApplication(Application application);
    List<TenantRole> getGlobalRolesForApplication(Application user, String applicationId);
    List<TenantRole> getGlobalRolesForUser(User user, String applicationId);
    List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant);
    List<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user);
    List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForFederatedToken(FederatedToken token);
    Iterable<TenantRole> getTenantRolesForFederatedTokenNoDetail(FederatedToken token);
    @Deprecated
    List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa);
    List<Tenant> getTenantsForUserByTenantRoles(User user);
    List<Tenant> getTenantsForFederatedTokenByTenantRoles(FederatedToken token);
    boolean hasTenantAccess(User user, String tenantId);
    List<User> getUsersForTenant(String tenantId, int offset, int limit);
    List<User> getUsersWithTenantRole(Tenant tenant, ClientRole role, int offset, int limit);
    List<TenantRole> getTenantRolesForTenant(String tenantId);
    boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles);

    List<Tenant> getTenantsByDomainId(String domainId);

    List<Tenant> getTenantsFromNameList(String[] tenants);

    Iterable<TenantRole> getTenantRolesForUserById(User user, List<ClientRole> clientRolesForFilter);
    List<String> getIdsForUsersWithTenantRole(String roleId);
	void setTenantDao(TenantDao tenantDao);
    void setTenantRoleDao(TenantRoleDao tenantRoleDao);

    Iterable<TenantRole> getTenantRolesForClientRole(ClientRole role);

    void deleteTenantRole(TenantRole role);

    void addUserIdToTenantRole(TenantRole tenantRole);
}
