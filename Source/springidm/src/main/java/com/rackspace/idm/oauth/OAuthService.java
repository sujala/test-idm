package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {

    AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;

    void revokeToken(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;


    @Deprecated
    AuthData getTokensDeprecated(OAuthGrantType grantType, AuthCredentials trParam, int expirationSeconds,
                                 DateTime currentTime);


    //TODO Move this to AccessTokenService?
    /*boolean authenticateAccessToken(String accessTokenStr);

    AccessToken getTokenByUsernameAndApiCredentials(BaseClient client, String username, String apiKey,
                                                    int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByNastIdAndApiCredentials(BaseClient client, String nastId, String apiKey,
                                                  int expirationSeconds, DateTime currentTime);

    AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client, int mossoId, String apiKey, int expirationSeconds,
                                                   DateTime currentTime);

    AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
                                           DateTime currentTime);
*/
    //RefreshToken getRefreshTokenForUser(BaseUser user, BaseClient client, DateTime currentTime);
}
