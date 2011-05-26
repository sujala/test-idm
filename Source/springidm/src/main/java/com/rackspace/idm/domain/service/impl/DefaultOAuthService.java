package com.rackspace.idm.domain.service.impl;

import static com.rackspace.idm.domain.entity.OAuthGrantType.AUTHORIZATION_CODE;
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
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.entity.hasRefreshToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.NotProvisionedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;

public class DefaultOAuthService implements OAuthService {
    private final AuthorizationService authorizationService;
    private final ClientService clientService;
    private final Configuration config;
    private final InputValidator inputValidator;
    final private Logger logger = LoggerFactory
        .getLogger(DefaultOAuthService.class);
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;

    {
        logger.info("Instantiating DefaultOauthService");
    }

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
    public ScopeAccess getAccessTokenByAuthHeader(final String authHeader) {
        return this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
    }

    
    @Override
    public OAuthGrantType getGrantType(final String grantTypeStrVal) {
        final OAuthGrantType grantType = OAuthGrantType.valueOf(grantTypeStrVal
            .replace("-", "_").toUpperCase());
        logger.debug("Verified GrantType: {}", grantTypeStrVal);
        return grantType;
    }

    
    @Override
    public ScopeAccess getTokens(final OAuthGrantType grantType,
        final AuthCredentials trParam, final DateTime currentTime)
        throws NotAuthenticatedException {

        if (StringUtils.isBlank(trParam.getClientId())) {
            String msg = "client_id cannot be blank";
            logger.warn(msg);
            throw new BadRequestException(msg);
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
                String msg = "username cannot be blank";
                logger.warn(msg);
                throw new BadRequestException(msg);
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
                racker.setRackerId(uaResult.getUser().getUsername());
                racker.setUniqueId(uaResult.getUser().getUniqueId());

                RackerScopeAccess scopeAccess = this
                    .getAndUpdateRackerScopeAccessForClientId(racker,
                        caResult.getClient());
                return scopeAccess;
            }

            DateTime rotationDate = this.userService
                .getUserPasswordExpirationDate(uaResult.getUser().getUsername());

            if (rotationDate != null && rotationDate.isBefore(currentTime)) {
                PasswordResetScopeAccess prsa = this.scopeAccessService
                    .getOrCreatePasswordResetScopeAccessForUser(uaResult
                        .getUser());
                prsa.setUserPasswordExpirationDate(rotationDate);
                return prsa;
            }

            UserScopeAccess usa = this.getAndUpdateUserScopeAccessForClientId(
                uaResult.getUser(), caResult.getClient());
            usa.setUserPasswordExpirationDate(rotationDate);
            return usa;
        }

        if (REFRESH_TOKEN == grantType) {

            ScopeAccess scopeAccess = this.scopeAccessService
                .getScopeAccessByRefreshToken(trParam.getRefreshToken());
            if (scopeAccess == null
                || ((hasRefreshToken) scopeAccess)
                    .isRefreshTokenExpired(currentTime)
                || !scopeAccess.getClientId().equalsIgnoreCase(
                    caResult.getClient().getClientId())) {
                final String msg = String
                    .format("Unauthorized Refresh Token: %s",
                        trParam.getRefreshToken());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            if (scopeAccess instanceof UserScopeAccess) {
                String username = ((UserScopeAccess) scopeAccess).getUsername();
                User user = this.userService.getUser(username);
                if (user == null || user.isDisabled()) {
                    String errMsg = String.format("User %S is disabled",
                        username);
                    logger.info(errMsg);
                    throw new UserDisabledException(errMsg);
                }
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

        if (AUTHORIZATION_CODE == grantType) {
            
            DelegatedClientScopeAccess scopeAccess = this.scopeAccessService
                .getScopeAccessByAuthCode(trParam.getAuthorizationCode());
            if (scopeAccess == null
                || scopeAccess.isAuthorizationCodeExpired(currentTime)
                || !scopeAccess.getClientId().equalsIgnoreCase(
                    caResult.getClient().getClientId())) {
                final String msg = String.format(
                    "Unauthorized Authorization Code: %s",
                    trParam.getAuthorizationCode());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(currentTime.plusSeconds(
                this.getDefaultTokenExpirationSeconds()).toDate());
            scopeAccess.setAuthCode(null);
            scopeAccess.setAuthCodeExp(null);

            this.scopeAccessService.updateScopeAccess(scopeAccess);

            return scopeAccess;
        }

        final String message = String.format("Unsupported GrantType: %s",
            grantType);
        logger.warn(message);
        throw new NotAuthenticatedException(message);
    }

    
    @Override
    public void revokeAccessToken(String tokenStringRequestingDelete,
        String tokenToDelete) {
        logger.debug("Deleting Token {}", tokenToDelete);
        ScopeAccess scopeAccessToDelete = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenToDelete);

        if (scopeAccessToDelete == null) {
            final String error = "No entry found for token " + tokenToDelete;
            logger.warn(error);
            throw new NotFoundException(error);
        }

        ScopeAccess scopeAccessRequestor = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenStringRequestingDelete);

        if (scopeAccessRequestor == null) {
            final String error = "No entry found for token "
                + tokenStringRequestingDelete;
            logger.warn(error);
            throw new IllegalStateException(error);
        }

        final boolean isGoodAsIdm = authorizationService
            .authorizeCustomerIdm(scopeAccessRequestor);
        // Only CustomerIdm Client and Client that got token or the user of
        // the token are authorized to revoke token
        final boolean isAuthorized = isGoodAsIdm
            || authorizationService.authorizeAsRequestorOrOwner(
                scopeAccessToDelete, scopeAccessRequestor);

        if (!isAuthorized) {
            String errMsg;
            errMsg = String.format(
                "Requesting token %s not authorized to revoke token %s.",
                tokenStringRequestingDelete, tokenToDelete);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (scopeAccessToDelete instanceof hasAccessToken) {
            ((hasAccessToken) scopeAccessToDelete).setAccessTokenExpired();
            this.scopeAccessService.updateScopeAccess(scopeAccessToDelete);
        }

        logger.debug("Deleted Token {}", tokenToDelete);
    }

    
    @Override
    public void revokeAllTokensForClient(final String clientId) {
        logger.debug("Deleting all access tokens for client {}.", clientId);
        this.scopeAccessService.expireAllTokensForClient(clientId);
        logger.debug("Deleted all access tokens for client {}.", clientId);
    }

    
    @Override
    public void revokeAllTokensForCustomer(final String customerId) {
        logger
            .debug("Revoking all access tokens for customer: {}.", customerId);
        final List<User> usersList = getAllUsersForCustomerId(customerId);
        for (final User user : usersList) {
            this.scopeAccessService.expireAllTokensForUser(user.getUsername());
        }

        final List<Client> clientsList = getAllClientsForCustomerId(customerId);
        for (final Client client : clientsList) {
            this.scopeAccessService.expireAllTokensForClient(client
                .getClientId());
        }

        logger.debug("Deleted all access tokens for customer {}.", customerId);
    }

    
    @Override
    public void revokeAllTokensForUser(final String username) {
        logger.debug("Deleting all access tokens for user {}.", username);
        this.scopeAccessService.expireAllTokensForUser(username);
        logger.debug("Deleted all access tokens for user {}.", username);
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

        if (OAuthGrantType.AUTHORIZATION_CODE == grantType) {
            return inputValidator.validate(trParam, Default.class,
                AuthorizationCodeCredentialsCheck.class);
        }

        return inputValidator.validate(trParam);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // private functions
    private List<Client> getAllClientsForCustomerId(final String customerId) {
        logger.debug("Finding Clients from CustomerId: {}", customerId);
        final List<Client> clientsList = new ArrayList<Client>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Clients clientsObj = clientService.getByCustomerId(
                customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        logger.debug("Found {} Client(s) from CustomerId: {}",
            clientsList.size(), customerId);
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(final String customerId) {
        logger.debug("Finding Users for CustomerId: {}", customerId);
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userService.getByCustomerId(customerId,
                offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        logger.debug("Found {} User(s) for CustomerId: {}", usersList.size(),
            customerId);
        return usersList;
    }

    private ClientScopeAccess getAndUpdateClientScopeAccessForClientId(
        BaseClient client) {

        logger.debug("Get and Update Client ScopeAccess for ClientId: {}",
            client.getClientId());

        ClientScopeAccess scopeAccess = this.scopeAccessService
            .getClientScopeAccessForClientId(client.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            logger.debug(
                "Creating ScopeAccess for Client: {} and ClientId: {}",
                client.getClientId(), client.getClientId());
            scopeAccess = new ClientScopeAccess();
            scopeAccess.setClientId(client.getClientId());
            scopeAccess.setClientRCN(client.getCustomerId());
            scopeAccess = (ClientScopeAccess) this.scopeAccessService
                .addDirectScopeAccess(client.getUniqueId(), scopeAccess);
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
            .minusDays(1) : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                this.getDefaultTokenExpirationSeconds()).toDate());
            logger.debug("Updating ScopeAccess: {} Expiration {}",
                scopeAccess.getAccessTokenString(),
                scopeAccess.getAccessTokenExp());
        }

        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger
            .debug("Found ScopeAccess: {} Expiration {}",
                scopeAccess.getAccessTokenString(),
                scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    private RackerScopeAccess getAndUpdateRackerScopeAccessForClientId(
        Racker racker, BaseClient client) {

        logger.debug(
            "Get and Update ScopeAccess for Racker: {} and ClientId: {}",
            racker.getRackerId(), client.getClientId());

        RackerScopeAccess scopeAccess = this.scopeAccessService
            .getRackerScopeAccessForClientId(racker.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            // Auto-Provision Scope Access Objects for Rackers
            scopeAccess = new RackerScopeAccess();
            scopeAccess.setRackerId(racker.getRackerId());
            scopeAccess.setClientId(client.getClientId());
            scopeAccess.setClientRCN(client.getCustomerId());
            logger.debug(
                "Creating ScopeAccess for Racker: {} and ClientId: {}",
                racker.getRackerId(), client.getClientId());
            scopeAccess = (RackerScopeAccess) this.scopeAccessService
                .addDirectScopeAccess(racker.getUniqueId(), scopeAccess);
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
            scopeAccess.setRefreshTokenExp(current.plusYears(100).toDate());
        }

        logger.debug("Updating Expirations for Racker: {} and ClientId: {}",
            racker.getRackerId(), client.getClientId());
        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger
            .debug("Returning ScopeAccess: {} Expiration {}",
                scopeAccess.getAccessTokenString(),
                scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    private UserScopeAccess getAndUpdateUserScopeAccessForClientId(
        BaseUser user, BaseClient client) {

        logger.debug(
            "Get and Update ScopeAccess for User: {} and ClientId: {}",
            user.getUsername(), client.getClientId());

        UserScopeAccess scopeAccess = this.scopeAccessService
            .getUserScopeAccessForClientId(user.getUniqueId(),
                client.getClientId());

        if (scopeAccess == null) {
            String errMsg = String.format(
                "User %s not provisioned for client %s", user.getUsername(),
                client.getClientId());
            logger.warn(errMsg);
            throw new NotProvisionedException(errMsg);
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
            scopeAccess.setRefreshTokenExp(current.plusYears(100).toDate());
        }

        logger.debug("Updating Expirations for User: {} and ClientId: {}",
            user.getUsername(), client.getClientId());
        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger
            .debug("Returning ScopeAccess: {} Expiration {}",
                scopeAccess.getAccessTokenString(),
                scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    private int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }
}
