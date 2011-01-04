package com.rackspace.idm.services;

public interface AuthorizationService {

    boolean authorizeRacker(String authHeader);

    boolean authorizeRackspaceClient(String authHeader);

    boolean authorizeClient(String authHeader, String verb, String uri);

    boolean authorizeCustomerUser(String authHeader, String customerId);

    boolean authorizeUser(String authHeader, String customerId, String username);

    boolean authorizeAdmin(String authHeader, String customerId);
}
