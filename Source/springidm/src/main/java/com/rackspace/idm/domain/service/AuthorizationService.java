package com.rackspace.idm.domain.service;

import javax.ws.rs.core.UriInfo;

import com.rackspace.idm.domain.entity.Entity;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.exception.ForbiddenException;

public interface AuthorizationService {
    
	/**
	 * determines if client token is authorized
	 * 
	 * @param token  - token to check if authorized to access resource
	 * @param object - the entity we are trying to access.
	 * @param authorizedRoles  - the role ids that client must have to access the object. null allowed.
	 */
	void authorize(String token, Entity object, String... authorizedRoles) throws ForbiddenException;
	
    boolean authorizeRacker(ScopeAccess scopeAccess);

    boolean authorizeRackspaceClient(ScopeAccess scopeAccess);

    boolean authorizeClient(ScopeAccess scopeAccess, String verb, UriInfo uriInfo);

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
    abstract boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess,
        ScopeAccess requestingScopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);

    boolean authorizeCloudAdmin(ScopeAccess scopeAccess);
}
