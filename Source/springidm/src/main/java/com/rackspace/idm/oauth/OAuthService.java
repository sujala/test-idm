package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.AuthData;
import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {
    AuthData getTokens(OAuthGrantType grantType,
                       AuthCredentials credentials, int expirationSeconds, DateTime currentTime);

    boolean authenticateToken(String token);
}
