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
    void addTenantRolesToUser(BaseUser user, List<TenantRole> tenantRoles);
    void addCallerTenantRolesToUser(User caller, User user);
    void addTenantRoleToClient(Application client, TenantRole role);
    void deleteTenantRoleForUser(EndUser user, TenantRole role);
    void deleteTenantRoleForApplication(Application application, TenantRole role);
    void deleteGlobalRole(TenantRole role);
    void updateTenant(Tenant tenant);

    void deleteRbacRolesForUser(EndUser user);
    
    TenantRole getTenantRoleForUserById(EndUser user, String roleId);
    boolean doesUserContainTenantRole(BaseUser user, String roleId);
    TenantRole checkAndGetTenantRoleForUserById(EndUser user, String roleId);

    TenantRole getTenantRoleForApplicationById(Application application, String id);
    @Deprecated
    List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess);
    List<TenantRole> getGlobalRolesForUser(BaseUser user);
    List<TenantRole> getGlobalRolesForApplication(Application application);
    List<TenantRole> getGlobalRolesForApplication(Application user, String applicationId);
    List<TenantRole> getGlobalRolesForUser(EndUser user, String applicationId);
    List<TenantRole> getTenantRolesForUserOnTenant(EndUser user, Tenant tenant);
    List<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user);
    List<TenantRole> getTenantRolesForUser(EndUser user, String applicationId, String tenantId);
    List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId);

    @Deprecated
    List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa);
    List<Tenant> getTenantsForUserByTenantRoles(BaseUser user);

    boolean hasTenantAccess(EndUser user, String tenantId);
    List<User> getUsersForTenant(String tenantId, int offset, int limit);
    List<User> getUsersWithTenantRole(Tenant tenant, ClientRole role, int offset, int limit);
    List<TenantRole> getTenantRolesForTenant(String tenantId);
    boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles);

    List<Tenant> getTenantsByDomainId(String domainId);

    List<Tenant> getTenantsFromNameList(String[] tenants);

    Iterable<TenantRole> getTenantRolesForUserById(EndUser user, List<ClientRole> clientRolesForFilter);
    List<String> getIdsForUsersWithTenantRole(String roleId, int sizeLimit);
	void setTenantDao(TenantDao tenantDao);
    void setTenantRoleDao(TenantRoleDao tenantRoleDao);

    Iterable<TenantRole> getTenantRolesForClientRole(ClientRole role);

    void deleteTenantRole(TenantRole role);

    void addUserIdToTenantRole(TenantRole tenantRole);
}
