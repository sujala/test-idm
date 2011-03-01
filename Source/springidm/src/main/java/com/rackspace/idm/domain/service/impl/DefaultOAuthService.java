package com.rackspace.idm.domain.service.impl;

import static com.rackspace.idm.domain.entity.OAuthGrantType.NONE;
import static com.rackspace.idm.domain.entity.OAuthGrantType.PASSWORD;
import static com.rackspace.idm.domain.entity.OAuthGrantType.REFRESH_TOKEN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.RefreshToken;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.RefreshTokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;

public class DefaultOAuthService implements OAuthService {
    private UserService userService;
    private ClientService clientService;
    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private AuthorizationService authorizationService;
    private Configuration config;

    public DefaultOAuthService(UserService userService, ClientService clientService,
        AccessTokenService accessTokenService, RefreshTokenService refreshTokenService,
        AuthorizationService authorizationService, Configuration config) {
        this.userService = userService;
        this.clientService = clientService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authorizationService = authorizationService;
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
            String message = "Bad Client credentials.";
            logger.warn(message);
			throw new NotAuthenticatedException(message);
        }

        AccessToken accessToken = null;
        RefreshToken refreshToken = null;
        if (PASSWORD == grantType) {
            UserAuthenticationResult uaResult = userService.authenticate(trParam.getUsername(),
                trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                String message = "Bad User credentials.";
                logger.warn(message);
				throw new NotAuthenticatedException(message);
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

        String message = String.format("Unsupported GrantType: %s", grantType);
        logger.warn(message);
        throw new NotAuthenticatedException(message);
    }

    public void revokeTokensLocally(String tokenStringRequestingDelete, String tokenToDelete) {
        logger.info("Deleting Token {}", tokenToDelete);

        AccessToken deletedToken = accessTokenService.getAccessTokenByTokenString(tokenToDelete);
        if (deletedToken == null) {
            String error = "No entry found for token " + tokenToDelete;
            logger.debug(error);
            throw new NotFoundException(error);
        }

        AccessToken requestingToken = accessTokenService
            .getAccessTokenByTokenString(tokenStringRequestingDelete);
        if (requestingToken == null) {
            String error = "No entry found for token " + tokenStringRequestingDelete;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        boolean isAuthorized = authorizationService.authorizeCustomerIdm(requestingToken);
        if (!isAuthorized) {
            String errMsg;
            errMsg = String.format("Requesting token %s not authorized to revoke token %s locally.",
                tokenStringRequestingDelete, tokenToDelete);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (deletedToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor", deletedToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }
        accessTokenService.delete(deletedToken.getTokenString());
        logger.info("Deleted Token {}", deletedToken);
    }

    @Override
    public void revokeTokensGlobally(String tokenStringRequestingDelete, String tokenToDelete) {
        logger.info("Deleting Token {}", tokenToDelete);
        AccessToken deletedToken = accessTokenService.getAccessTokenByTokenStringGlobally(tokenToDelete);
        if (deletedToken == null) {
            String error = "No entry found for token " + tokenToDelete;
            logger.debug(error);
            throw new NotFoundException(error);
        }

        AccessToken requestingToken = accessTokenService
            .getAccessTokenByTokenStringGlobally(tokenStringRequestingDelete);
        if (requestingToken == null) {
            String error = "No entry found for token " + tokenStringRequestingDelete;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        boolean isGoodAsIdm = authorizationService.authorizeCustomerIdm(requestingToken);
        // Only CustomerIdm Client and Client that got token or the user of
        // the token are authorized to revoke token
        boolean isAuthorized = isGoodAsIdm
            || authorizationService.authorizeAsRequestorOrOwner(deletedToken, requestingToken);

        if (!isAuthorized) {
            String errMsg;
            errMsg = String.format("Requesting token %s not authorized to revoke token %s.",
                tokenStringRequestingDelete, tokenToDelete);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (deletedToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor", deletedToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }
        // This user is to be completely logged out of the system.
        deleteRefreshTokenByAccessToken(deletedToken);
        accessTokenService.deleteAllGloballyForOwner(deletedToken.getOwner());
        logger.info("Deleted Token {}", deletedToken);
    }

    @Override
    public void revokeTokensLocallyForOwner(String authTokenString, String ownerId) {
        AccessToken requestingToken = accessTokenService.getAccessTokenByTokenString(authTokenString);
        if (requestingToken == null) {
            String error = "No entry found for token " + authTokenString;
            logger.warn(error);
            throw new IllegalArgumentException(error);
        }

        boolean isAuthorized = authorizationService.authorizeCustomerIdm(requestingToken);

        if (!isAuthorized) {
            String errMsg = String.format("Requesting token %s not authorized to revoke token for owner %s.",
                authTokenString, ownerId);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (requestingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor",
                requestingToken.getTokenString());
            logger.warn(error);
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
            String error = "No entry found for token " + authTokenString;
            logger.warn(error);
            throw new IllegalArgumentException(error);
        }

        boolean isAuthorized = authorizationService.authorizeCustomerIdm(requestingToken);

        if (!isAuthorized) {
            String errMsg = String.format(
                "Requesting token %s not authorized to revoke token for customer %s.", authTokenString,
                customerId);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (requestingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor",
                requestingToken.getTokenString());
            logger.warn(error);
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
            String msg = String
                .format("Unauthorized Refresh Token: %s", refreshTokenString);
            logger.warn(msg);
            throw new NotAuthenticatedException(msg);
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

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }
}
