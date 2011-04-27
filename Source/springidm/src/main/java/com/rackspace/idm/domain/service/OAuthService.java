package com.rackspace.idm.domain.service;

import org.joda.time.DateTime;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.exception.NotAuthenticatedException;

public interface OAuthService {
    ScopeAccessObject getAccessTokenByAuthHeader(String authHeader);

    OAuthGrantType getGrantType(String grantTypeString);

    ScopeAccessObject getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;
    
    void revokeAccessToken(String tokenStringRequestingDelete, String tokenToDelete);
    
    void revokeAllTokensForClient(String clientId);
    
    void revokeAllTokensForCustomer(String customerId);

    void revokeAllTokensForUser(String username);

    ApiError validateGrantType(AuthCredentials trParam, OAuthGrantType grantType);
}
