package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.NotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(Tenant tenant);

    /**
     * Deletes the tenant from the tenant role. If the tenant is the only tenant on
     * the tenant role, the role is deleted.
     *
     * @param role
     * @param tenantId
     */
    void deleteTenantFromTenantRole(TenantRole role, String tenantId);

    Tenant getTenant(String tenantId);
    Tenant checkAndGetTenant(String tenantId);
    Tenant getTenantByName(String name);
    Iterable<Tenant> getTenants();

    /**
     * Explicitly grant (or modify) a given role assignment to the specified user.
     *
     * If the user is not already explicitly assigned the role, the role is added as is. It can be assigned either
     * globally or on a specific set of tenants.
     *
     * If the user is already explicitly assigned the role one of the following occurs:
     * <ol>
     *     <li>If the requested role assignment is a global role assignment (no tenantIds), the role modification is effectively a no-op.
     *     If the existing assignment included a set of tenants, they remain set. No new tenants are added or removed</li>
     *     <li>If the requested role assignment is a tenant role assignment (includes tenantIds):
     *     <ol>
     *         <li>If the existing role assignment includes a set of tenants:
     *         <ul>
     *             <li>A ClientConflictException is thrown if the request includes a tenantId already assigned</li>
     *             <li>Otherwise the requested tenants are added to the existing assignment</li>
     *         </ul>
     *         </li>
     *         <li>If the existing role assignment is a global assignment, the requested tenants are added to the existing
     *         assignment - effectively turning the global assignment into a tenant based assignment for the specified
     *         tenants</li>
     *     </ol>
     * </ol>
     *
     * If the service does not throw an exception, regardless of whether or not the backend tenant role is actually
     * modified, the following additional logic is performed:
     * <ol>
     *     <li>If 'sendEventFeedForTargetUser' is set to true, a feed event is posted for the user indicating an update of roles</li>
     *     <li>If the user is a user-admin and the role is a propagating role, perform the same assignment on all
     *     subusers (both Federated and Provisioned). Any ClientConflictException resulting from this assignment are
     *     logged, but ignored. Any updates to a provisioned user also results in the posting of a User Update Feed event</li>
     * </ol>
     *
     * @param user
     * @param role
     * @param sendEventFeedForTargetUser
     * @throws NotFoundException - if an application with the role's clientId does not exist or a role with the given role name is not found
     * @throws ClientConflictException - If the requested role assignment on a particular tenant already exists.
     */
    void addTenantRoleToUser(BaseUser user, TenantRole role, boolean sendEventFeedForTargetUser);

    /**
     * Iterates over the specified tenant roles (assignments) and calls {@link #addTenantRoleToUser(BaseUser, TenantRole, boolean)}
     *
     * Does not perform any rollback logic or continuation logic if an exception is encountered.
     *
     * For example, if the list contains 5 role assignments and an exception is thrown adding the 3rd one:
     * <ul>
     *     <li>The first 2 assignments would have been updated and will remain updated.</li>
     *     <li>The last 2 assignments will not even be attempted</li>
     *     <li>The state of the 3rd role assignment depends on the error that was thrown (e.g. if after the update occurred)</li>
     * </ul>
     *
     * @param user
     * @param tenantRoles
     */
    void addTenantRolesToUser(BaseUser user, List<TenantRole> tenantRoles);

    /**
     * Delete the role explicitly assigned to the specified user (Federated or Provisioned).
     *
     * If the user is not explicitly assigned the role, no role is removed (and no exception is thrown)
     *
     * If the user is explicitly assigned the role, one of the following occurs:
     * <ol>
     *     <li>If the requested role deletion is a global role deletion (no tenantIds)
     *          <ul>
     *              <li>If the existing role is also globally assigned, the role assignment is removed</li>
     *              <li>If the existing role is assigned on specific tenants, the role assignment is unaltered</li>
     *          </ul>
     *     </li>
     *     <li>If the requested role deletion targets specific tenants to be removed (includes tenantIds), any tenants
     *     matching that from the request are removed from the existing role assignment. A request to remove a tenant from
     *     the role assignment that is not already assigned is ignored. Then, one of the following occurs:
     *     <ul>
     *         <li>If the modified existing role assignment no longer includes any tenantIds, the entire role assignment
     *         is deleted.
     *         </li>
     *         <li>If the modified existing role assignment still includes one or more tenantIds, the role assignment
     *         is updated to only include these tenants.</li>
     *     </ul>
     * </ol>
     *
     * If the service does not throw an exception, regardless of whether or not a backend tenant role is actually
     * modified, the following additional logic is performed:
     * <ol>
     *     <li>If the user is a provisioned user, a User Role Update Feed event is posted</li>
     *     <li>If the user is a provisioned user-admin and the role is a propagating role, perform the same removal on all
     *     subusers (both Federated and Provisioned). Any ClientConflictException resulting from this assignment are
     *     logged, but ignored. Any updates to a provisioned subuser also results in the posting of a User Role Update Feed event</li>
     * </ol>
     *
     * @param user
     * @param role
     * @param sendEventFeedForTargetUser
     * @throws NotFoundException - if an application with the role's clientId does not exist
     */
    void deleteTenantRoleForUser(EndUser user, TenantRole role, boolean sendEventFeedForTargetUser);

    /**
     * See {@link #deleteTenantRoleForUser(EndUser, TenantRole, boolean)} as this appears to perform the exact same logic
     *
     * TODO: Simplify this service to simply call {@link #deleteTenantRoleForUser(EndUser, TenantRole, boolean)}
     *
     * @param user
     * @param role
     * @param tenant
     */
    void deleteTenantOnRoleForUser(EndUser user, TenantRole role, Tenant tenant);

    /**
     * Deletes the specified role assignment. Neither how the role is assigned (via global or tenant based assignment)
     * nor to what entity the role assignment is on (user or user group) matters.
     *
     * @param role
     */
    void deleteGlobalRole(TenantRole role);

    void updateTenant(Tenant tenant);

    /**
     * Deletes all role assignments explicitly on the user where administratorRole=identity:user-manage (aka 1000 weight
     * roles)
     *
     * This does not account for roles the user receives based on group membership.
     *
     * @param user
     */
    void deleteRbacRolesForUser(EndUser user);

    /**
     * Retrieves a role assignment explicitly assigned to the specified user. Will not return a role assigned via
     * group membership, and will not combine assignments if assigned both explicitly to the user and via user group
     * membership.
     *
     * @param user
     * @param roleId
     * @return
     */
    TenantRole getTenantRoleForUserById(BaseUser user, String roleId);

    /**
     * Whether or not a role is explicitly assigned to the specified user. Does NOT account for whether the user receives
     * the role based on group membership.
     *
     * @param user
     * @param roleId
     * @return
     */
    boolean doesUserContainTenantRole(BaseUser user, String roleId);

    /**
     * Retrieves a role assignment explicitly assigned to the specified user. If the user is not *explicitly* assigned the role,
     * throws a NotFoundException. This service does not account for roles assigned via
     * group membership, and will not combine assignments if assigned both explicitly to the user and via user group
     * membership.
     *
     * @param user
     * @param roleId
     * @return
     */
    TenantRole checkAndGetTenantRoleForUserById(EndUser user, String roleId);

    /**
     * Retrieves the roles explicitly assigned to the user as a "global" (domain) assignment. This will include RCN roles
     * that are assigned globally, but are applied as if assigned on tenants.
     *
     * The roles returned have various properties from the client role populated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>tenant types (if RCN role)</li>
     * </ul>
     *
     * @param user
     * @return
     * @throws IllegalArgumentException If supplied user is null
     */
    List<TenantRole> getGlobalRolesForUser(BaseUser user);

    /**
     * Retrieves the roles "effectively" assigned to the user as "global" (domain) assignment. This will include RCN roles
     *
     * The global roles returned include
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership (if enabled)</li>
     * </ul>
     *
     * The roles returned have various properties from the client role populated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>tenant types (if RCN role)</li>
     * </ul>
     *
     * @param user
     * @return
     * @throws IllegalArgumentException If supplied user is null
     */
    List<TenantRole> getEffectiveGlobalRolesForUserIncludeRcnRoles(BaseUser user);

    /**
     * Retrieves the roles "effectively" assigned to the user as "global" (domain) assignment. This will exclude RCN roles
     *
     * The global roles returned include
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership (if enabled)</li>
     * </ul>
     *
     * The roles returned have various properties from the client role populated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>tenant types (if RCN role)</li>
     * </ul>
     *
     * @param user
     * @return
     * @throws IllegalArgumentException If supplied user is null
     */
    List<TenantRole> getEffectiveGlobalRolesForUserExcludeRcnRoles(BaseUser user);

    /**
     * Returns all roles assignments explicitly made on the user that are assigned globally and have an
     * administratorRole=identity:user-manage (aka 1000 weight role)
     *
     * Also populates the name and description on the returned tenant role with data from the client role.
     *
     * Note - This does NOT return any roles the user receives based on user group membership.
     *
     * @param user
     * @return
     */
    List<TenantRole> getGlobalRbacRolesForUser(EndUser user);

    /**
     * Returns all roles assignments explicitly made to the user, including both those assigned on specific tenants and
     * those assigned globally, that have have an administratorRole=identity:user-manage (aka 1000 weight role)
     *
     * Also populates the name and description on the returned tenant role with data from the client role.
     *
     * Note - This does NOT return any roles the user receives based on user group membership.
     * @param user
     * @return
     */
    List<TenantRole> getRbacRolesForUser(EndUser user);

    /**
     * Returns all roles assignments explicitly made to the user, including both those assigned on specific tenants and
     * those assigned globally.
     *
     * Also populates the name and description on the returned tenant role with data from the client role.
     *
     * Note - This does NOT return any roles the user receives based on user group membership.
     * @param user
     * @return
     */
    List<TenantRole> getExplicitlyAssignedTenantRolesForUserPerformant(EndUser user);

    /**
     * Retrieves the specified application's roles that are "effectively" assigned to the user as "global" (domain)
     * assignment. This will include RCN roles
     *
     * The global roles returned include
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership (if enabled)</li>
     * </ul>
     *
     * The roles returned have various properties from the client role populated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>tenant types (if RCN role)</li>
     * </ul>
     *
     * @param user
     * @param applicationId
     * @return
     * @throws IllegalArgumentException If supplied user is null
     */
    List<TenantRole> getEffectiveGlobalRolesForUserIncludeRcnRoles(BaseUser user, String applicationId);

    /**
     * Retrieves the specified application's roles that are "effectively" assigned to the user as "global" (domain)
     * assignment. This will exclude RCN roles
     *
     * The global roles returned include
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership (if enabled)</li>
     * </ul>
     *
     * The roles returned have various properties from the client role populated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>tenant types (if RCN role)</li>
     * </ul>
     *
     * @param user
     * @param applicationId
     * @return
     * @throws IllegalArgumentException If supplied user is null
     */
    List<TenantRole> getEffectiveGlobalRolesForUserExcludeRcnRoles(EndUser user, String applicationId);

    /**
     * Return the set of roles the user "effectively" has on the specified tenant. This includes all explicitly assigned
     * roles on that tenant, the identity:tenant-access role (if enabled), and any roles received via group membership on
     * that tenant (if enabled).
     *
     * RCN roles on the user are NOT applied, so no RCN roles will be returned. Domain assigned roles are also not returned.
     *
     * @param user
     * @param tenant
     * @return
     */
    List<TenantRole> getEffectiveTenantRolesForUserOnTenant(EndUser user, Tenant tenant);

    /**
     * Return the set of roles the user "effectively" has on the specified tenant. This includes all explicitly assigned
     * roles, the identity:tenant-access role (if enabled), and any roles received via group membership (if enabled).
     *
     * RCN roles on the user are applied, so RCN roles will be returned if applicable. Furthermore, roles domain assigned
     * to the user will be returned for tenants within the same domain as the user.
     *
     * @param user
     * @param tenant
     * @return
     */
    List<TenantRole> getEffectiveTenantRolesForUserOnTenantApplyRcnRoles(EndUser user, Tenant tenant);

    /**
     * Retrieves all the roles "effectively" assigned to the user. The resultant tenant roles are further fully populated
     * with all the client role details such as role
     * name, propagation, description, etc that are stored in the "client role" rather than that "tenant role".
     *
     * The client role details are loaded from back end client role w/o use of the client role cache.
     *
     * The calculation of which roles the user has includes:
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership (if enabled)</li>
     * <li>Implicitly assigned identity:tenant-access role (if enabled)</li>
     * </ul>
     *
     * {@link #getTenantRolesForUserPerformant(BaseUser)} should generally be preferred over this method as it's more
     * performant by retrieving client role detail information via the cache rather than hitting the backend for each role.
     *
     * @param user
     * @return
     * @deprecated Use {@link #getTenantRolesForUserPerformant(BaseUser)} instead
     */
    List<TenantRole> getTenantRolesForUser(BaseUser user);

    /**
     * Given a set of roleIds, returns the subset for which the specified user is explicitly assigned. This does NOT account
     * for roles the user receives via group membership or the identity:tenant-access logic.
     *
     * @param user
     * @param roleIds
     * @return
     */
    Iterable<TenantRole> getTenantRolesForUserWithId(User user, Collection<String> roleIds);

    /**
     * Retrieves the count of tenants of the given type within the specified domain.
     *
     * @param tenantType
     * @param domainId
     * @return
     */
    int countTenantsWithTypeInDomain(String tenantType, String domainId);

    /**
     * Returns a list of effective tenant roles the user has assigned. This includes all roles the user explicitly has
     * assigned on tenants, the automatically assigned "access" role to all tenants within the user's
     * domain (if enabled), and those due to group membership (if enabled).
     *
     * The returned roles are populated with "role details" such as name, and role type from the client role cache.
     *
     * If an associated client role is not found for an assigned tenant role, that role assignment is simply ignored.
     *
     * @param user
     * @return
     */
    List<TenantRole> getTenantRolesForUserPerformant(BaseUser user);

    /**
     * Retrieves a list of tenant roles explicitly assigned (global or on a tenant) to a user without the details such as name, propagate, etc populated.
     *
     * This does not include roles received due to group membership or the identity:tenant-access implicity assigned role.
     *
     * @param user
     * @return
     */
    Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user);

    /**
     * Returns all enabled tenants on which the user has a role assigned - excluding those tenants to which the user only has access
     * via the application of RCN logic.
     *
     * The calculation of which roles the user has includes:
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership</li>
     * <li>Implicitly assigned identity:tenant-access role</li>
     * </ul>
     *
     * The calculation of applicable tenants explicitly *excludes* the application of RCN logic across a user's RCN.
     * This means even if the user has an RCN role that would grant access to a tenant in another domain (and this is
     * the only role assignment that would grant such access), this service will not return that tenant.
     *
     * @param user
     * @return
     */
    List<Tenant> getTenantsForUserByTenantRoles(EndUser user);

    /**
     * Returns all enabled tenants on which the user has a role assigned - including those tenants to which the user has access
     * via the application of RCN logic.
     *
     * The calculation of which roles the user has includes:
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership</li>
     * <li>Implicitly assigned identity:tenant-access role</li>
     * </ul>
     *
     * The calculation of applicable tenants explicitly *excludes* the application of RCN logic across a user's RCN.
     * This means even if the user has an RCN role that would grant access to a tenant in another domain (and this is
     * the only role assignment that would grant such access), this service will not return that tenant.
     *
     * @param user
     * @return
     */
    List<Tenant> getTenantsForUserByTenantRolesApplyRcnRoles(EndUser user);

    /**
     * Returns TRUE if the user matches the following criteria:
     * - The user does not have any tenants
     * - The user has AT LEAST ONE enabled tenant
     * Returns FALSE in all other cases (all tenants on the user are disable)
     *
     * This solely considers those tenants on which the user has an explicitly assigned role. It does *not* account for
     * tenants on which the user receives a role via group membership, via the
     * identity:tenant-access implicit role logic, or through global roles.
     *
     * @param user the user to check tenants for
     *
     */
    boolean allTenantsDisabledForUser(EndUser user);

    /**
     * Return a list of enabled provisioned users that are assigned a role on the specified tenant. Callers may optionally
     * limit the results to those users with the specified role on the tenant and use pagination. Services takes into account
     * <ul>
     *     <li>Direct Assignments to Users</li>
     *     <li>Group Membership</li>
     *     <li>System assigned roles</li>
     * </ul>
     *
     * Does not take into consideration RCN roles or roles assigned to users on the domain (global).
     *
     * @param tenant
     * @param paginationParams
     * @return
     */
    PaginatorContext<User> getEnabledUsersForTenantWithRole(Tenant tenant, String limitingRoleId, PaginationParams paginationParams);

    /**
     * Return the list of enabled users with contactId for tenant.
     * @param tenantId
     * @param contactId
     * @throws IllegalArgumentException If supplied tenantId is null or empty string
     * @throws IllegalArgumentException If supplied contactId is null or empty string
     * @return
     */
    List<User> getEnabledUsersWithContactIdForTenant(String tenantId, String contactId);

    /**
     * Returns all role assignments to users or user groups that are explicitly made for the specified tenant.
     *
     * Populates the description and name from the client role.
     *
     * @param tenantId
     * @return
     */
    List<TenantRole> getTenantRolesForTenant(String tenantId);

    /**
     * Whether or not at least one of the specified roles is explicitly assigned the tenantId.
     *
     * @param tenantId
     * @param roles
     * @return
     */
    boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles);

    /**
     * Return all the tenants that belong to the specified domainId based on the list of tenants linked to within the domain
     * entity.
     *
     * @param domainId
     * @return
     * @throws NotFoundException if the domain does not include any tenants
     */
    List<Tenant> getTenantsByDomainId(String domainId);

    /**
     * Return the tenants with enabled status that belong to the specified domainId.
     *
     * @param domainId
     * @param enabled
     * @return
     * @throws NotFoundException if the domain does not include any tenants
     */
    List<Tenant> getTenantsByDomainId(String domainId, boolean enabled);

    /**
     * Return all the tenant ids that belong to the specified domainId based on the list of tenants linked to within the domain
     * entity.
     *
     * @param domain
     * @return
     */
    String[] getTenantIdsForDomain(Domain domain);

    List<Tenant> getTenantsFromNameList(String[] tenants);

    /**
     * Given a set of client roles, return those that are explicitly assigned to the user. Does not account for identity:tenant-access
     * or roles received via group membership.
     *
     * @param user
     * @param clientRolesForFilter
     * @return
     */
    Iterable<TenantRole> getTenantRolesForUserById(EndUser user, List<ClientRole> clientRolesForFilter);

    /**
     * Retrieves all users (up to specified limit) which are explicitly assigned the tenant role. THis excludes those
     * users receiving the role via group membership.
     *
     * TODO: This call will incorrectly return a user group id as a "user id" if a returned tenant role is assigned to
     * a user group rather than a user. Created https://jira.rax.io/browse/CID-1165 to address this issue.
     *
     * @param roleId
     * @param sizeLimit
     * @return
     */
    List<String> getIdsForUsersWithTenantRole(String roleId, int sizeLimit);

    void setTenantDao(TenantDao tenantDao);
    void setTenantRoleDao(TenantRoleDao tenantRoleDao);

    /**
     * Return all explicit role assignments to a user or user group for the specified role
     *
     * @param role
     * @return
     */
    Iterable<TenantRole> getTenantRolesForClientRole(ClientRole role);

    /**
     * Deletes the specified tenant role - regardless of user type (provisioned or federated) or if on a user group.
     *
     * @param role
     */
    void deleteTenantRole(TenantRole role);

    /**
     * Updates a tenant role to set the userId based on the parent entity of the tenant role. This must not occur on
     * user groups as the parent entity is a user group rather than a user.
     *
     * See https://jira.rax.io/browse/CID-1167
     *
     * @param tenantRole
     */
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
     * Retrieves the set of "roles" that a Racker within the given rackerId is assigned. The groups the user belongs to
     * within IAM are retrieved and dynamically converted/adapted into a "TenantRole" (no roleRsId). Additionally, the
     * identity specific "Racker role" is automatically included.
     *
     * This does *not* include implicit roles the racker may receive in Identity due to membership in an IAM group.
     *
     * @param rackerId
     * @return
     */
    List<TenantRole> getEphemeralRackerTenantRoles(String rackerId);

    /**
     * Returns the tenants that are explicitly assigned the specified endpoint.
     *
     * Note - This does not account for tenant type
     * mapping rules where a given tenant will receive endpoints based on having a given tenant type.
     *
     * @param endpointId
     * @return
     */
    List<Tenant> getTenantsForEndpoint(String endpointId);

    /**
     * Given a tenant ID, return the default tenant type for the tenant based on the tenant ID/name prefix using the following logic:
     *
     * 1) If the tenant's id/name can be parsed as a java integer (a legacy mosso restriction), set/infer the tenant type 'cloud'
     * 2) Else if the tenant id/name is prefixed with the value of the configuration property 'nast.tenant.prefix', set/infer the tenant type "files"
     * 3) Else if the tenant id/name is prefixed w/ "hybrid:", set/infer the tenant type "managed_hosting"
     * 4) Else if the tenant id/name contains a ":", search for a tenant type that matches the string prior to the first ":". If exists, set/infer that as tenant type (e.g. asdf:332 would set/infer a tenant type "asdf" if it exists)
     * 5) Else, do not set a tenant type
     *
     */
    String inferTenantTypeForTenantId(String tenantId);

    /**
     * Temporary service to infer the tenant type based on a passed in set of available tenant types. This is a temporary
     * service. It should not be used for any other service to allow it to easily removed in the future.
     * @param tenantId
     * @param existingTenantTypes
     * @deprecated
     * @return
     */
    @Deprecated
    String inferTenantTypeForTenantId(String tenantId, Set<String> existingTenantTypes);

    /**
     * Temporary service to retrieve the list of tenant types in the same manner as used internaally by the service.
     * This is a temporary service that breaks encapsulation and must not be used for any other code so it can easily be
     * removed in the future.
     * @deprecated
     * @return
     */
    @Deprecated
    Set<String> getTenantTypes();

    /**
     * Returns the set of Identity managed roles associated with the Racker. This includes implicit roles the user
     * receives due to IAM group membership. It does *not* return the racker's IAM group memberships.
     *
     * @param racker
     * @return
     */
    SourcedRoleAssignments getSourcedRoleAssignmentsForRacker(Racker racker);

    /**
     * Return the set of effective role assignments on tenants for the specified user. The result includes the exact
     * tenants the user is considered to have those roles on.
     *
     * The service considers:
     * <ul>
     *     <li>All roles explicitly assigned to user regardless of assignment via global or on tenant</li>
     *     <li>The identity:tenant-access role as appropriate for tenants in user's domain</li>
     *     <li>Roles received via group membership (if enabled for user's domain)</li>
     *     <li>RCN Role logic to apply RCN roles across all applicable tenants with the user's RCN</li>
     *     <li>Hidden tenants that a user only receives domain roles if explicitly assigned a role on the tenant</li>
     * </ul>
     *
     * There is a possibility that an empty set of tenant ids is listed for the role. This means that while the user is
     * assigned the role, the user does not have that role on any tenants. For example, a user could be assigned an
     * RCN role, but that RCN role is not applicable for any tenants within the user's RCN.
     *
     * The set of sources by which the user is assigned a role is returned for each role.
     *
     * @param user
     * @return
     */
    SourcedRoleAssignments getSourcedRoleAssignmentsForUser(EndUser user);

}
