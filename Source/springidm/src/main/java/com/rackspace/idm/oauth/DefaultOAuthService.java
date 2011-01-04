package com.rackspace.idm.oauth;

import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private ClientService clientService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private AuthHeaderHelper authHeaderHelper;
    private Logger logger;

    public DefaultOAuthService(UserService userService,
        ClientService clientService, AccessTokenService accessTokenService,
        RefreshTokenService refreshTokenService,
        AuthHeaderHelper authHeaderHelper, Logger logger) {

        this.userService = userService;
        this.clientService = clientService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authHeaderHelper = authHeaderHelper;
        this.logger = logger;
    }

    public boolean authenticateAuthHeader(String authHeader) {
        logger.debug("Authorizing Header: {}", authHeader);
        Boolean authenticated = false;
        try {

            Map<String, String> authParams = authHeaderHelper
                .parseTokenParams(authHeader);
            AuthFlowType authType = getAuthTypeFromHeader(authHeader);
            if (AuthFlowType.oauth == authType) {
                String token = authParams.get("token");
                authenticated = this.authenticateToken(token);
            } else {
                authenticated = false;
            }

        } catch (Exception ex) {
            logger.error("Encountered an error during authentication.", ex);
            authenticated = false;
        }
        logger.debug("Authorized Header: {} : {}", authHeader, authenticated);
        return authenticated;
    }

    public boolean authenticateClient(String clientKey, String clientSecret) {
        logger.debug("Authorizing Client: {}", clientKey);
        boolean authenticated = clientService.authenticate(clientKey,
            clientSecret);
        logger.debug("Authorized Client: {} : {}", clientKey, authenticated);
        return authenticated;
    }

    public boolean authenticateUser(String username, String password) {
        boolean authenticated = userService.authenticate(username, password);
        logger.debug("Authorized User: {} : {}", username, authenticated);

        return authenticated;
    }

    public boolean authenticateUserApiKey(String username, String apiKey) {
        logger.debug("Authorising User: {} by API Key", username);
        boolean authenticated = userService.authenticateWithApiKey(username,
            apiKey);
        logger.debug("Authorized User: {} by API Key - {}", username,
            authenticated);
        return authenticated;
    }

    public boolean authenticateToken(String token) {
        logger.debug("Authorizing Token: {}", token);
        Boolean authenticated = false;

        // check token is valid and not expired
        AccessToken accessToken = accessTokenService
            .getTokenByTokenString(token);
        if (accessToken != null && !accessToken.isExpired(new DateTime())) {
            authenticated = true;
        }
        logger.debug("Authorized Token: {} : {}", token, authenticated);
        return authenticated;
    }

    public AuthFlowType getAuthTypeFromHeader(String authHeader) {
        logger.debug("Getting AuthType From Header: {}", authHeader);
        String authType = authHeader.substring(0, authHeader.indexOf(' '))
            .toLowerCase();
        return AuthFlowType.valueOf(authType);
    }
    
    public AccessToken getTokenFromAuthHeader(String authHeader) {
        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        AccessToken accessToken = accessTokenService
            .getTokenByTokenString(tokenString);
        return accessToken;
    }

    public String getUsernameFromAuthHeaderToken(String authHeader) {

        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        String username = accessTokenService
            .getUsernameByTokenString(tokenString);

        return username;
    }

    public String getCustomerIdFromAuthHeaderToken(String authHeader) {
        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        String customerId = accessTokenService
            .getCustomerIdByTokenString(tokenString);

        return customerId;
    }

    public String getClientIdFromAuthHeaderToken(String authHeader) {
        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        String clientId = accessTokenService
            .getClientIdByTokenString(tokenString);

        return clientId;
    }

    public AuthData getTokens(OAuthGrantType grantType,
        AuthCredentials credentials, int expirationSeconds, DateTime currentTime) {

        String clientId = credentials.getClientId();
        String username = credentials.getUsername();
        String userpasswd = credentials.getPassword();
        String refreshTokenStr = credentials.getRefreshToken();

        AccessToken accessToken = null;
        RefreshToken refreshToken = null;

        switch (grantType) {
            case API_CREDENTIALS:
                accessToken = getTokenByApiCredentials(clientId, username,
                    userpasswd, expirationSeconds, currentTime);
                refreshToken = getRefreshTokenForUser(username, clientId,
                    currentTime);
                break;

            case PASSWORD:
                accessToken = getTokenByBasicCredentials(clientId, username,
                    userpasswd, expirationSeconds, currentTime);
                refreshToken = getRefreshTokenForUser(username, clientId,
                    currentTime);
                break;

            case REFRESH_TOKEN:
                accessToken = getTokenByRefreshToken(refreshTokenStr,
                    expirationSeconds, currentTime);
                refreshToken = refreshTokenService
                    .getRefreshTokenByTokenString(credentials.getRefreshToken());
                break;

            case NONE:
                accessToken = getTokenByNoCredentials(clientId,
                    expirationSeconds, currentTime);
                break;

            default:
                throwNotAuthenticatedException(String.format(
                    "Unsupported GrantType: %s", grantType));
        }

        AuthData authData = new AuthData(accessToken, refreshToken);

        return authData;
    }

    // private funcs
    private AccessToken getTokenByApiCredentials(String clientId,
        String username, String userpasswd, int expirationSeconds,
        DateTime currentTime) {

        if (!clientId.equals(GlobalConstants.RESTRICTED_CLIENT_ID)) {
            throwNotAuthenticatedException(String.format(
                "Unauthorized Client For: %s", clientId));
        }

        if (!this.authenticateUserApiKey(username, userpasswd)) {
            throwNotAuthenticatedException(String.format(
                "Incorrect Credentials For: %s", username));
        }

        AccessToken token = accessTokenService.getAccessTokenForUser(username,
            clientId, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = accessTokenService.createAccessTokenForUser(username,
                clientId, expirationSeconds);
            logger.debug("Access Token Created For User: {} : {}", username,
                token);
        } else {
            logger.debug("Access Token Found For User: {} by Client : {}",
                username, token);
        }
        return token;
    }

    private AccessToken getTokenByBasicCredentials(String clientId,
        String username, String userpasswd, int expirationSeconds,
        DateTime currentTime) {

        if (!this.authenticateUser(username, userpasswd)) {
            throwNotAuthenticatedException(String.format(
                "User failed authentication: %s", username));
        }

        AccessToken token = accessTokenService.getAccessTokenForUser(username,
            clientId, currentTime);

        if (token == null || token.isExpired(currentTime)) {
            token = accessTokenService.createAccessTokenForUser(username,
                clientId, expirationSeconds);

            logger.debug(String.format(
                "Access Token Created For User: %s : %s", username, token));
        } else {
            logger.debug(String.format(
                "Access Token Found For User: {} by Client : {}", username,
                token));
        }
        return token;
    }

    private AccessToken getTokenByNoCredentials(String clientId,
        int expirationSeconds, DateTime currentTime) {

        AccessToken token = accessTokenService.getAccessTokenForClient(
            clientId, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = accessTokenService.createAccessTokenForClient(clientId,
                expirationSeconds);
            logger.debug("Access Token Created for Client: {} : {}", clientId,
                token);
        } else {
            logger.debug("Access Token Found for Client: {} : {}", clientId,
                token);
        }
        return token;
    }

    private AccessToken getTokenByRefreshToken(String refreshTokenString,
        int expirationSeconds, DateTime currentTime) {

        RefreshToken refreshToken = refreshTokenService
            .getRefreshTokenByTokenString(refreshTokenString);

        if (refreshToken == null || refreshToken.isExpired(currentTime)) {
            throwNotAuthenticatedException(String.format(
                "Unauthorized Refresh Token: %s", refreshTokenString));
        }

        refreshTokenService.resetTokenExpiration(refreshToken);

        String owner = refreshToken.getOwner();
        String requestor = refreshToken.getRequestor();

        AccessToken token = accessTokenService.createAccessTokenForUser(owner,
                requestor, expirationSeconds);
        
        return token;
    }

    private RefreshToken getRefreshTokenForUser(String username,
        String clientId, DateTime currentTime) {

        RefreshToken refreshToken = refreshTokenService
            .getRefreshTokenByUserAndClient(username, clientId, currentTime);

        if (refreshToken == null) {
            refreshToken = refreshTokenService.createRefreshTokenForUser(
                username, clientId);
        } else {
            refreshTokenService.resetTokenExpiration(refreshToken);
        }

        return refreshToken;
    }

    private void throwNotAuthenticatedException(String errorMsg) {
        logger.error(errorMsg);
        throw new NotAuthenticatedException(errorMsg);
    }
}
