package com.rackspace.idm.services;

import com.rackspace.idm.entities.AccessToken;

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
    boolean authorizeCustomerIdm(AccessToken authToken, String verb, String uri);
}
