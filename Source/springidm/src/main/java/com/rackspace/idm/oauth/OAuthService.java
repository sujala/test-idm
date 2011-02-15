package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {
    AccessToken getAccessTokenByAuthHeader(String authHeader);

    AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;

    void revokeTokensLocally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
    
    void revokeTokensGlobally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;

    /**
     * Only other IDM instances are allowed to call this method.
     * 
     * @param authTokenString
     * @param ownerId
     */
    void revokeTokensLocallyForOwner(String authTokenString, String ownerId);
    
    void revokeTokensGloballyForOwner(String authTokenString, String ownerId);
}
