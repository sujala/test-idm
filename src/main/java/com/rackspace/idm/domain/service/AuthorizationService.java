package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import org.apache.commons.configuration.Configuration;

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
    void verifyDomain(EndUser retrievedUser, EndUser caller);

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
}
