package com.rackspace.idm.services;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

public interface AccessTokenService {
    AccessToken getAccessTokenByAuthHeader(String authHeader);

    AccessToken getAccessTokenByTokenString(String tokenString);

    AccessToken getAccessTokenForUser(String username, String clientId,
        DateTime expiresAfter);

    AccessToken getAccessTokenForClient(String clientId, DateTime expiresAfter);

    AccessToken createAccessTokenForUser(String username, String clientId);

    AccessToken createAccessTokenForUser(String username, String clientId,
        int expirationSeconds);

    AccessToken createPasswordResetAccessTokenForUser(String username,
        String clientId);

    AccessToken createPasswordResetAccessTokenForUser(String username,
        String clientId, int expirationTimeInSeconds);

    AccessToken createAccessTokenForClient(String clientId);

    AccessToken createAccessTokenForClient(String clientId,
        int expirationSeconds);

    int getDefaultTokenExpirationSeconds();

    int getCloudAuthDefaultTokenExpirationSeconds();

    void revokeToken(String tokenStringRequestingDelete, String tokenToDelete)
        throws NotAuthorizedException;

    AccessToken validateToken(String tokenString);
}
