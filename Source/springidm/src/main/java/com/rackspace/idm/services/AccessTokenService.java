package com.rackspace.idm.services;

import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

public interface AccessTokenService {

    boolean authenticateAccessToken(String accessTokenStr);

    AccessToken getTokenByUsernameAndApiCredentials(BaseClient client, String username, String apiKey,
                                                    int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByNastIdAndApiCredentials(BaseClient client, String nastId, String apiKey,
                                                  int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client, int mossoId, String apiKey, int expirationSeconds,
                                                   DateTime currentTime);

    AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
                                           DateTime currentTime);


    AccessToken getAccessTokenByAuthHeader(String authHeader);

    AccessToken getAccessTokenByTokenString(String tokenString);

    AccessToken getAccessTokenForUser(BaseUser user, BaseClient client, DateTime expiresAfter);

    AccessToken getAccessTokenForClient(BaseClient client, DateTime expiresAfter);

    AccessToken createAccessTokenForUser(BaseUser user, BaseClient client, int expirationSeconds);

    AccessToken createPasswordResetAccessTokenForUser(User user, String clientId);

    AccessToken createPasswordResetAccessTokenForUser(User user, String clientId, int expirationTimeInSeconds);

    AccessToken createAccessTokenForClient(BaseClient client);

    AccessToken createAccessTokenForClient(BaseClient client, int expirationSeconds);

    AccessToken createAccessTokenForUser(String username, String clientId, int expirationSeconds);

    @Deprecated
    AccessToken getAccessTokenForUser(String username, String clientId, DateTime expiresAfter);

    @Deprecated
    AccessToken getAccessTokenForClient(String clientId, DateTime expiresAfter);

    @Deprecated
    AccessToken createAccessTokenForUser(String username, String clientId);

    @Deprecated
    AccessToken createPasswordResetAccessTokenForUser(String username, String clientId);

    @Deprecated
    AccessToken createPasswordResetAccessTokenForUser(String username, String clientId, int expirationTimeInSeconds);

    @Deprecated
    AccessToken createAccessTokenForClient(String clientId);

    @Deprecated
    AccessToken createAccessTokenForClient(String clientId, int expirationSeconds);

    int getDefaultTokenExpirationSeconds();

    int getCloudAuthDefaultTokenExpirationSeconds();

    AccessToken validateToken(String tokenString);

    void delete(String tokenString);
}
