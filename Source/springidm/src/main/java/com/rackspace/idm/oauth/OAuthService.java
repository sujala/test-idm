package com.rackspace.idm.oauth;

import org.joda.time.DateTime;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;

public interface OAuthService {

    boolean authenticateAuthHeader(String authHeader);

    boolean authenticateClient(String clientId, String clientSecret);

    boolean authenticateUser(String username, String password);

    boolean authenticateUserApiKey(String username, String apiKey);

    boolean authenticateToken(String token);

    AuthFlowType getAuthTypeFromHeader(String authHeader);
    
    AccessToken getTokenFromAuthHeader(String authHeader);

    String getUsernameFromAuthHeaderToken(String authHeader);

    String getClientIdFromAuthHeaderToken(String authHeader);

    String getCustomerIdFromAuthHeaderToken(String authHeader);

    AuthData getTokens(OAuthGrantType grantType, 
        AuthCredentials credentials, int expirationSeconds, DateTime currentTime);
}
