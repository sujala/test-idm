package com.rackspace.idm.domain.service.impl;

import java.util.UUID;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.RefreshTokenDao;
import com.rackspace.idm.domain.entity.RefreshToken;
import com.rackspace.idm.domain.entity.RefreshTokenDefaultAttributes;
import com.rackspace.idm.domain.service.RefreshTokenService;

public class DefaultRefreshTokenService implements RefreshTokenService {

    private RefreshTokenDao refreshTokenDao;
    
    private int expirationSeconds;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultRefreshTokenService(RefreshTokenDefaultAttributes defaultAttributes,
        RefreshTokenDao refreshTokenDao) {

        this.refreshTokenDao = refreshTokenDao;

        this.expirationSeconds = defaultAttributes.getExpirationSeconds();
    }

    public RefreshToken createRefreshTokenForUser(String username, String clientId) {

        logger.debug("Creating Refresh Token For User: {}", username);

        String tokenString = generateToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenString(tokenString);
        refreshToken.setExpiration(expirationSeconds);
        refreshToken.setExpirationTime(new DateTime().plusSeconds(this.expirationSeconds));
        refreshToken.setOwner(username);
        refreshToken.setRequestor(clientId);

        refreshTokenDao.save(refreshToken);

        logger.debug("Created Refresh Token For User: {} : {}", username, refreshToken);

        return refreshToken;
    }

    public RefreshToken getRefreshTokenByTokenString(String tokenString) {

        logger.debug("Getting refresh token: {}", tokenString);
        RefreshToken refreshToken = refreshTokenDao.findByTokenString(tokenString);
        logger.debug("Got refresh token: {}", refreshToken);
        return refreshToken;
    }

    public RefreshToken getRefreshTokenByUserAndClient(String username, String clientId, DateTime validAfter) {

        logger.debug("Getting refresh token by user and client: {}, {}", username, clientId);
        RefreshToken refreshToken = refreshTokenDao.findTokenForOwner(username, clientId, validAfter);
        logger.debug("Got refresh token: {}", refreshToken);

        return refreshToken;
    }

    public void resetTokenExpiration(RefreshToken token) {
        logger.debug("resetting refresh token expiration: {}", token);

        if (token == null) {
            logger.warn("Null instance of RefreshToken was passed");
            throw new IllegalArgumentException("Null instance of RefreshToken was passed.");
        }

        token.setExpiration(expirationSeconds);
        token.setExpirationTime(new DateTime().plusSeconds(expirationSeconds));
        refreshTokenDao.updateToken(token);
        logger.debug("done resetting refresh token expiration: {}", token);
    }

    public void deleteAllTokensForUser(String username) {
        logger.info("Deleting all refresh tokens for user {}", username);
        refreshTokenDao.deleteAllTokensForUser(username);
        logger.info("Deleted all refresh tokens for user {}", username);
    }

    public void deleteTokenForUserByClientId(String username, String clientId) {
        logger.info("Deleting all refresh tokens for user {} and client {}", username, clientId);
        refreshTokenDao.deleteTokenForUserByClientId(username, clientId);
        logger.info("Deleted all refresh tokens for user {} and client {}", username, clientId);
    }

    // private
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
