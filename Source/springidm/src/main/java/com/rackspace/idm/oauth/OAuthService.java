package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.BaseClient;

import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {
    AuthData getTokens(OAuthGrantType grantType, AuthCredentials credentials,
        int expirationSeconds, DateTime currentTime);

    boolean authenticateToken(String token);

    AccessToken getTokenByUsernameAndApiCredentials(BaseClient client,
        String username, String apiKey, int expirationSeconds,
        DateTime currentTime);

    AccessToken getTokenByNastIdAndApiCredentials(BaseClient client,
        String nastId, String apiKey, int expirationSeconds,
        DateTime currentTime);

    AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client,
        int mossoId, String apiKey, int expirationSeconds, DateTime currentTime);
}
