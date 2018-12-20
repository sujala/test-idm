package com.rackspace.idm.domain.service.impl;

import com.newrelic.api.agent.Trace;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.dao.impl.RackerAuthResult;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.RSAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultRackerAuthenticationService implements RackerAuthenticationService {

    @Autowired
    private RackerAuthDao rackerAuthDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RSAClient rsaClient;
    @Autowired
    private IdentityConfig identityConfig;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Trace
    @Override
    public UserAuthenticationResult authenticateRackerUsernamePassword(String username, String password) {
        // Server must allow racker auth (e.g. can connect to AD)
        if(!isRackerAuthSupported()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, password, false);
    }

    @Trace
    @Override
    public UserAuthenticationResult authenticateRackerRSA(String username, String tokenkey) {
        // Server must allow racker auth (e.g. can connect to AD)
        if(!isRackerAuthSupported()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, tokenkey, true);
    }

    @Override
    public void setRackerAuthDao(RackerAuthDao authDao) {
        this.rackerAuthDao = authDao;
    }


    @Override
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    UserAuthenticationResult authenticateRacker(String username, String password, boolean usesRSAAuth) {
        logger.debug("Authenticating Racker: {}", username);

        if (!isRackerAuthSupported()) {
            throw new ForbiddenException();
        }
        boolean authenticated;
        if (usesRSAAuth) {
            authenticated = rsaClient.authenticate(username, password);
        } else {
            if (identityConfig.getReloadableConfig().cacheRackerAuthResult()) {
                RackerAuthResult result = rackerAuthDao.authenticateWithCache(username, password);
                authenticated = result != null && result == RackerAuthResult.SUCCESS;
            } else {
                authenticated = rackerAuthDao.authenticate(username, password);
            }
        }
        logger.debug("Authenticated Racker {} : {}", username, authenticated);

        if(!authenticated) {
            throw new NotAuthorizedException("Unable to authenticate user with credentials provided.");
        }

        Racker racker = userService.getRackerByRackerId(username);
        return new UserAuthenticationResult(racker, authenticated);
    }

    boolean isRackerAuthSupported() {
        return identityConfig.getStaticConfig().isRackerAuthAllowed();
    }
}
