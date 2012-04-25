package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.converter.TenantRoleConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.RSAClient;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.validation.groups.Default;
import java.util.List;
import java.util.UUID;

import static com.rackspace.idm.domain.entity.OAuthGrantType.*;

public class DefaultAuthenticationService implements AuthenticationService {

    private final ApplicationDao clientDao;
    private final TenantService tenantService;
    private final ScopeAccessService scopeAccessService;
    private final AuthDao authDao;
    private final Configuration config;
    private final UserDao userDao;
    private final CustomerDao customerDao;
    private final InputValidator inputValidator;
    private final TenantRoleConverter tenantRoleConverter;

    @Autowired
    private RSAClient rsaClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultAuthenticationService(TokenService tokenService,
                                        AuthDao authDao, TenantService tenantService,
                                        ScopeAccessService scopeAccessService,
                                        ApplicationDao clientDao,
                                        Configuration config, UserDao userDao,
                                        CustomerDao customerDao, InputValidator inputValidator,
                                        TenantRoleConverter tenantRoleConverter) {
        this.authDao = authDao;
        this.tenantService = tenantService;
        this.scopeAccessService = scopeAccessService;
        this.clientDao = clientDao;
        this.config = config;
        this.userDao = userDao;
        this.customerDao = customerDao;
        this.inputValidator = inputValidator;
        this.tenantRoleConverter = tenantRoleConverter;
    }

    @Override
    public AuthData authenticate(Credentials credentials) {

        validateCredentials(credentials);

        ScopeAccess scopeAccess = getTokens(credentials, this.getCurrentTime());

        return getAuthData(scopeAccess);
    }

    @Override
    public AuthData getAuthDataFromToken(String authToken) {
        ScopeAccess scopeAccess = this.scopeAccessService
                .loadScopeAccessByAccessToken(authToken);

        return getAuthDataWithClientRoles(scopeAccess);
    }

    /**
     * Gets the auth data object from corresponding scope access object. Also
     * appends the roles of the client that is authenticated.
     *
     * @param scopeAccess
     * @return AuthData with roles
     */
    private AuthData getAuthDataWithClientRoles(ScopeAccess scopeAccess) {
        AuthData authData = getAuthData(scopeAccess);

        if (authData.getUser() != null) {
            User user = authData.getUser();
            List<TenantRole> roles = tenantService
                    .getTenantRolesForScopeAccess(scopeAccess);
            user.setRoles(roles);
        } else if (authData.getApplication() != null) {
            Application application = authData.getApplication();
            List<TenantRole> roles = tenantService
                    .getTenantRolesForScopeAccess(scopeAccess);
            application.setRoles(roles);
        } else if (authData.getRacker() != null) {
            Racker racker = authData.getRacker();
            racker.setRackerRoles(authDao.getRackerRoles(racker.getRackerId()));
        }

        return authData;
    }

    /**
     * Gets the auth data object from corresponding scope access object
     *
     * @param scopeAccess
     * @return AuthData
     */
    private AuthData getAuthData(ScopeAccess scopeAccess) {
        AuthData authData = new AuthData();

        if (scopeAccess instanceof HasAccessToken) {
            HasAccessToken tokenScopeAccessObject = (HasAccessToken) scopeAccess;
            authData.setAccessToken(tokenScopeAccessObject
                    .getAccessTokenString());
            authData.setAccessTokenExpiration(tokenScopeAccessObject
                    .getAccessTokenExp());
        }

        if (scopeAccess instanceof HasRefreshToken) {
            HasRefreshToken tokenScopeAccessObject = (HasRefreshToken) scopeAccess;
            authData.setRefreshToken(tokenScopeAccessObject
                    .getRefreshTokenString());
        }

        if (scopeAccess instanceof PasswordResetScopeAccess) {
            PasswordResetScopeAccess prsca = (PasswordResetScopeAccess) scopeAccess;
            DateTime passwordExpirationDate = prsca.getUserPasswordExpirationDate();

            authData.setPasswordResetOnlyToken(true);
            authData.setPasswordExpirationDate(passwordExpirationDate);
        }

        setClient(scopeAccess, authData);

        return authData;
    }

    /**
     * All auth data must have exactly one client that is attached to it, a
     * user, an application, or a racker. This method determines what that
     * client is based on the type of scope access and set the client
     * accordingly.
     *
     * @param scopeAccess
     * @param authData
     */
    private void setClient(ScopeAccess scopeAccess, AuthData authData) {
        if (scopeAccess instanceof ClientScopeAccess) {
            // TODO: consider getting from client dao, so can retrieve more info
            // about client
            Application application = new Application();
            application.setClientId(scopeAccess.getClientId());
            application.setRCN(scopeAccess.getClientRCN());
            authData.setApplication(application);
        }

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
            DateTime passwordExpirationDate = userScopeAccess
                    .getUserPasswordExpirationDate();

            // TODO: consider getting from user dao
            User user = new User();
            user.setUsername(userScopeAccess.getUsername());
            user.setCustomerId(userScopeAccess.getUserRCN());

            authData.setUser(user);
            authData.setPasswordExpirationDate(passwordExpirationDate);
        }

        if (scopeAccess instanceof DelegatedClientScopeAccess) {
            DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;

            // TODO: consider getting from user dao
            User user = new User();
            user.setUsername(dcsa.getUsername());
            user.setCustomerId(dcsa.getUserRCN());
            authData.setUser(user);
        }

        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;

            Racker racker = new Racker();
            racker.setRackerId(rackerScopeAccess.getRackerId());
            authData.setRacker(racker);
        }
    }

    private ScopeAccess getTokens(final Credentials trParam, final DateTime currentTime) throws NotAuthenticatedException {

        OAuthGrantType grantType = trParam.getOAuthGrantType();
        if (StringUtils.isBlank(trParam.getClientId())) {
            String msg = "client_id cannot be blank";
            logger.warn(msg);
            throw new BadRequestException(msg);
        }

        final ClientAuthenticationResult caResult = clientDao.authenticate(trParam.getClientId(), trParam.getClientSecret());
        if (!caResult.isAuthenticated()) {
            final String message = "Bad Client credentials for " + trParam.getClientId();
            logger.warn(message);
            throw new NotAuthenticatedException(message);
        }

        if (trParam instanceof RackerCredentials) {
            if (StringUtils.isBlank(trParam.getUsername())) {
                String msg = "username cannot be blank";
                logger.warn(msg);
                throw new BadRequestException(msg);
            }

            final UserAuthenticationResult uaResult = authenticateRacker(trParam.getUsername(), trParam.getPassword(), false);
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad User credentials for " + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }

            RackerScopeAccess scopeAccess = this.getAndUpdateRackerScopeAccessForClientId((Racker) uaResult.getUser(), caResult.getClient());
            return scopeAccess;
        }

        if (trParam instanceof RSACredentials) {
            final UserAuthenticationResult uaResult = authenticateRacker(trParam.getUsername(), trParam.getPassword(), true);
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad RSA credentials for " + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }
            RackerScopeAccess scopeAccess = this.getAndUpdateRackerScopeAccessForClientId((Racker) uaResult.getUser(), caResult.getClient());
            return scopeAccess;
        }

        if (PASSWORD == grantType) {

            if (StringUtils.isBlank(trParam.getUsername())) {
                String msg = "username cannot be blank";
                logger.warn(msg);
                throw new BadRequestException(msg);
            }

            final UserAuthenticationResult uaResult = authenticate(trParam.getUsername(), trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad User credentials for "
                        + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }

            DateTime rotationDate = getUserPasswordExpirationDate(uaResult.getUser().getUsername());
            if (rotationDate != null && rotationDate.isBefore(currentTime)) {
                PasswordResetScopeAccess prsa = this.scopeAccessService
                        .getOrCreatePasswordResetScopeAccessForUser(uaResult
                                .getUser());
                prsa.setUserPasswordExpirationDate(rotationDate);
                return prsa;
            }

            UserScopeAccess usa = this.getAndUpdateUserScopeAccessForClientId(uaResult.getUser(), caResult.getClient());
            usa.setUserPasswordExpirationDate(rotationDate);
            return usa;
        }

        if (REFRESH_TOKEN == grantType) {
            ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByRefreshToken(trParam.getRefreshToken());
            if (scopeAccess == null
                    || ((HasRefreshToken) scopeAccess).isRefreshTokenExpired(currentTime)
                    || !scopeAccess.getClientId().equalsIgnoreCase(caResult.getClient().getClientId())) {
                final String msg = String.format("Unauthorized Refresh Token: %s", trParam.getRefreshToken());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            if (scopeAccess instanceof UserScopeAccess) {
                String username = ((UserScopeAccess) scopeAccess).getUsername();
                String userId = ((UserScopeAccess) scopeAccess).getUserRsId();
                User user = this.userDao.getUserById(userId);
                if (user == null || user.isDisabled()) {
                    String errMsg = String.format("User %S is disabled", username);
                    logger.info(errMsg);
                    throw new UserDisabledException(errMsg);
                }
            }

            ((HasAccessToken) scopeAccess).setAccessTokenString(this
                    .generateToken());
            ((HasAccessToken) scopeAccess).setAccessTokenExp(new DateTime()
                    .plusSeconds(this.getDefaultTokenExpirationSeconds())
                    .toDate());
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
                        "Unauthorized Authorization Code: %s", trParam
                        .getAuthorizationCode());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(currentTime.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
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

    private UserScopeAccess getAndUpdateUserScopeAccessForClientId(User user,
                                                                   Application client) {

        logger.debug(
                "Get and Update ScopeAccess for User: {} and ClientId: {}",
                user.getUsername(), client.getClientId());

        UserScopeAccess scopeAccess = this.scopeAccessService
                .getUserScopeAccessForClientId(user.getUniqueId(), client
                        .getClientId());

        if (scopeAccess == null) {
            String errMsg = String.format(
                    "User %s not provisioned for client %s",
                    user.getUsername(), client.getClientId());
            logger.warn(errMsg);
            throw new NotProvisionedException(errMsg);
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                    this.getDefaultTokenExpirationSeconds()).toDate());
        }

        DateTime refreshExpiration = scopeAccess.getRefreshTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccess.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setRefreshTokenExp(current.plusYears(100).toDate());
        }

        logger.debug("Updating Expirations for User: {} and ClientId: {}", user
                .getUsername(), client.getClientId());
        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger.debug("Returning ScopeAccess: {} Expiration {}", scopeAccess
                .getAccessTokenString(), scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    private ClientScopeAccess getAndUpdateClientScopeAccessForClientId(
            Application client) {

        logger.debug("Get and Update Client ScopeAccess for ClientId: {}",
                client.getClientId());

        ClientScopeAccess scopeAccess = this.scopeAccessService
                .getClientScopeAccessForClientId(client.getUniqueId(), client
                        .getClientId());

        if (scopeAccess == null) {
            logger.debug(
                    "Creating ScopeAccess for Client: {} and ClientId: {}",
                    client.getClientId(), client.getClientId());
            scopeAccess = new ClientScopeAccess();
            scopeAccess.setClientId(client.getClientId());
            scopeAccess.setClientRCN(client.getRCN());
            scopeAccess = (ClientScopeAccess) this.scopeAccessService
                    .addDirectScopeAccess(client.getUniqueId(), scopeAccess);
        }

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(
                    this.getDefaultTokenExpirationSeconds()).toDate());
            logger.debug("Updating ScopeAccess: {} Expiration {}", scopeAccess
                    .getAccessTokenString(), scopeAccess.getAccessTokenExp());
        }

        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccess
                .getAccessTokenString(), scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    RackerScopeAccess getAndUpdateRackerScopeAccessForClientId(Racker racker, Application client) {

        logger.debug("Get and Update ScopeAccess for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());

        RackerScopeAccess scopeAccess = scopeAccessService.getRackerScopeAccessForClientId(racker.getUniqueId(), client.getClientId());
        if (scopeAccess == null) {
            // Auto-Provision Scope Access Objects for Rackers
            scopeAccess = new RackerScopeAccess();
            scopeAccess.setRackerId(racker.getRackerId());
            scopeAccess.setClientId(client.getClientId());
            scopeAccess.setClientRCN(client.getRCN());
            logger.debug("Creating ScopeAccess for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());
            scopeAccess = (RackerScopeAccess) scopeAccessService.addDirectScopeAccess(racker.getUniqueId(), scopeAccess);
        }
        validateRackerHasRackerRole(racker, scopeAccess, client);

        DateTime current = new DateTime();

        DateTime accessExpiration = scopeAccess.getAccessTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccess.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(current.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
        }

        DateTime refreshExpiration = scopeAccess.getRefreshTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccess.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccess.setRefreshTokenString(this.generateToken());
            scopeAccess.setRefreshTokenExp(current.plusYears(100).toDate());
        }

        logger.debug("Updating Expirations for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());
        this.scopeAccessService.updateScopeAccess(scopeAccess);

        logger.debug("Returning ScopeAccess: {} Expiration {}", scopeAccess.getAccessTokenString(), scopeAccess.getAccessTokenExp());
        return scopeAccess;
    }

    private void validateRackerHasRackerRole(Racker racker, RackerScopeAccess scopeAccess, Application client) {
        List<TenantRole> tenantRolesForScopeAccess = tenantService.getTenantRolesForScopeAccess(scopeAccess);
        boolean hasRackerRole = false;
        for (TenantRole tenantRole : tenantRolesForScopeAccess) {
            if (tenantRole.getName().equals("Racker") && tenantRole.getClientId().equals(config.getString("idm.clientId"))) {
                hasRackerRole = true;
            }
        }
        if (!hasRackerRole) {
            List<ClientRole> clientRoles = clientDao.getClientRolesByClientId(config.getString("idm.clientId"));
            for(ClientRole clientRole : clientRoles){
                if(clientRole.getName().equals("Racker")){
                    TenantRole tenantRole = new TenantRole();
                    tenantRole.setRoleRsId(clientRole.getId());
                    tenantRole.setName(clientRole.getName());
                    tenantRole.setClientId(clientRole.getClientId());
                    tenantService.addTenantRoleToUser(racker, tenantRole);
                }
            }
        }
    }

    private UserAuthenticationResult authenticate(String username, String password) {
        logger.debug("Authenticating User: {}", username);

        UserAuthenticationResult result = userDao.authenticate(username,
                password);

        logger.debug("Authenticated User: {} : {}", username, result);
        return result;
    }

    UserAuthenticationResult authenticateRacker(String username, String password, boolean usesRSAAuth) {
        logger.debug("Authenticating Racker: {}", username);

        if (!isTrustedServer()) {
            throw new ForbiddenException();
        }
        boolean authenticated;
        if (usesRSAAuth) {
            authenticated = rsaClient.authenticate(username, password);
        } else {
            authenticated = authDao.authenticate(username, password);
        }
        logger.debug("Authenticated Racker {} : {}", username, authenticated);

        Racker racker = userDao.getRackerByRackerId(username);
        if (racker == null) {
            racker = new Racker();
            racker.setRackerId(username);
            this.userDao.addRacker(racker);
        }

        return new UserAuthenticationResult(racker, authenticated);
    }

    private DateTime getUserPasswordExpirationDate(String userName) {
        User user = this.userDao.getUserByUsername(userName);
        if (user == null) {
            logger.debug("No user found, returning null.");
            return null;
        }

        Customer customer = customerDao.getCustomerByCustomerId(user.getCustomerId());
        if (customer == null) {
            logger.debug("No customer found, returning null");
            return null;
        }

        if (customer.getPasswordRotationEnabled()) {
            int passwordRotationDurationInDays = customer.getPasswordRotationDuration();
            DateTime timeOfLastPwdChange = user.getPasswordObj().getLastUpdated();
            DateTime passwordExpirationDate = timeOfLastPwdChange.plusDays(passwordRotationDurationInDays);
            logger.debug("Password expiration date set: {}", passwordExpirationDate);
            return passwordExpirationDate;
        }

        return null;
    }

    void validateCredentials(final Credentials trParam) {
        ApiError error = null;
        if (trParam.getGrantType() == null) {
            throw new BadRequestException("Invalid request: Missing or malformed parameter(s).");
        }

        switch (trParam.getOAuthGrantType()) {
            case PASSWORD:
                error = inputValidator.validate(trParam, Default.class, BasicCredentialsCheck.class);
                break;

            case REFRESH_TOKEN:
                error = inputValidator.validate(trParam, Default.class, RefreshTokenCredentialsCheck.class);
                break;

            case AUTHORIZATION_CODE:
                error = inputValidator.validate(trParam, Default.class, AuthorizationCodeCredentialsCheck.class);
                break;

            default:
                error = inputValidator.validate(trParam);
        }

        if (error != null) {
            String msg = String.format("Bad request parameters: %s", error.getMessage());
            logger.warn(msg);
            throw new BadRequestException(msg);
        }
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

    protected DateTime getCurrentTime() {
        return new DateTime();
    }

    public void setRsaClient(RSAClient rsaClient) {
        this.rsaClient = rsaClient;
    }
}
