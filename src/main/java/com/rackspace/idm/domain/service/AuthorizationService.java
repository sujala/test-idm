package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.*;
import org.apache.commons.configuration.Configuration;

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
    boolean authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess);
    boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);

    boolean isSelf(User requester, User requestedUser);
    boolean isDefaultUser(User user);

    boolean hasDefaultUserRole(EndUser user);
    boolean hasUserAdminRole(EndUser user);
    boolean hasUserManageRole(EndUser user);
    boolean hasServiceAdminRole(EndUser user);
    boolean hasSameDomain(User caller, User retrievedUser);

    void verifyIdmSuperAdminAccess(String authToken);
    void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyRackerOrIdentityAdminAccess(ScopeAccess authScopeAccess);
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
     *
     * @param user
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictUserAuthentication(EndUser user, ServiceCatalogInfo serviceCatalogInfo);
}
