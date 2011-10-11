package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.Credentials;

public interface AuthenticationService {
    
    AuthData authenticate(Credentials credentials);
    
    AuthData validateAuthToken(String authToken);
}
