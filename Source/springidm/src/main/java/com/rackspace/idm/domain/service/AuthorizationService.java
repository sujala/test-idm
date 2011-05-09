package com.rackspace.idm.domain.service;

import javax.ws.rs.core.UriInfo;

import com.rackspace.idm.domain.entity.ScopeAccess;

public interface AuthorizationService {
    
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
     * @return
     */
    abstract boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess,
        ScopeAccess requestingScopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);
}
