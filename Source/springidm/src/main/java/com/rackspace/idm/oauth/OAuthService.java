package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {

    int getCloudAuthDefaultTokenExpirationSeconds();



    RefreshToken createRefreshTokenForUser(String username, String clientId);

    RefreshToken getRefreshTokenByUserAndClient(String username, String clientId, DateTime validAfter);

    void resetTokenExpiration(RefreshToken token);


    //TODO Move this to OAuthService
    void revokeToken(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
    AuthData getTokens(OAuthGrantType grantType, BaseUser user, BaseClient client, String refreshTokenStr, int expirationSeconds, DateTime currentTime);


    //TODO Move this to AccessTokenService?
    boolean authenticateAccessToken(String accessTokenStr);

    AccessToken getTokenByUsernameAndApiCredentials(BaseClient client, String username, String apiKey,
                                                    int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByNastIdAndApiCredentials(BaseClient client, String nastId, String apiKey,
                                                  int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client, int mossoId, String apiKey, int expirationSeconds,
                                                   DateTime currentTime);

    AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
                                           DateTime currentTime);

    RefreshToken getRefreshTokenForUser(BaseUser user, BaseClient client, DateTime currentTime);

    AccessToken getTokenByRefreshToken(String refreshTokenStr, int expirationSeconds, DateTime currentTime);
}
