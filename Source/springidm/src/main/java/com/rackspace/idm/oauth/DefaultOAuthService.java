package com.rackspace.idm.oauth;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.rackspace.idm.oauth.OAuthGrantType.*;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private ClientService clientService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private Logger logger;

    @Deprecated
    public DefaultOAuthService(UserService userService, AccessTokenService accessTokenService,
                               RefreshTokenService refreshTokenService, Logger logger) {

        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.logger = logger;
    }

    public DefaultOAuthService(UserService userService, ClientService clientService,
                               AccessTokenService accessTokenService, RefreshTokenService refreshTokenService,
                               Logger logger) {

        this.userService = userService;
        this.clientService = clientService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.logger = logger;
    }

    @Override
    @Deprecated
    public AuthData getTokensDeprecated(OAuthGrantType grantType, AuthCredentials trParam, int expirationSeconds,
                                        DateTime currentTime) {
        /*AccessToken accessToken = null;
        RefreshToken refreshToken = null;

        switch (grantType) {
            case PASSWORD:
                accessToken = accessTokenService.getTokenByBasicCredentials(client, user, expirationSeconds,
                        currentTime);
                refreshToken = getRefreshTokenForUser(user.getUsername(), client.getClientId(), currentTime);
                break;

            case REFRESH_TOKEN:
                accessToken = getTokenByRefreshToken(trParam.getRefreshToken(), expirationSeconds, currentTime);
                refreshToken = refreshTokenService.getRefreshTokenByTokenString(trParam.getRefreshToken());
                break;

            case NONE:
                accessToken = getTokenByNoCredentials(client, expirationSeconds, currentTime);
                break;

            default:
                throwNotAuthenticatedException(String.format("Unsupported GrantType: %s", grantType));
        }

        return new AuthData(accessToken, refreshToken);*/
        throw new NotImplementedException("Method to be removed");
    }

    @Override
    public AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException {
        int expirationSeconds = accessTokenService.getDefaultTokenExpirationSeconds();

        ClientAuthenticationResult caResult =
                clientService.authenticate(trParam.getClientId(), trParam.getClientSecret());
        if (!caResult.isAuthenticated()) {
            throw new NotAuthenticatedException("Bad Client credentials.");
        }

        AccessToken accessToken = null;
        RefreshToken refreshToken = null;
        if (PASSWORD == grantType) {
            UserAuthenticationResult uaResult = userService.authenticate(trParam.getUsername(), trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                throw new NotAuthenticatedException("Bad User credentials.");
            }

            accessToken = accessTokenService
                    .getTokenByBasicCredentials(caResult.getClient(), uaResult.getUser(), expirationSeconds,
                            currentTime);
            refreshToken = getRefreshTokenForUser(uaResult.getUser().getUsername(), caResult.getClient().getClientId(),
                    currentTime);
            return new AuthData(accessToken, refreshToken);
        }

        if (REFRESH_TOKEN == grantType) {
            return getTokenByRefreshToken(trParam.getRefreshToken(), expirationSeconds, currentTime);
        }

        if (NONE == grantType) {
            accessToken = getTokenByNoCredentials(caResult.getClient(), expirationSeconds, currentTime);
            return new AuthData(accessToken, refreshToken);
        }

        throwNotAuthenticatedException(String.format("Unsupported GrantType: %s", grantType));

        // The execution never gets here since the line above throws an exception. But the compiler no likey.
        return null;
    }

    /*@Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        AccessToken accessToken = accessTokenService
            .getAccessTokenByTokenString(accessTokenStr);
        if (accessToken != null && !accessToken.isExpired(new DateTime())) {
            authenticated = true;
        }
        logger.debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }*/

    /* @Override
    public AuthData getTokens(OAuthGrantType grantType,
        AuthCredentials credentials, int expirationSeconds, DateTime currentTime) {

        String clientId = credentials.getClientId();
        String username = credentials.getUsername();
        String userpasswd = credentials.getPassword();
        String refreshTokenStr = credentials.getRefreshToken();

        AccessToken accessToken = null;
        RefreshToken refreshToken = null;

        switch (grantType) {
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

        return new AuthData(accessToken, refreshToken);
    }*/

    @Override
    public void revokeToken(String tokenStringRequestingDelete, String tokenToDelete) {

        logger.info("Deleting Token {}", tokenToDelete);

        AccessToken deletingToken = accessTokenService.getAccessTokenByTokenString(tokenToDelete);
        if (deletingToken == null) {
            String error = "No entry found for token " + deletingToken;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        AccessToken requestingToken = accessTokenService.getAccessTokenByTokenString(tokenStringRequestingDelete);
        if (requestingToken == null) {
            String error = "No entry found for token " + tokenStringRequestingDelete;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        // Only CustomerIdm Client and Client that got token or the user of the
        // toke are authorized to revoke token

        boolean isCustomerIdm = requestingToken.isClientToken() &&
                requestingToken.getTokenClient().getClientId().equals(GlobalConstants.IDM_CLIENT_ID);

        boolean isRequestor = requestingToken.isClientToken() &&
                requestingToken.getTokenClient().getClientId().equals(deletingToken.getTokenClient().getClientId());

        boolean isOwner = requestingToken.getTokenUser() != null && deletingToken.getTokenUser() != null &&
                requestingToken.getTokenUser().getUsername().equals(deletingToken.getTokenUser().getUsername());

        boolean authorized = isCustomerIdm || isRequestor || isOwner;

        if (!authorized) {
            String errMsg =
                    String.format("Requesting token %s not authorized to revoke token %s", tokenStringRequestingDelete,
                            tokenToDelete);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (deletingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor", deletingToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        deleteRefreshTokenByAccessToken(deletingToken);
        accessTokenService.delete(deletingToken.getTokenString());
        logger.info("Deleted Token {}", deletingToken);
    }

    private void deleteRefreshTokenByAccessToken(AccessToken accessToken) {
        // Clients do not get Refresh Tokens so there is no need to
        // revoke one if the access token is for a client
        if (accessToken.isClientToken()) {
            return;
        }

        String username = StringUtils.EMPTY;
        String clientId = StringUtils.EMPTY;

        //TODO Look for inefficiencies here
        User tokenUser = userService.getUser(accessToken.getOwner());
        if (tokenUser != null) {
            username = tokenUser.getUsername();
        }
        Client tokenClient = clientService.getById(accessToken.getRequestor());
        if (tokenClient != null) {
            clientId = tokenClient.getClientId();
        }
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(clientId)) {
            Set<String> tokenRequestors = new HashSet<String>();
            tokenRequestors.add(clientId);
            refreshTokenService.deleteAllTokensForUser(username, tokenRequestors);
        }
    }
/*
    public AccessToken getTokenByUsernameAndApiCredentials(BaseClient client,
        String username, String apiKey, int expirationSeconds,
        DateTime currentTime) {

        UserAuthenticationResult authResult = userService
            .authenticateWithApiKey(username, apiKey);

        return getTokenByApiCredentials(client,authResult, expirationSeconds, currentTime);
    }
    
    public AccessToken getTokenByNastIdAndApiCredentials(BaseClient client,
        String nastId, String apiKey, int expirationSeconds,
        DateTime currentTime) {

        UserAuthenticationResult authResult = userService
            .authenticateWithNastIdAndApiKey(nastId, apiKey);

        return getTokenByApiCredentials(client,authResult, expirationSeconds, currentTime);
    }
    
    public AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client,
        int mossoId, String apiKey, int expirationSeconds,
        DateTime currentTime) {

        UserAuthenticationResult authResult = userService
            .authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        return getTokenByApiCredentials(client,authResult, expirationSeconds, currentTime);
    }*/

/*
    private AccessToken getTokenByApiCredentials(BaseClient client,
        UserAuthenticationResult authResult, int expirationSeconds,
        DateTime currentTime) {

        if (!authResult.isAuthenticated()) {
            throwNotAuthenticatedException("Incorrect Credentials");
        }
        
        String username = authResult.getUser().getUsername();
        String clientId = client.getClientId();

        AccessToken token = accessTokenService.getAccessTokenForUser(username,
            client, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = accessTokenService.createAccessTokenForUser(username,
                client, expirationSeconds);
            logger.debug("Access Token Created For User: {} : {}", username,
                token);
        } else {
            logger.debug("Access Token Found For User: {} by Client : {}",
                username, token);
        }
        return token;
    }*/


/*    private AccessToken getTokenByBasicCredentials(String clientId,
    String username, String userpasswd, int expirationSeconds,
    DateTime currentTime) {

    boolean authenticated = userService.authenticateDeprecated(username, userpasswd);
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
}*/

    private AccessToken getTokenByNoCredentials(Client client, int expirationSeconds, DateTime currentTime) {
        AccessToken token = accessTokenService.getAccessTokenForClient(client, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = accessTokenService.createAccessTokenForClient(client, expirationSeconds);
            logger.debug("Access Token Created for Client: {} : {}", client.getClientId(), token);
        } else {
            logger.debug("Access Token Found for Client: {} : {}", client.getClientId(), token);
        }
        return token;
    }

    private AuthData getTokenByRefreshToken(String refreshTokenString, int expirationSeconds, DateTime currentTime) {
        RefreshToken refreshToken = refreshTokenService.getRefreshTokenByTokenString(refreshTokenString);
        if (refreshToken == null || refreshToken.isExpired(currentTime)) {
            throwNotAuthenticatedException(String.format("Unauthorized Refresh Token: %s", refreshTokenString));
        }
        refreshTokenService.resetTokenExpiration(refreshToken);
        String username = refreshToken.getOwner();
        String clientId = refreshToken.getRequestor();
        AccessToken token = accessTokenService.createAccessTokenForUser(username, clientId, expirationSeconds);
        return new AuthData(token, refreshToken);
    }

    private RefreshToken getRefreshTokenForUser(String username, String clientId, DateTime currentTime) {

        RefreshToken refreshToken = refreshTokenService.getRefreshTokenByUserAndClient(username, clientId, currentTime);

        if (refreshToken == null) {
            refreshToken = refreshTokenService.createRefreshTokenForUser(username, clientId);
        } else {
            refreshTokenService.resetTokenExpiration(refreshToken);
        }

        return refreshToken;
    }

    private void throwNotAuthenticatedException(String errorMsg) throws NotAuthenticatedException{
        logger.error(errorMsg);
        throw new NotAuthenticatedException(errorMsg);
    }
}
