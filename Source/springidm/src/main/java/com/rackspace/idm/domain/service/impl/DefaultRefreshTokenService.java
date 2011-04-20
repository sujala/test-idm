package com.rackspace.idm.domain.service.impl;

import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.RefreshToken;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.RefreshTokenService;

public class DefaultRefreshTokenService implements RefreshTokenService {
    
    private final ScopeAccessDao scopeAccessDao;
    private final UserDao userDao;
    private final Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultRefreshTokenService(ScopeAccessDao scopeAccessDao, UserDao userDao, Configuration config) {
        this.userDao = userDao;
        this.scopeAccessDao = scopeAccessDao;
        this.config = config;
    }
    
    public int getDefaultRefreshTokenExpirationSeconds() {
        return config.getInt("token.refreshTokenExpirationSeconds");
    }

    @Override
    public RefreshToken createRefreshTokenForUser(BaseUser user, BaseClient client) {
        logger.debug("Creating Refresh Token For User: {}", user);
        
        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByUsernameAndClientId(user.getUsername(), client.getClientId());
        
        if (scopeAccess == null) {
            throw new IllegalStateException("User is not provisioned for this client");
        }

        String tokenString = generateToken();
        
        scopeAccess.setRefreshToken(tokenString);
        scopeAccess.setRefreshTokenExpiration(new DateTime().plusSeconds(this.getDefaultRefreshTokenExpirationSeconds()));
        
        scopeAccessDao.updateScopeAccess(scopeAccess);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenString(tokenString);
        refreshToken.setExpirationTime(new DateTime().plusSeconds(this.getDefaultRefreshTokenExpirationSeconds()));
        refreshToken.setOwner(user.getUsername());
        refreshToken.setRequestor(client.getClientId());

        logger.debug("Created Refresh Token For User: {} : {}", user.getUsername(), refreshToken);

        return refreshToken;
    }

    @Override
    public RefreshToken getRefreshTokenByTokenString(String tokenString) {
        logger.debug("Getting refresh token: {}", tokenString);
        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByRefreshToken(tokenString);
        if (scopeAccess == null) {
            return null;
        }
        RefreshToken refreshToken = getRefreshTokenFromScopeAccess(scopeAccess);
        logger.debug("Got refresh token: {}", refreshToken);
        return refreshToken;
    }

    @Override
    public RefreshToken getRefreshTokenByUserAndClient(String username, String clientId, DateTime validAfter) {
        logger.debug("Getting refresh token by user and client: {}, {}", username, clientId);
        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId);
        if (scopeAccess == null || validAfter.isAfter(scopeAccess.getRefreshTokenExpiration())) {
            return null;
        }
        RefreshToken refreshToken = getRefreshTokenFromScopeAccess(scopeAccess);
        logger.debug("Got refresh token: {}", refreshToken);
        return refreshToken;
    }

    @Override
    public void resetTokenExpiration(RefreshToken token) {
        logger.debug("resetting refresh token expiration: {}", token);
        if (token == null) {
            throw new IllegalArgumentException();
        }
        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByRefreshToken(token.getTokenString());
        DateTime newExpiration = new DateTime().plusSeconds(this.getDefaultRefreshTokenExpirationSeconds());
        scopeAccess.setRefreshTokenExpiration(newExpiration);
        scopeAccessDao.updateScopeAccess(scopeAccess);
        token.setExpirationTime(newExpiration);
        logger.debug("done resetting refresh token expiration: {}", token);
    }

    @Override
    public void deleteAllTokensForUser(String username) {
        logger.info("Deleting all refresh tokens for user {}", username);
        User user = userDao.getUserByUsername(username);
        List<ScopeAccess> scopes = scopeAccessDao.getScopeAccessesByParent(user.getUniqueId());
        for (ScopeAccess scope : scopes) {
            scope.setRefreshTokenExpiration(new DateTime().minusSeconds(this.getDefaultRefreshTokenExpirationSeconds()));
            scopeAccessDao.updateScopeAccess(scope);
        }
        logger.info("Deleted all refresh tokens for user {}", username);
    }

    @Override
    public void deleteTokenForUserByClientId(String username, String clientId) {
        logger.info("Deleting all refresh tokens for user {} and client {}", username, clientId);
        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId);
        scopeAccess.setRefreshTokenExpiration(new DateTime().minusSeconds(getDefaultRefreshTokenExpirationSeconds()));
        scopeAccessDao.updateScopeAccess(scopeAccess);
        logger.info("Deleted all refresh tokens for user {} and client {}", username, clientId);
    }

    // private
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private RefreshToken getRefreshTokenFromScopeAccess(ScopeAccess scopeAccess) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpirationTime(scopeAccess.getRefreshTokenExpiration());
        refreshToken.setTokenString(scopeAccess.getRefreshToken());
        refreshToken.setOwner(scopeAccess.getUsername());
        refreshToken.setRequestor(scopeAccess.getClientId());
        return refreshToken;
    }
}
