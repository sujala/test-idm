package com.rackspace.idm.services;

import com.rackspace.idm.entities.AccessToken;

public interface AuthorizationService {

//    boolean authorizeRacker(String authHeader);
//
//    boolean authorizeRackspaceClient(String authHeader);
//
//    boolean authorizeClient(String authHeader, String verb, String uri);
//
//    boolean authorizeCustomerUser(String authHeader, String customerId);
//
//    boolean authorizeUser(String authHeader, String customerId, String username);
//
//    boolean authorizeAdmin(String authHeader, String customerId);
    
    boolean authorizeRacker(AccessToken token);

    boolean authorizeRackspaceClient(AccessToken token);

    boolean authorizeClient(AccessToken token, String verb, String uri);

    boolean authorizeCustomerUser(AccessToken token, String customerId);

    boolean authorizeUser(AccessToken token, String customerId, String username);

    boolean authorizeAdmin(AccessToken token, String customerId);
}
