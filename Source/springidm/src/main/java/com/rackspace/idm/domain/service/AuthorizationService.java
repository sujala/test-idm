package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AccessToken;

public interface AuthorizationService {
    
    boolean authorizeRacker(AccessToken token);

    boolean authorizeRackspaceClient(AccessToken token);

    boolean authorizeClient(AccessToken token, String verb, String uri);

    boolean authorizeCustomerUser(AccessToken token, String customerId);

    boolean authorizeUser(AccessToken token, String customerId, String username);

    boolean authorizeAdmin(AccessToken token, String customerId);

    /**
     * Appropriate for authorizing internal calls.
     */
    boolean authorizeCustomerIdm(AccessToken authToken);

    /**
     * @param targetToken Token against which the action being performed is being evaluated.
     * @param requestingToken Representing the caller's credentials
     * @return
     */
    abstract boolean authorizeAsRequestorOrOwner(AccessToken targetToken, AccessToken requestingToken);
}
