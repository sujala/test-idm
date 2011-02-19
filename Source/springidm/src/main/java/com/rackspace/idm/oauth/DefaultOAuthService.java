package com.rackspace.idm.oauth;

import static com.rackspace.idm.oauth.OAuthGrantType.NONE;
import static com.rackspace.idm.oauth.OAuthGrantType.PASSWORD;
import static com.rackspace.idm.oauth.OAuthGrantType.REFRESH_TOKEN;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientAuthenticationResult;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserAuthenticationResult;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private ClientService clientService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private Logger logger;
    private Configuration config;

    public DefaultOAuthService(UserService userService, ClientService clientService,
        AccessTokenService accessTokenService, RefreshTokenService refreshTokenService, Configuration config,
        Logger logger) {

        this.userService = userService;
        this.clientService = clientService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.logger = logger;
        this.config = config;
    }

    @Override
    public AccessToken getAccessTokenByAuthHeader(String authHeader) {
        return accessTokenService.getAccessTokenByAuthHeader(authHeader);
    }

    @Override
    public AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime)
        throws NotAuthenticatedException {
        int expirationSeconds = accessTokenService.getDefaultTokenExpirationSeconds();

        ClientAuthenticationResult caResult = clientService.authenticate(trParam.getClientId(),
            trParam.getClientSecret());
        if (!caResult.isAuthenticated()) {
            throw new NotAuthenticatedException("Bad Client credentials.");
        }

        AccessToken accessToken = null;
        RefreshToken refreshToken = null;
        if (PASSWORD == grantType) {
            UserAuthenticationResult uaResult = userService.authenticate(trParam.getUsername(),
                trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                throw new NotAuthenticatedException("Bad User credentials.");
            }

            accessToken = accessTokenService.getTokenByBasicCredentials(caResult.getClient(),
                uaResult.getUser(), expirationSeconds, currentTime);
            refreshToken = getRefreshTokenForUser(uaResult.getUser().getUsername(), caResult.getClient()
                .getClientId(), currentTime);
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

        // The execution never gets here since the line above throws an
        // exception. But the compiler no likey.
        return null;
    }

    public void revokeTokensLocally(String tokenStringRequestingDelete, String tokenToDelete) {
        revokeToken(tokenStringRequestingDelete, tokenToDelete, false);
    }

    @Override
    public void revokeTokensGlobally(String tokenStringRequestingDelete, String tokenToDelete) {
        revokeToken(tokenStringRequestingDelete, tokenToDelete, true);
    }

    @Override
    public void revokeTokensLocallyForOwner(String authTokenString, String ownerId) {
        AccessToken requestingToken = accessTokenService.getAccessTokenByTokenString(authTokenString);
        if (requestingToken == null) {
            String error = "No entry found for token " + requestingToken;
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        boolean isAuthorized = isAuthorizedAsCustomerIdm(requestingToken);

        if (!isAuthorized) {
            String errMsg = String.format("Requesting token %s not authorized to revoke token for owner %s.",
                authTokenString, ownerId);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (requestingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor",
                requestingToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        accessTokenService.deleteAllForOwner(ownerId);
        logger.debug("Deleted all access tokens for owner {}.", ownerId);
    }

    @Override
    public void revokeTokensGloballyForOwner(String ownerId) {
        refreshTokenService.deleteAllTokensForUser(ownerId);
        accessTokenService.deleteAllGloballyForOwner(ownerId);
        logger.debug("Deleted all access tokens for owner {}.", ownerId);
    }

    @Override
    public void revokeTokensLocallyForCustomer(String authTokenString, String customerId) {
        AccessToken requestingToken = accessTokenService.getAccessTokenByTokenString(authTokenString);
        if (requestingToken == null) {
            String error = "No entry found for token " + requestingToken;
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        boolean isAuthorized = isAuthorizedAsCustomerIdm(requestingToken);

        if (!isAuthorized) {
            String errMsg = String.format(
                "Requesting token %s not authorized to revoke token for customer %s.", authTokenString,
                customerId);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (requestingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor",
                requestingToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        for (User user : userService.getByCustomerId(customerId, 0, -1).getUsers()) {
            accessTokenService.deleteAllForOwner(user.getUsername());
        }
        logger.debug("Deleted all access tokens for customer {}.", customerId);

    }

    @Override
    public void revokeTokensGloballyForCustomer(String customerId) {
        List<User> users = userService.getByCustomerId(customerId, 0, -1).getUsers();
        for (User user : users) {
            refreshTokenService.deleteAllTokensForUser(user.getUsername());
        }

        accessTokenService.deleteAllGloballyForCustomer(customerId, users);
        logger.debug("Deleted all access tokens for customer {}.", customerId);
    }

    private void revokeToken(String tokenStringRequestingDelete, String tokenToDelete, boolean isGlobal)
        throws NotAuthorizedException {
        logger.info("Deleting Token {}", tokenToDelete);

        AccessToken deletingToken = accessTokenService.getAccessTokenByTokenString(tokenToDelete);
        if (deletingToken == null) {
            String error = "No entry found for token " + deletingToken;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        AccessToken requestingToken = accessTokenService
            .getAccessTokenByTokenString(tokenStringRequestingDelete);
        if (requestingToken == null) {
            String error = "No entry found for token " + tokenStringRequestingDelete;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        boolean isGoodAsIdm = isAuthorizedAsCustomerIdm(requestingToken);
        boolean isAuthorized = false;
        if (isGlobal) {
            // Only CustomerIdm Client and Client that got token or the user of
            // the token are authorized to revoke token
            isAuthorized = isGoodAsIdm || isAuthorizedAsRequestorOrOwner(deletingToken, requestingToken);
        } else {
            // Only Customer IDM can make a non-global token revocation call.
            isAuthorized = isGoodAsIdm;
        }

        if (!isAuthorized) {
            String errMsg;
            if (isGlobal) {
                errMsg = String.format("Requesting token %s not authorized to revoke token %s.",
                    tokenStringRequestingDelete, tokenToDelete);
            } else {
                errMsg = String.format("Requesting token %s not authorized to revoke token %s locally.",
                    tokenStringRequestingDelete, tokenToDelete);
            }
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (deletingToken.getRequestor() == null) {
            String error = String
                .format("Token %s does not have a requestor", deletingToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        if (isGlobal) {
            // This user is to be completely logged out of the system.
            deleteRefreshTokenByAccessToken(deletingToken);
            accessTokenService.deleteAllGloballyForOwner(deletingToken.getOwner());
        } else {
            accessTokenService.delete(deletingToken.getTokenString());
        }
        logger.info("Deleted Token {}", deletingToken);
    }

    private boolean isAuthorizedAsRequestorOrOwner(AccessToken deletingToken, AccessToken requestingToken) {
        boolean isRequestor = requestingToken.isClientToken()
            && requestingToken.getTokenClient().getClientId()
                .equals(deletingToken.getTokenClient().getClientId());

        boolean isOwner = requestingToken.getTokenUser() != null
            && deletingToken.getTokenUser() != null
            && requestingToken.getTokenUser().getUsername()
                .equals(deletingToken.getTokenUser().getUsername());

        boolean authorized = isRequestor || isOwner;
        return authorized;
    }

    private boolean isAuthorizedAsCustomerIdm(AccessToken requestingToken) {
        boolean isCustomerIdm = requestingToken.isClientToken()
            && requestingToken.getTokenClient().getClientId().equals(getIdmClientId());
        return isCustomerIdm;
    }

    private void deleteRefreshTokenByAccessToken(AccessToken accessToken) {
        // Clients do not get Refresh Tokens so there is no need to
        // revoke one if the access token is for a client
        if (accessToken.isClientToken()) {
            return;
        }
        refreshTokenService.deleteTokenForUserByClientId(accessToken.getOwner(), accessToken.getRequestor());
    }

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

    private AuthData getTokenByRefreshToken(String refreshTokenString, int expirationSeconds,
        DateTime currentTime) {
        RefreshToken refreshToken = refreshTokenService.getRefreshTokenByTokenString(refreshTokenString);
        if (refreshToken == null || refreshToken.isExpired(currentTime)) {
            throwNotAuthenticatedException(String
                .format("Unauthorized Refresh Token: %s", refreshTokenString));
        }
        refreshTokenService.resetTokenExpiration(refreshToken);
        String username = refreshToken.getOwner();
        String clientId = refreshToken.getRequestor();
        AccessToken token = accessTokenService
            .createAccessTokenForUser(username, clientId, expirationSeconds);
        return new AuthData(token, refreshToken);
    }

    private RefreshToken getRefreshTokenForUser(String username, String clientId, DateTime currentTime) {
        RefreshToken refreshToken = refreshTokenService.getRefreshTokenByUserAndClient(username, clientId,
            currentTime);
        if (refreshToken == null) {
            refreshToken = refreshTokenService.createRefreshTokenForUser(username, clientId);
        } else {
            refreshTokenService.resetTokenExpiration(refreshToken);
        }

        return refreshToken;
    }

    private void throwNotAuthenticatedException(String errorMsg) throws NotAuthenticatedException {
        logger.error(errorMsg);
        throw new NotAuthenticatedException(errorMsg);
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }
}
