package com.rackspace.idm.oauth;

import static com.rackspace.idm.oauth.OAuthGrantType.NONE;
import static com.rackspace.idm.oauth.OAuthGrantType.PASSWORD;
import static com.rackspace.idm.oauth.OAuthGrantType.REFRESH_TOKEN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientAuthenticationResult;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserAuthenticationResult;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private ClientService clientService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private AuthorizationService authorizationService;
    private Logger logger;
    private Configuration config;

    public DefaultOAuthService(UserService userService, ClientService clientService,
        AccessTokenService accessTokenService, RefreshTokenService refreshTokenService,
        AuthorizationService authorizationService, Configuration config, Logger logger) {
        this.userService = userService;
        this.clientService = clientService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authorizationService = authorizationService;
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

        boolean isAuthorized = authorizationService.authorizeCustomerIdm(requestingToken);

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

        boolean isAuthorized = authorizationService.authorizeCustomerIdm(requestingToken);

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

        for (User user : getAllUsersForCustomerId(customerId)) {
            accessTokenService.deleteAllForOwner(user.getUsername());
        }

        for (Client client : getAllClientsForCustomerId(customerId)) {
            accessTokenService.deleteAllForOwner(client.getClientId());
        }

        logger.debug("Deleted all access tokens for customer {}.", customerId);

    }

    @Override
    public void revokeTokensGloballyForCustomer(String customerId) {
        List<User> usersList = getAllUsersForCustomerId(customerId);
        for (User user : usersList) {
            refreshTokenService.deleteAllTokensForUser(user.getUsername());
        }

        List<Client> clientsList = getAllClientsForCustomerId(customerId);

        accessTokenService.deleteAllGloballyForCustomer(customerId, usersList, clientsList);
        logger.debug("Deleted all access tokens for customer {}.", customerId);
    }

    private List<Client> getAllClientsForCustomerId(String customerId) {
        List<Client> clientsList = new ArrayList<Client>();
        int total = 1; // This gets overwritten, just needs to be greater than
                       // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            Clients clientsObj = clientService.getByCustomerId(customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(String customerId) {
        List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
                       // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            Users usersObj = userService.getByCustomerId(customerId, offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        return usersList;
    }

    private void revokeToken(String tokenStringRequestingDelete, String tokenToDelete, boolean isGlobal)
        throws NotAuthorizedException {
        logger.info("Deleting Token {}", tokenToDelete);

        AccessToken deletedToken = accessTokenService.getAccessTokenByTokenString(tokenToDelete);
        if (deletedToken == null) {
            String error = "No entry found for token " + deletedToken;
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

        boolean isGoodAsIdm = authorizationService.authorizeCustomerIdm(requestingToken);
        boolean isAuthorized = false;
        if (isGlobal) {
            // Only CustomerIdm Client and Client that got token or the user of
            // the token are authorized to revoke token
            isAuthorized = isGoodAsIdm || authorizationService.authorizeAsRequestorOrOwner(deletedToken, requestingToken);
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

        if (deletedToken.getRequestor() == null) {
            String error = String
                .format("Token %s does not have a requestor", deletedToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        if (isGlobal) {
            // This user is to be completely logged out of the system.
            deleteRefreshTokenByAccessToken(deletedToken);
            accessTokenService.deleteAllGloballyForOwner(deletedToken.getOwner());
        } else {
            accessTokenService.delete(deletedToken.getTokenString());
        }
        logger.info("Deleted Token {}", deletedToken);
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

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }
}
