package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.Credentials;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;

public interface AuthenticationService {
    
	String AUTH_TOKEN_HEADER = "X-Auth-Token";
	
    AuthData authenticate(Credentials credentials);

    UserAuthenticationResult authenticateDomainUsernamePassword(String username, String password, Domain domain);

    UserAuthenticationResult authenticateDomainRSA(String username, String tokenkey, Domain domain);

    AuthData getAuthDataFromToken(String authToken);

    void setAuthDao(AuthDao authDao);

    void setTenantService(TenantService tenantService);

    void setScopeAccessService(ScopeAccessService scopeAccessService);

    void setApplicationDao(ApplicationDao applicationDao);

    void setConfig(Configuration appConfig);

    void setUserDao(UserDao userDao);

    void setCustomerDao(CustomerDao customerDao);

    void setInputValidator(InputValidator inputValidator);
}
