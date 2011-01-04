package com.rackspace.idm.services;

import org.joda.time.DateTime;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.TokenStatusCode;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.oauthAuthentication.OauthTokenService;

public interface AccessTokenService extends OauthTokenService {

    AccessToken getTokenByTokenString(String tokenString);

    AccessToken getAccessTokenForUser(String username, String clientId,
        DateTime expiresAfter);

    AccessToken getAccessTokenForClient(String clientId, DateTime expiresAfter);

    AccessToken createAccessTokenForUser(String username, String clientId);

    AccessToken createAccessTokenForUser(String username, String clientId,
        int expirationSeconds);
    
    AccessToken createPasswordResetAccessTokenForUser(String username, String clientId);
    
    AccessToken createPasswordResetAccessTokenForUser(String username, String clientId,
        int expirationTimeInSeconds);

    AccessToken createAccessTokenForClient(String clientId);

    AccessToken createAccessTokenForClient(String clientId, int expirationSeconds);

    String getUsernameByTokenString(String tokenString);
    
    String getClientIdByTokenString(String tokenString);

    String getCustomerIdByTokenString(String tokenString);
    
    int getDefaultTokenExpirationSeconds();
    
    int getMaxTokenExpirationSeconds();
    
    int getMinTokenExpirationSeconds();

    void revokeToken(String tokenStringRequestingDelete, String tokenToDelete)
        throws NotAuthorizedException;

    TokenStatusCode authenticateToken(String tokenString);
    
    AccessToken validateToken(String tokenString);
}
