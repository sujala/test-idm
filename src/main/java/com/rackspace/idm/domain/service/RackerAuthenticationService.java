package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;

public interface RackerAuthenticationService {
    
	String AUTH_TOKEN_HEADER = "X-Auth-Token";
	
    UserAuthenticationResult authenticateRackerUsernamePassword(String username, String password);

    UserAuthenticationResult authenticateRackerRSA(String username, String tokenkey);

    void setRackerAuthDao(RackerAuthDao authDao);

    void setUserService(UserService userService);
}
