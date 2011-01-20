package com.rackspace.idm.oauth;

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

    AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;

    void revokeToken(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
}
