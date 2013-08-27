package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
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

    boolean hasDefaultUserRole(User user);
    boolean hasUserAdminRole(User user);
    boolean hasUserManageRole(User user);
    boolean hasServiceAdminRole(User user);
    boolean hasSameDomain(User caller, User retrievedUser);

    void verifyIdmSuperAdminAccess(String authToken);
    void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyRackerOrIdentityAdminAccess(ScopeAccess authScopeAccess);
    void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserManagedLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserLevelAccess(ScopeAccess authScopeAccess);
    void verifySelf(User requester, User requestedUser);
    void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess);
    void verifyDomain(User retrievedUser, User caller);

    void setConfig(Configuration config);

    boolean hasIdentityAdminRole(User user);
}
