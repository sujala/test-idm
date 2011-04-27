package com.rackspace.idm.domain.service;

import javax.ws.rs.core.UriInfo;

import com.rackspace.idm.domain.entity.ScopeAccessObject;

public interface AuthorizationService {
    
    boolean authorizeRacker(ScopeAccessObject scopeAccess);

    boolean authorizeRackspaceClient(ScopeAccessObject scopeAccess);

    boolean authorizeClient(ScopeAccessObject scopeAccess, String verb, UriInfo uriInfo);

    boolean authorizeCustomerUser(ScopeAccessObject scopeAccess, String customerId);

    boolean authorizeUser(ScopeAccessObject scopeAccess, String customerId, String username);

    boolean authorizeAdmin(ScopeAccessObject scopeAccess, String customerId);

    /**
     * Appropriate for authorizing internal calls.
     */
    boolean authorizeCustomerIdm(ScopeAccessObject scopeAccess);

    /**
     * @param targetScopeAccess ScopeAccess against which the action being performed is being evaluated.
     * @param requestingScopeAccess Representing the caller's credentials
     * @return
     */
    abstract boolean authorizeAsRequestorOrOwner(ScopeAccessObject targetScopeAccess,
        ScopeAccessObject requestingScopeAccess);
}
