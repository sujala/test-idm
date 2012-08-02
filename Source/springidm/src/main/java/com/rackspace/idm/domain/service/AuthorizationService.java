package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Entity;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public interface AuthorizationService {
    
	/**
	 * determines if client token is authorized
	 * 
	 * @param token  - token to check if authorized to access resource
	 * @param object - the entity we are trying to access.
	 * @param authorizedRoles  - the role ids that client must have to access the object. null allowed.
	 */
	void authorize(String token, Entity object, String... authorizedRoles);
	
    boolean authorizeRacker(ScopeAccess scopeAccess);

    boolean authorizeRackspaceClient(ScopeAccess scopeAccess);

    boolean authorizeCustomerUser(ScopeAccess scopeAccess, String customerId);

    boolean authorizeUser(ScopeAccess scopeAccess, String customerId, String username);

    boolean authorizeAdmin(ScopeAccess scopeAccess, String customerId);

    /**
     * Appropriate for authorizing internal calls.
     */
    boolean authorizeCustomerIdm(ScopeAccess scopeAccess);

    /**
     * @param targetScopeAccess ScopeAccess against which the action being performed is being evaluated.
     * @param requestingScopeAccess Representing the caller's credentials
     * @return true/false
     */
    boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess,
        ScopeAccess requestingScopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);

    boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess);

    boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess);

    boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess);

    boolean authorizeCloudUser(ScopeAccess scopeAccess);
    boolean hasDefaultUserRole(ScopeAccess scopeAccess);
    boolean hasUserAdminRole(ScopeAccess scopeAccess);
    boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess);

    void authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess);

    void verifyIdmSuperAdminAccess(String authToken);
    void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserLevelAccess(ScopeAccess authScopeAccess);
    void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess);
    void verifyDomain(User retrievedUser, User caller);
}
