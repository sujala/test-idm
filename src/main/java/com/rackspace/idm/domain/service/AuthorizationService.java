package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;

import java.util.Collection;
import java.util.List;

public interface AuthorizationService {

    boolean authorizeRacker(ScopeAccess scopeAccess);
    boolean authorizeRackspaceClient(ScopeAccess scopeAccess);

    boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess, ScopeAccess requestingScopeAccess);
    boolean authorizeCustomerIdm(ScopeAccess scopeAccess);
    boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess);
    boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess);
    boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess);
    boolean authorizeUserManageRole(ScopeAccess scopeAccess);
    boolean authorizeCloudUser(ScopeAccess scopeAccess);
    boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);

    boolean isSelf(User requester, User requestedUser);
    boolean isDefaultUser(User user);

    boolean hasDefaultUserRole(EndUser user);
    boolean hasUserAdminRole(EndUser user);
    boolean hasUserManageRole(EndUser user);
    boolean hasServiceAdminRole(EndUser user);
    boolean hasSameDomain(User caller, User retrievedUser);

    void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess);

    /**
     * Verifies a user can impersonate another user.
     *
     * @param caller
     * @param callerToken
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the specified caller does not exist (e.g. Racker doesn't exist in eDir)
     * @throws ForbiddenException If the user does not have the appropriate role to impersonate another user.
     */
    void verifyCallerCanImpersonate(BaseUser caller, ScopeAccess callerToken);

    void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserManagedLevelAccess(ScopeAccess authScopeAccess);

    /**
     * Determines whether the specified user has AT LEAST user-manager level access
     * @param user
     *
     * @throws com.rackspace.idm.exception.NotAuthorizedException if the user does not have the specified access
     */
    void verifyUserManagedLevelAccess(EndUser user);

    /**
     * Determines whether the specified userType has AT LEAST user-manager level access
     * @param userType
     *
     * @throws com.rackspace.idm.exception.NotAuthorizedException if the userType does not have the specified access
     */
    void verifyUserManagedLevelAccess(IdentityUserTypeEnum userType);

    void verifyUserLevelAccess(ScopeAccess authScopeAccess);
    void verifySelf(User requester, User requestedUser);
    void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess);
    void verifyDomain(BaseUser retrievedUser, BaseUser caller);

    void setConfig(Configuration config);

    boolean hasIdentityAdminRole(EndUser user);

    /**
     * Convert an identity 'classification' role to a static enum that can be subsequently used for comparison/identification purposes
     *
     * @return
     * @throws java.lang.IllegalArgumentException If the provided role is not an identity classification role
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(ClientRole identityTypeRole);

    /**
     * Return the classification of the user as a static enum that can be subsequently used for comparison/identification purposes.
     * Returns null if the user is a Racker or the specified user does not have a classification role.
     *
     * @param baseUser
     * @return
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(BaseUser baseUser);

    /**
     * Given a collection of tenant roles associated with the user, determine the associated Identity role for that user
     *
     * @param userRoles
     * @return
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(Collection<TenantRole> userRoles);

    /**
     * Look up an identity role by id within the cache that is populated at node startup
     *
     * @param id
     * @return
     */
    ImmutableClientRole getCachedIdentityRoleById(String id);

    /**
     * Look up the identity role by name within the cache that is populated at node startup
     *
     * @param name
     * @return
     */
    ImmutableClientRole getCachedIdentityRoleByName(String name);

    /**
     * Whether the effective caller has the specified identity type (or higher) and/or identity role with the
     * specified name. This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleName
     */
    boolean authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName);

    /**
     * Whether the effective caller has at least one role with the specified role name. This can only be used to check
     * identity RBAC roles.
     *
     * @param roleNames
     */
    boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames);

    /**
     * Whether the effective caller has at least one role with the specified role name. This can only be used to check
     * identity RBAC roles.
     *
     * @param roleNames
     */
    boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(String... roleNames);

    /**
     * Verifies that the effective caller has the specified identity type (or higher) and/or a role with the
     * specified name. If so, the method returns. Otherwise, throws a ForbiddenException.
     *
     * This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleName
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName);

    /**
     * Verifies that the effective caller has the specified identity type (or higher). If so, the method returns;
     * otherwise, throws a ForbiddenException.
     *
     * @param identityType
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum identityType);

    /**
     * Verifies that the effective caller has the specified identity type (or higher) and/or a role with the
     * specified name. If so, the method returns. Otherwise, throws a ForbiddenException.
     *
     * This can only be used to check identity RBAC roles.
     *
     * @param roleNames
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames);

    /**
     * Verifies that the effective caller has a role with the specified name (either as tenant or global role). If so,
     * the method returns.Otherwise, throws a ForbiddenException.
     *
     * @param roleName
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasRoleByName(String roleName);

    /**
     * Return the list of implicit roles associated with the role with the given name.
     *
     * @param roleName
     * @return
     */
    List<ImmutableClientRole> getImplicitRolesForRole(String roleName);

    /**
     * Checks that a user should be considered disabled based on the user state and the service catalog information
     * that would be returned for the user.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     * - The IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP == true
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictUserAuthentication(ServiceCatalogInfo serviceCatalogInfo);

    /**
     * Checks that a user should be considered SUSPENDED based on the user state
     * and the endpoints for their tokens would be returned.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     * - The IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP == true
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictTokenEndpoints(ServiceCatalogInfo serviceCatalogInfo);

    /**
     * Checks that a user should be considered SUSPENDED based on the user state.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictEndpointsForTerminator(ServiceCatalogInfo serviceCatalogInfo);

}