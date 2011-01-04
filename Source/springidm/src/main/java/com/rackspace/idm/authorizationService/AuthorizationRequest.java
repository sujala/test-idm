package com.rackspace.idm.authorizationService;

public class AuthorizationRequest {

    Object authorizationRequest;

    public AuthorizationRequest() {
        this.authorizationRequest = new Object();
    }

    public AuthorizationRequest(Object request) {
        this.authorizationRequest = request;
    }

    public Object getRequest() {
        return authorizationRequest;
    }
}