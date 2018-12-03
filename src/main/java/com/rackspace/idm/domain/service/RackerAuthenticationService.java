package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;

public interface RackerAuthenticationService {
    
	String AUTH_TOKEN_HEADER = "X-Auth-Token";
	
    UserAuthenticationResult authenticateDomainUsernamePassword(String username, String password, Domain domain);

    UserAuthenticationResult authenticateDomainRSA(String username, String tokenkey, Domain domain);

    void setRackerAuthDao(RackerAuthDao authDao);

    void setUserService(UserService userService);

    void setInputValidator(InputValidator inputValidator);
}
