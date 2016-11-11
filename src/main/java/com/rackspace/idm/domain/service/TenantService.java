package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(Tenant tenant);
    Tenant getTenant(String tenantId);
    Tenant checkAndGetTenant(String tenantId);
    Tenant getTenantByName(String name);
    Iterable<Tenant> getTenants();

    void addTenantRoleToUser(BaseUser user, TenantRole role);
    void addTenantRolesToUser(BaseUser user, List<TenantRole> tenantRoles);
    void addCallerTenantRolesToUser(User caller, User user);

    /**
     * Deletes the TenantRole from the user. This will delete the role from the
     * given user regardless if this is a global role or associated with tenants.
     *
     * @param user
     * @param role
     */
    void deleteTenantRoleForUser(EndUser user, TenantRole role);

    /**
     * Deletes the TenantRole from the user for the given tenant. There are two different
     * scenarios this method accounts for:
     * 1) The number of tenants associated with the role is greater than 1. In this case this
     * method will simply remove the given tenant from the role. Afterwards, the user will
     * still have the role assigned to the other tenants.
     * 2) The number of tenants is equal to one. In this case the role is deleted from the user.
     * Afterwards the user will not have the role assigned (either associated with a tenant or globally).
     *
     * @param user
     * @param role
     * @param tenant
     */
    void deleteTenantOnRoleForUser(EndUser user, TenantRole role, Tenant tenant);
    void deleteGlobalRole(TenantRole role);
    void updateTenant(Tenant tenant);

    void deleteRbacRolesForUser(EndUser user);
    
    TenantRole getTenantRoleForUserById(BaseUser user, String roleId);
    boolean doesUserContainTenantRole(BaseUser user, String roleId);
    TenantRole checkAndGetTenantRoleForUserById(EndUser user, String roleId);

    List<TenantRole> getGlobalRolesForUser(BaseUser user);
    List<TenantRole> getRbacRolesForUser(EndUser user);
    List<TenantRole> getGlobalRolesForUser(EndUser user, String applicationId);
    List<TenantRole> getEffectiveTenantRolesForUserOnTenant(EndUser user, Tenant tenant);
    List<TenantRole> getTenantRolesForUser(BaseUser user);
    Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user);

    List<Tenant> getTenantsForUserByTenantRoles(BaseUser user);

    /**
     * Returns TRUE if the user matches the following criteria:
     * - The user does not have any tenants
     * - The user has AT LEAST ONE enabled tenant
     * Returns FALSE in all other cases (all tenants on the user are disable)
     *
     * @param user the user to check tenants for
     *
     */
    boolean allTenantsDisabledForUser(EndUser user);

    PaginatorContext<User> getPaginatedEffectiveEnabledUsersForTenant(String tenantId, int offset, int limit);
    PaginatorContext<User> getEnabledUsersWithEffectiveTenantRole(Tenant tenant, ClientRole role, int offset, int limit);
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

    /**
     * Returns the tenantId of a role with name "compute:default" in the roles list.
     * <p>
     * If that role is not present then it returns the tenantId of a role where the tenant is all digits.
     * (this is currently how we define the mossoId).
     * </p>
     * <p>
     * If neither of those roles exist in the roles parameter then this method returns null.
     * </p>
     *
     * @param roles
     * @throws IllegalArgumentException if roles is null
     */
    String getMossoIdFromTenantRoles(List<TenantRole> roles);

    /**
     * Returns an ephemeral set of TenantRoles that the specified racker has. This includes the Identity based "Racker"
     * role and all groups the user belongs to within EDir
     *
     * @return
     */
    List<TenantRole> getEphemeralRackerTenantRoles(String rackerId);

    /**
     * Returns a unique Identity "Racker" tenant role that all rackers are considered to have.
     *
     * @return
     */
    TenantRole getEphemeralRackerTenantRole();

    /**
     * Returns a list of tenants associated to endpoint.
     *
     * @return
     */
    List<Tenant> getTenantsForEndpoint(String endpointId);
}
