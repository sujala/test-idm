package com.rackspace.idm.domain.service.impl;

import static com.rackspace.idm.domain.entity.OAuthGrantType.CLIENT_CREDENTIALS;
import static com.rackspace.idm.domain.entity.OAuthGrantType.PASSWORD;
import static com.rackspace.idm.domain.entity.OAuthGrantType.REFRESH_TOKEN;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.groups.Default;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.entity.hasRefreshToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;

public class DefaultOAuthService implements OAuthService {
    private final UserService userService;
    private final ClientService clientService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthorizationService authorizationService;
    private final Configuration config;
    private final InputValidator inputValidator;
    private final ScopeAccessService scopeAccessService;

    public DefaultOAuthService(final UserService userService,
        final ClientService clientService,
        final AuthorizationService authorizationService,
        final Configuration config, final InputValidator inputValidator,
        final ScopeAccessService scopeAccessService) {
        this.userService = userService;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.config = config;
        this.inputValidator = inputValidator;
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public ScopeAccessObject getAccessTokenByAuthHeader(final String authHeader) {
        return this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
    }

    @Override
    public ScopeAccessObject getTokens(final OAuthGrantType grantType,
        final AuthCredentials trParam, final DateTime currentTime)
        throws NotAuthenticatedException {

        if (StringUtils.isBlank(trParam.getClientId())) {
            throw new BadRequestException("client_id cannot be blank");
        }

        final ClientAuthenticationResult caResult = clientService.authenticate(
            trParam.getClientId(), trParam.getClientSecret());
        if (!caResult.isAuthenticated()) {
            final String message = "Bad Client credentials for "
                + trParam.getClientId();
            logger.warn(message);
            throw new NotAuthenticatedException(message);
        }

        if (PASSWORD == grantType) {

            if (StringUtils.isBlank(trParam.getUsername())) {
                throw new BadRequestException("username cannot be blank");
            }

            final UserAuthenticationResult uaResult = userService.authenticate(
                trParam.getUsername(), trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad User credentials for "
                    + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }

            if (isTrustedServer()) {
                Racker racker = new Racker();
                racker.setUniqueId(uaResult.getUser().getUniqueId());
                racker.setRackerId(uaResult.getUser().getUsername());
                RackerScopeAccessObject scopeAccess = this
                    .getAndUpdateRackerScopeAccessForClientId(racker,
                        caResult.getClient());
                return scopeAccess;
            }

            DateTime rotationDate = this.userService
                .getUserPasswordExpirationDate(uaResult.getUser().getUsername());

            if (rotationDate != null && rotationDate.isBefore(currentTime)) {
                PasswordResetScopeAccessObject prsa = this.scopeAccessService
                    .getOrCreatePasswordResetScopeAccessForUser(uaResult
                        .getUser().getUniqueId());
                prsa.setUserPasswordExpirationDate(rotationDate);
                return prsa;
            }

            UserScopeAccessObject usa = this
                .getAndUpdateUserScopeAccessForClientId(uaResult.getUser(),
                    caResult.getClient());
            usa.setUserPasswordExpirationDate(rotationDate);
            return usa;
        }

        if (REFRESH_TOKEN == grantType) {

            ScopeAccessObject scopeAccess = this.scopeAccessService
                .getScopeAccessByRefreshToken(trParam.getRefreshToken());
            if (scopeAccess == null
                || ((hasRefreshToken) scopeAccess)
                    .isRefreshTokenExpired(currentTime)) {
                final String msg = String
                    .format("Unauthorized Refresh Token: %s",
                        trParam.getRefreshToken());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }
            ((hasAccessToken) scopeAccess).setAccessTokenString(this
                .generateToken());
            ((hasAccessToken) scopeAccess).setAccessTokenExp(new DateTime()
                .plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
            this.scopeAccessService.updateScopeAccess(scopeAccess);
            return scopeAccess;
        }

        if (CLIENT_CREDENTIALS == grantType) {
            return this.getAndUpdateClientScopeAccessForClientId(caResult
                .getClient());
        }

        final String message = String.format("Unsupported GrantType: %s",
            grantType);
        logger.warn(message);
        throw new NotAuthenticatedException(message);
    }

    //
    // @Override
    // public void revokeTokensLocally(final String tokenStringRequestingDelete,
    // final String tokenToDelete) {
    // logger.info("Deleting Token {}", tokenToDelete);
    //
    // final AccessToken deletedToken = accessTokenService
    // .getAccessTokenByTokenString(tokenToDelete);
    // if (deletedToken == null) {
    // final String error = "No entry found for token " + tokenToDelete;
    // logger.debug(error);
    // throw new NotFoundException(error);
    // }
    //
    // final AccessToken requestingToken = accessTokenService
    // .getAccessTokenByTokenString(tokenStringRequestingDelete);
    // if (requestingToken == null) {
    // final String error = "No entry found for token "
    // + tokenStringRequestingDelete;
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    //
    // final boolean isAuthorized = authorizationService
    // .authorizeCustomerIdm(requestingToken);
    // if (!isAuthorized) {
    // String errMsg;
    // errMsg = String
    // .format(
    // "Requesting token %s not authorized to revoke token %s locally.",
    // tokenStringRequestingDelete, tokenToDelete);
    // logger.warn(errMsg);
    // throw new ForbiddenException(errMsg);
    // }
    //
    // if (deletedToken.getRequestor() == null) {
    // final String error = String.format(
    // "Token %s does not have a requestor",
    // deletedToken.getTokenString());
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    // accessTokenService.delete(deletedToken.getTokenString());
    // logger.info("Deleted Token {}", deletedToken);
    // }
    //
    // @Override
    // public void revokeTokensGlobally(final String
    // tokenStringRequestingDelete,
    // final String tokenToDelete) {
    // logger.debug("Deleting Token {}", tokenToDelete);
    // final AccessToken deletedToken = accessTokenService
    // .getAccessTokenByTokenStringGlobally(tokenToDelete);
    // if (deletedToken == null) {
    // final String error = "No entry found for token " + tokenToDelete;
    // logger.warn(error);
    // throw new NotFoundException(error);
    // }
    //
    // final AccessToken requestingToken = accessTokenService
    // .getAccessTokenByTokenStringGlobally(tokenStringRequestingDelete);
    // if (requestingToken == null) {
    // final String error = "No entry found for token "
    // + tokenStringRequestingDelete;
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    //
    // final boolean isGoodAsIdm = authorizationService
    // .authorizeCustomerIdm(requestingToken);
    // // Only CustomerIdm Client and Client that got token or the user of
    // // the token are authorized to revoke token
    // final boolean isAuthorized = isGoodAsIdm
    // || authorizationService.authorizeAsRequestorOrOwner(deletedToken,
    // requestingToken);
    //
    // if (!isAuthorized) {
    // String errMsg;
    // errMsg = String.format(
    // "Requesting token %s not authorized to revoke token %s.",
    // tokenStringRequestingDelete, tokenToDelete);
    // logger.warn(errMsg);
    // throw new ForbiddenException(errMsg);
    // }
    //
    // if (deletedToken.getRequestor() == null) {
    // final String error = String.format(
    // "Token %s does not have a requestor",
    // deletedToken.getTokenString());
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    // // This user is to be completely logged out of the system.
    // deleteRefreshTokenByAccessToken(deletedToken);
    // accessTokenService.deleteAllGloballyForOwner(deletedToken.getOwner());
    // logger.debug("Deleted Token {}", deletedToken);
    // }
    //
    // @Override
    // public void revokeTokensLocallyForOwner(final String authTokenString,
    // final String ownerId) {
    // final AccessToken requestingToken = accessTokenService
    // .getAccessTokenByTokenString(authTokenString);
    // if (requestingToken == null) {
    // final String error = "No entry found for token " + authTokenString;
    // logger.warn(error);
    // throw new IllegalArgumentException(error);
    // }
    //
    // final boolean isAuthorized = authorizationService
    // .authorizeCustomerIdm(requestingToken);
    //
    // if (!isAuthorized) {
    // final String errMsg = String
    // .format(
    // "Requesting token %s not authorized to revoke token for owner %s.",
    // authTokenString, ownerId);
    // logger.warn(errMsg);
    // throw new ForbiddenException(errMsg);
    // }
    //
    // if (requestingToken.getRequestor() == null) {
    // final String error = String.format(
    // "Token %s does not have a requestor",
    // requestingToken.getTokenString());
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    //
    // accessTokenService.deleteAllForOwner(ownerId);
    // logger.debug("Deleted all access tokens for owner {}.", ownerId);
    // }
    //
    // @Override
    // public void revokeTokensGloballyForOwner(final String ownerId) {
    // refreshTokenService.deleteAllTokensForUser(ownerId);
    // accessTokenService.deleteAllGloballyForOwner(ownerId);
    // logger.debug("Deleted all access tokens for owner {}.", ownerId);
    // }
    //
    // @Override
    // public void revokeTokensLocallyForCustomer(final String authTokenString,
    // final String customerId) {
    // final AccessToken requestingToken = accessTokenService
    // .getAccessTokenByTokenString(authTokenString);
    // if (requestingToken == null) {
    // final String error = "No entry found for token " + authTokenString;
    // logger.warn(error);
    // throw new IllegalArgumentException(error);
    // }
    //
    // final boolean isAuthorized = authorizationService
    // .authorizeCustomerIdm(requestingToken);
    //
    // if (!isAuthorized) {
    // final String errMsg = String
    // .format(
    // "Requesting token %s not authorized to revoke token for customer %s.",
    // authTokenString, customerId);
    // logger.warn(errMsg);
    // throw new ForbiddenException(errMsg);
    // }
    //
    // if (requestingToken.getRequestor() == null) {
    // final String error = String.format(
    // "Token %s does not have a requestor",
    // requestingToken.getTokenString());
    // logger.warn(error);
    // throw new IllegalStateException(error);
    // }
    //
    // for (final User user : getAllUsersForCustomerId(customerId)) {
    // accessTokenService.deleteAllForOwner(user.getUsername());
    // }
    //
    // for (final Client client : getAllClientsForCustomerId(customerId)) {
    // accessTokenService.deleteAllForOwner(client.getClientId());
    // }
    //
    // logger.debug("Deleted all access tokens for customer {}.", customerId);
    //
    // }
    //
    // @Override
    // public void revokeTokensGloballyForCustomer(final String customerId) {
    // final List<User> usersList = getAllUsersForCustomerId(customerId);
    // for (final User user : usersList) {
    // refreshTokenService.deleteAllTokensForUser(user.getUsername());
    // }
    //
    // final List<Client> clientsList = getAllClientsForCustomerId(customerId);
    //
    // accessTokenService.deleteAllGloballyForCustomer(customerId, usersList,
    // clientsList);
    // logger.debug("Deleted all access tokens for customer {}.", customerId);
    // }
    //
    // @Override
    // public void revokeTokensLocallyForOwnerOrCustomer(String idmAuthTokenStr,
    // TokenDeleteByType queryType, String ownerId) {
    // if (GlobalConstants.TokenDeleteByType.owner == queryType) {
    // revokeTokensLocallyForOwner(idmAuthTokenStr, ownerId);
    // logger.warn("Revoked Token for owner {}", ownerId);
    // } else if (GlobalConstants.TokenDeleteByType.customer == queryType) {
    // revokeTokensLocallyForCustomer(idmAuthTokenStr, ownerId);
    // logger.warn("Revoked Token for customer {}", ownerId);
    // } else {
    // // If this happens, the developer forgot to implement this.
    // throw new NotImplementedException("querytype " + queryType
    // + " is not supported.");
    // }
    // }

    @Override
    public OAuthGrantType getGrantType(final String grantTypeStrVal) {
        final OAuthGrantType grantType = OAuthGrantType.valueOf(grantTypeStrVal
            .replace("-", "_").toUpperCase());
        logger.debug("Verified GrantType: {}", grantTypeStrVal);
        return grantType;
    }

    @Override
    public ApiError validateGrantType(final AuthCredentials trParam,
        final OAuthGrantType grantType) {

        if (OAuthGrantType.PASSWORD == grantType) {
            return inputValidator.validate(trParam, Default.class,
                BasicCredentialsCheck.class);
        }

        if (OAuthGrantType.REFRESH_TOKEN == grantType) {
            return inputValidator.validate(trParam, Default.class,
                RefreshTokenCredentialsCheck.class);
        }

        return inputValidator.validate(trParam);
    }

    // private functions
    private List<Client> getAllClientsForCustomerId(final String customerId) {
        final List<Client> clientsList = new ArrayList<Client>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Clients clientsObj = clientService.getByCustomerId(
                customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(final String customerId) {
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userService.getByCustomerId(customerId,
                offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        return usersList;
    }

    private RackerScopeAccessObject getAndUpdateRackerScopeAccessForClientId(
        Racker racker, BaseClient client) {
        RackerScopeAccessObject scopeAccess = this.scopeAccessService
            .getRackerScopeAccessForClientId(racker.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            // TODO: Auto Provision?
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                this.getCloudAuthDefaultTokenExpirationSeconds()).toDate());
        }

        DateTime refreshExpiration = scopeAccess.getRefreshTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusYears(100).toDate());
        }

        this.scopeAccessService.updateScopeAccess(scopeAccess);

        return scopeAccess;
    }

    private UserScopeAccessObject getAndUpdateUserScopeAccessForClientId(
        BaseUser user, BaseClient client) {
        UserScopeAccessObject scopeAccess = this.scopeAccessService
            .getUserScopeAccessForClientId(user.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            // TODO: Throw new not provisioned error
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                this.getDefaultTokenExpirationSeconds()).toDate());
        }

        DateTime refreshExpiration = scopeAccess.getRefreshTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusYears(100).toDate());
        }

        this.scopeAccessService.updateScopeAccess(scopeAccess);

        return scopeAccess;
    }

    private ClientScopeAccessObject getAndUpdateClientScopeAccessForClientId(
        BaseClient client) {
        ClientScopeAccessObject scopeAccess = this.scopeAccessService
            .getClientScopeAccessForClientId(client.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            // TODO: Auto Provision?
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                this.getCloudAuthDefaultTokenExpirationSeconds()).toDate());
        }

        this.scopeAccessService.updateScopeAccess(scopeAccess);

        return scopeAccess;
    }

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

    public int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    public int getCloudAuthDefaultTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    private String generateToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        return token;
    }
}
