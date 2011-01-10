package com.rackspace.idm.oauth;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;
import org.joda.time.DateTime;
import org.slf4j.Logger;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private Logger logger;

    public DefaultOAuthService(UserService userService, AccessTokenService accessTokenService,
        RefreshTokenService refreshTokenService, Logger logger) {

        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.logger = logger;
    }

    @Override
    public boolean authenticateToken(String token) {
        logger.debug("Authorizing Token: {}", token);
        Boolean authenticated = false;

        // check token is valid and not expired
        AccessToken accessToken = accessTokenService
            .getAccessTokenByTokenString(token);
        if (accessToken != null && !accessToken.isExpired(new DateTime())) {
            authenticated = true;
        }
        logger.debug("Authorized Token: {} : {}", token, authenticated);
        return authenticated;
    }

    @Override
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

        boolean authenticated = userService.authenticateWithApiKey(username,
            userpasswd);
        if (!authenticated) {
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

        boolean authenticated = userService.authenticate(username, userpasswd);
        if (!authenticated) {
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
