package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.Credentials;

public interface AuthenticationService {
    
	public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
	
    AuthData authenticate(Credentials credentials);
    
    AuthData getAuthDataFromToken(String authToken);
}
