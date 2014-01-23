package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthorizationContext;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import org.apache.commons.configuration.Configuration;

public interface AuthorizationService {

    AuthorizationContext getAuthorizationContext(ScopeAccess scopeAccess);
    AuthorizationContext getAuthorizationContext(User user);

    boolean authorizeRacker(AuthorizationContext context);
    boolean authorizeRackspaceClient(AuthorizationContext context);

    boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess, ScopeAccess requestingScopeAccess);
    boolean authorizeCustomerIdm(AuthorizationContext context);
    boolean authorizeCloudServiceAdmin(AuthorizationContext context);
    boolean authorizeCloudIdentityAdmin(AuthorizationContext context);
    boolean authorizeCloudUserAdmin(AuthorizationContext context);
    boolean authorizeUserManageRole(AuthorizationContext context);
    boolean authorizeCloudUser(AuthorizationContext context);
    boolean authorizeIdmSuperAdminOrRackspaceClient(AuthorizationContext context);
    boolean authorizeIdmSuperAdmin(AuthorizationContext context);

    void checkAuthAndHandleFailure(boolean authorized, AuthorizationContext context);

    boolean hasDefaultUserRole(AuthorizationContext context);
    boolean hasUserAdminRole(AuthorizationContext context);
    boolean hasUserManageRole(AuthorizationContext context);
    boolean hasServiceAdminRole(AuthorizationContext context);
    boolean hasIdentityAdminRole(AuthorizationContext context);
    boolean hasSameDomain(User caller, User retrievedUser);

    void verifyIdmSuperAdminAccess(String authToken);
    void verifyServiceAdminLevelAccess(AuthorizationContext context);
    void verifyRackerOrIdentityAdminAccess(AuthorizationContext context);
    void verifyIdentityAdminLevelAccess(AuthorizationContext context);
    void verifyUserAdminLevelAccess(AuthorizationContext context);
    void verifyUserManagedLevelAccess(AuthorizationContext context);
    void verifyUserLevelAccess(AuthorizationContext context);
    void verifySelf(User requester, User requestedUser);
    void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess);
    void verifyDomain(User retrievedUser, User caller);

    void setConfig(Configuration config);

}
