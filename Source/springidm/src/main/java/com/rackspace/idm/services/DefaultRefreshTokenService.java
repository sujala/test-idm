package com.rackspace.idm.services;

import java.security.NoSuchAlgorithmException;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.RefreshTokenDefaultAttributes;
import com.rackspace.idm.util.HashHelper;

public class DefaultRefreshTokenService implements RefreshTokenService {
    
    private RefreshTokenDao refreshTokenDao;
    private Logger logger;
    private int expirationSeconds;
    private String dataCenterPrefix;
    
    public DefaultRefreshTokenService(
        RefreshTokenDefaultAttributes defaultAttributes,
        RefreshTokenDao refreshTokenDao,
        Logger logger) {
        
        this.refreshTokenDao = refreshTokenDao;
        this.logger = logger;
        
        this.expirationSeconds = defaultAttributes.getExpirationSeconds();
        this.dataCenterPrefix = defaultAttributes.getDataCenterPrefix();
    }
    
    public RefreshToken createRefreshTokenForUser(String username, String clientId) {
        
        logger.debug("Creating Refresh Token For User: {}", username);
        
        String tokenString = generateTokenWithDcPrefix();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenString(tokenString);
        refreshToken.setExpiration(expirationSeconds);
        refreshToken.setExpirationTime(new DateTime().plusSeconds(this.expirationSeconds));
        refreshToken.setOwner(username);
        refreshToken.setRequestor(clientId);
        
        refreshTokenDao.save(refreshToken);
        
        logger.debug("Created Refresh Token For User: {} : {}", username,
            refreshToken);
        
        return refreshToken;
    }

    public RefreshToken getRefreshTokenByTokenString(String tokenString) {
        
        logger.debug("Getting refresh token: {}", tokenString);
        RefreshToken refreshToken = (RefreshToken) refreshTokenDao.findByTokenString(tokenString);
        logger.debug("Got refresh token: {}", refreshToken);
        return refreshToken;
    }

    public RefreshToken getRefreshTokenByUserAndClient(String username,
        String clientId, DateTime validAfter) {
        
        logger.debug("Getting refresh token by user and client: {}, {}", 
            username, clientId);
        RefreshToken refreshToken = refreshTokenDao
            .findTokenForOwner(username, clientId, validAfter);
        logger.debug("Got refresh token: {}", refreshToken);
        
        return refreshToken;
    }

    public void resetTokenExpiration(RefreshToken token) {
        logger.debug("resetting refresh token expiration: {}", token);
        
        if (token == null) {
            logger.error("Null instance of RefreshToken was passed");
            throw new IllegalArgumentException(
                "Null instance of RefreshToken was passed.");
        }
        
        token.setExpiration(expirationSeconds);
        token.setExpirationTime(new DateTime()
            .plusSeconds(expirationSeconds));
        refreshTokenDao.updateToken(token);
        logger.debug("done resetting refresh token expiration: {}", token);
    }
    
    // private
    private String generateTokenWithDcPrefix() {
        try {
            String tokenString = String.format("%s-%s", 
                this.dataCenterPrefix, HashHelper.getRandomSha1());
            return tokenString;
        } catch (NoSuchAlgorithmException e) {
            String error = "Unsupported hashing alogrithm in create Token for User";
            logger.error(error, e);
            throw new IllegalStateException(error, e);
        }
    }
}
