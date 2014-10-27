package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
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
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.validation.groups.Default;
import java.util.List;
import java.util.UUID;

import static com.rackspace.idm.domain.entity.OAuthGrantType.*;

@Component
public class DefaultAuthenticationService implements AuthenticationService {

    public static final int YEARS = 100;

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private UserService userService;
    @Autowired
    private InputValidator inputValidator;
    @Autowired
    private Configuration config;
    @Autowired
    private RSAClient rsaClient;
    @Autowired
    private IdentityUserService identityUserService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public AuthData authenticate(Credentials credentials) {

        validateCredentials(credentials);

        ScopeAccess scopeAccess = getTokens(credentials, this.getCurrentTime());

        return getAuthData(scopeAccess);
    }

    @Override
    public UserAuthenticationResult authenticateDomainUsernamePassword(String username, String password, Domain domain) {
        // ToDo: Check what Domain to authenticate against, default Rackers
        if(!isTrustedServer()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, password, false);
    }

    @Override
    public UserAuthenticationResult authenticateDomainRSA(String username, String tokenkey, Domain domain) {
        // ToDo: Check what Domain to authenticate against, default Rackers
        if(!isTrustedServer()){
            throw new ForbiddenException();
        }
        return authenticateRacker(username, tokenkey, true);
    }

    @Override
    public AuthData getAuthDataFromToken(String authToken) {
        ScopeAccess scopeAccess = this.scopeAccessService
                .loadScopeAccessByAccessToken(authToken);

        return getAuthDataWithClientRoles(scopeAccess);
    }

    @Override
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void setConfig(Configuration appConfig) {
        this.config = appConfig;
    }

    @Override
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void setInputValidator(InputValidator inputValidator) {
        this.inputValidator = inputValidator;
    }

    /**
     * Gets the auth data object from corresponding scope access object. Also
     * appends the roles of the client that is authenticated.
     *
     * @param scopeAccess
     * @return AuthData with roles
     */
    AuthData getAuthDataWithClientRoles(ScopeAccess scopeAccess) {
        AuthData authData = getAuthData(scopeAccess);

        if (authData.getUser() != null && (authData.getUser() instanceof User)) {
            //only care if persistent user. This is foundation API which is no longer used and should be deleted soon. Even
            //if it is used, it's not supported for federated users
            EndUser user = authData.getUser();
            List<TenantRole> roles = tenantService
                    .getTenantRolesForScopeAccess(scopeAccess);
            ((User)user).setRoles(roles);
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
    AuthData getAuthData(ScopeAccess scopeAccess) {
        AuthData authData = new AuthData();

        if (scopeAccess != null) {
            authData.setToken(scopeAccess);
            authData.setAccessToken(scopeAccess.getAccessTokenString());
            authData.setAccessTokenExpiration(scopeAccess.getAccessTokenExp());
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
    void setClient(ScopeAccess scopeAccess, AuthData authData) {
        if (scopeAccess instanceof ClientScopeAccess) {
            // TODO: consider getting from client dao, so can retrieve more info
            // about client
            Application application = new Application();
            application.setClientId(scopeAccess.getClientId());
            application.setRcn(scopeAccess.getClientRCN());
            authData.setApplication(application);
        }

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
            DateTime passwordExpirationDate = userScopeAccess
                    .getUserPasswordExpirationDate();

            // TODO: consider getting from user dao
            EndUser userEntity = identityUserService.getEndUserById(userScopeAccess.getUserRsId());
            User user = new User();
            user.setUsername(userEntity.getUsername());
            user.setCustomerId(userScopeAccess.getUserRCN());

            authData.setUser(user);
            authData.setPasswordExpirationDate(passwordExpirationDate);
        }

        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;

            Racker racker = new Racker();
            racker.setRackerId(rackerScopeAccess.getRackerId());
            racker.setRackerRoles(authDao.getRackerRoles(racker.getRackerId()));
            authData.setRacker(racker);
        }
    }

    ScopeAccess getTokens(final Credentials trParam, final DateTime currentTime) {

        if(trParam.getGrantType() == null){
            throw new BadRequestException("grant_type cannot be null");
        }

        OAuthGrantType grantType = trParam.getOAuthGrantType();

        if (StringUtils.isBlank(trParam.getClientId())) {
            String msg = "client_id cannot be blank";
            logger.warn(msg);
            throw new BadRequestException(msg);
        }

        final ClientAuthenticationResult caResult = applicationService.authenticate(trParam.getClientId(), trParam.getClientSecret());
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

            return this.getAndUpdateRackerScopeAccessForClientId((Racker) uaResult.getUser(), caResult.getClient());
        }

        if (trParam instanceof RSACredentials) {
            final UserAuthenticationResult uaResult = authenticateRacker(trParam.getUsername(), trParam.getPassword(), true);
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad RSA credentials for " + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }
            return this.getAndUpdateRackerScopeAccessForClientId((Racker) uaResult.getUser(), caResult.getClient());
        }

        if (PASSWORD == grantType) {

            if (StringUtils.isBlank(trParam.getUsername())) {
                String msg = "username cannot be blank";
                logger.warn(msg);
                throw new BadRequestException(msg);
            }

            final UserAuthenticationResult uaResult = authenticate(trParam.getUsername(), trParam.getPassword());
            if (!uaResult.isAuthenticated()) {
                final String message = "Bad User credentials for " + trParam.getUsername();
                logger.warn(message);
                throw new NotAuthenticatedException(message);
            }

            UserScopeAccess usa = this.getAndUpdateUserScopeAccessForClientId((User) uaResult.getUser(), caResult.getClient());
            return usa;
        }

        if (REFRESH_TOKEN == grantType) {
            ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByRefreshToken(trParam.getRefreshToken());
            ScopeAccess scopeAccessToAdd = new ScopeAccess();
            if (scopeAccess == null
                    || ((HasRefreshToken) scopeAccess).isRefreshTokenExpired(currentTime)
                    || !scopeAccess.getClientId().equalsIgnoreCase(caResult.getClient().getClientId())) {
                final String msg = String.format("Unauthorized Refresh Token: %s", trParam.getRefreshToken());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            if (scopeAccess instanceof UserScopeAccess) {
                String userId = ((UserScopeAccess) scopeAccess).getUserRsId();
                User user = this.userService.getUserById(userId);
                if (user == null) {
                    // this should never happen. If we are able to load the scope access for a user,
                    // then the user should exist in the directory.
                    String errMsg = String.format("User %S is disabled", userId);
                    logger.info(errMsg);
                    throw new UserDisabledException(errMsg);
                } else if (user.isDisabled()) {
                    String errMsg = String.format("User %S is disabled", user.getUsername());
                    logger.info(errMsg);
                    throw new UserDisabledException(errMsg);
                }
                ((UserScopeAccess) scopeAccess).setUserRsId(userId);
            }

            int expirationSeconds;
            if (scopeAccess instanceof RackerScopeAccess) {
                expirationSeconds = scopeAccessService.getTokenExpirationSeconds(getDefaultRackerTokenExpirationSeconds());
            } else {
                expirationSeconds = scopeAccessService.getTokenExpirationSeconds(getDefaultTokenExpirationSeconds());
            }

            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccessToAdd.setClientId(caResult.getClient().getClientId());
            scopeAccessToAdd.setClientRCN(caResult.getClient().getRcn());

            updateScopeAccess(scopeAccess, scopeAccessToAdd);

            return scopeAccessToAdd;
        }

        if (CLIENT_CREDENTIALS == grantType) {
            return this.getAndUpdateClientScopeAccessForClientId(caResult.getClient());
        }

        final String message = String.format("Unsupported GrantType: %s",
                grantType);
        logger.warn(message);
        throw new NotAuthenticatedException(message);
    }

    private void updateScopeAccess(ScopeAccess scopeAccess, ScopeAccess scopeAccessToAdd) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);

        if(user != null) {
            this.scopeAccessService.addUserScopeAccess(user, scopeAccessToAdd);
        } else {
            Application application = applicationService.getApplicationByScopeAccess(scopeAccess);

            if(application != null) {
                this.scopeAccessService.addApplicationScopeAccess(application, scopeAccessToAdd);
            }
        }

        this.scopeAccessService.deleteScopeAccess(scopeAccess);
    }

    UserScopeAccess getAndUpdateUserScopeAccessForClientId(User user, Application client) {
        if(user == null || client == null){
            throw new IllegalArgumentException("Argument(s) cannot be null.");
        }

        logger.debug("Get and Update ScopeAccess for User: {} and ClientId: {}", user.getUsername(), client.getClientId());

        UserScopeAccess scopeAccess = scopeAccessService.getUserScopeAccessByClientId(user, client.getClientId());
        UserScopeAccess scopeAccessToAdd = new UserScopeAccess();

        if (scopeAccess == null) {
            // provision scopeAccess with defaults
            scopeAccessToAdd.setUserRsId(user.getId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRcn());
        } else {
            scopeAccessToAdd.setUserRsId(scopeAccess.getUserRsId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRcn());
            scopeAccessToAdd.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setRefreshTokenExp(scopeAccess.getRefreshTokenExp());
            scopeAccessToAdd.setRefreshTokenString(this.generateToken());
        }

        DateTime current = new DateTime();
        DateTime accessExpiration = scopeAccessToAdd.getAccessTokenExp() == null ? new DateTime()
                .minusDays(1)
                : new DateTime(scopeAccessToAdd.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            int expirationSeconds = scopeAccessService.getTokenExpirationSeconds(getDefaultTokenExpirationSeconds());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(expirationSeconds).toDate());
        }

        DateTime refreshExpiration = scopeAccessToAdd.getRefreshTokenExp() == null ? new DateTime().minusDays(1)
                : new DateTime(scopeAccessToAdd.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccessToAdd.setRefreshTokenString(this.generateToken());
            scopeAccessToAdd.setRefreshTokenExp(current.plusYears(YEARS).toDate());
        }

        logger.debug("Updating Expirations for User: {} and ClientId: {}", user.getUsername(), client.getClientId());

        if (scopeAccess != null && !scopeAccess.isAccessTokenExpired(current)) {
            logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccess.getAccessTokenString(), scopeAccess.getAccessTokenExp());
            return scopeAccess;
        } else if (scopeAccess == null) {
            scopeAccessService.addUserScopeAccess(user, scopeAccessToAdd);
        } else {
            scopeAccessService.deleteScopeAccess(scopeAccess);
            scopeAccessService.addUserScopeAccess(user, scopeAccessToAdd);
        }

        logger.debug("Returning ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        return scopeAccessToAdd;
    }

    ClientScopeAccess getAndUpdateClientScopeAccessForClientId(Application client) {
        if(client == null){
            throw new IllegalArgumentException("Argument cannot be null.");
        }

        logger.debug("Get and Update Client ScopeAccess for ClientId: {}", client.getClientId());

        ClientScopeAccess scopeAccess = this.scopeAccessService.getApplicationScopeAccess(client);
        ClientScopeAccess scopeAccessToAdd = new ClientScopeAccess();

        if (scopeAccess == null) {
            scopeAccessToAdd.setClientRCN(client.getRcn());
            scopeAccessToAdd.setClientId(client.getClientId());
            logger.debug("Creating ScopeAccess for Client: {} and ClientId: {}", client.getClientId(), client.getClientId());
        } else {
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRcn());
            scopeAccessToAdd.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
        }

        DateTime current = new DateTime();
        DateTime accessExpiration = scopeAccessToAdd.getAccessTokenExp() == null ? new DateTime().minusDays(1)
                                                                                : new DateTime(scopeAccessToAdd.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            int expirationSeconds = scopeAccessService.getTokenExpirationSeconds(getDefaultTokenExpirationSeconds());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(expirationSeconds).toDate());
            logger.debug("Updating ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        }

        if (scopeAccess != null && !scopeAccess.isAccessTokenExpired(current)) {
            logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccess.getAccessTokenString(), scopeAccess.getAccessTokenExp());
            return scopeAccess;
        } else if (scopeAccess == null) {
            scopeAccessService.addApplicationScopeAccess(client, scopeAccessToAdd);
        } else {
            scopeAccessService.deleteScopeAccess(scopeAccess);
            scopeAccessService.addApplicationScopeAccess(client, scopeAccessToAdd);
        }

        logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        return scopeAccessToAdd;
    }

    RackerScopeAccess getAndUpdateRackerScopeAccessForClientId(Racker racker, Application client) {
        if(racker == null || client == null){
            throw new IllegalArgumentException("Argument(s) cannot be null.");
        }

        logger.debug("Get and Update ScopeAccess for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());

        RackerScopeAccess scopeAccess = scopeAccessService.getRackerScopeAccessByClientId(racker, client.getClientId());

        RackerScopeAccess scopeAccessToAdd = new RackerScopeAccess();

        if (scopeAccess == null) {
            // Auto-Provision Scope Access Objects for Rackers
            scopeAccessToAdd.setRackerId(racker.getRackerId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRcn());
        } else {
            scopeAccessToAdd.setRackerId(scopeAccess.getRackerId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRcn());
            scopeAccessToAdd.setRefreshTokenExp(scopeAccess.getRefreshTokenExp());
            scopeAccessToAdd.setRefreshTokenString(scopeAccess.getAccessTokenString());
            scopeAccessToAdd.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            scopeAccessToAdd.setAccessTokenString(scopeAccess.getAccessTokenString());
        }

        this.validateRackerHasRackerRole(racker, scopeAccessToAdd, client);

        DateTime current = new DateTime();
        DateTime accessExpiration = scopeAccessToAdd.getAccessTokenExp() == null ? new DateTime().minusDays(1)
                                                                       : new DateTime(scopeAccessToAdd.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            int expirationSeconds = scopeAccessService.getTokenExpirationSeconds(getDefaultRackerTokenExpirationSeconds());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(expirationSeconds).toDate());
        }

        DateTime refreshExpiration = scopeAccessToAdd.getRefreshTokenExp() == null ? new DateTime().minusDays(1)
                                                                         : new DateTime(scopeAccessToAdd.getRefreshTokenExp());

        if (refreshExpiration.isBefore(current)) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setRefreshTokenString(this.generateToken());
            scopeAccessToAdd.setRefreshTokenExp(current.plusYears(YEARS).toDate());
        }

        logger.debug("Updating Expirations for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());
        if (scopeAccess != null && !scopeAccess.isAccessTokenExpired(current)) {
            logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccess.getAccessTokenString(), scopeAccess.getAccessTokenExp());
            return scopeAccess;
        } else if (scopeAccess == null) {
            scopeAccessService.addUserScopeAccess(racker, scopeAccessToAdd);
        } else {
            scopeAccessService.deleteScopeAccess(scopeAccess);
            scopeAccessService.addUserScopeAccess(racker, scopeAccessToAdd);
        }

        logger.debug("Returning ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        return scopeAccessToAdd;
    }

    void validateRackerHasRackerRole(Racker racker, RackerScopeAccess scopeAccess, Application client) {
        List<TenantRole> tenantRolesForRacker = tenantService.getTenantRolesForUser(racker);
        boolean hasRackerRole = false;
        for (TenantRole tenantRole : tenantRolesForRacker) {
            if (tenantRole.getName().equals("Racker") && tenantRole.getClientId().equals(config.getString("idm.clientId"))) {
                hasRackerRole = true;
            }
        }
        if (!hasRackerRole) {
            for (ClientRole clientRole : applicationService.getClientRolesByClientId(config.getString("idm.clientId"))) {
                if (clientRole.getName().equals("Racker")) {
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

        UserAuthenticationResult result = userService.authenticate(username,
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

        if(!authenticated) {
            throw new NotAuthorizedException("Unable to authenticate user with credentials provided.");
        }

        Racker racker = userService.getRackerByRackerId(username);
        if (racker == null) {
            racker = new Racker();
            racker.setRackerId(username);
            this.userService.addRacker(racker);
            TenantRole rackerTenantRole = new TenantRole();
            rackerTenantRole.setRoleRsId(getRackerRoleRsId());
            rackerTenantRole.setClientId(getFoundationClientId());
            rackerTenantRole.setName("Racker");
            tenantService.addTenantRoleToUser(racker, rackerTenantRole);
        }

        return new UserAuthenticationResult(racker, authenticated);
    }

    private String getRackerRoleRsId() {
        return config.getString("cloudAuth.rackerRoleRsId");
    }

    private String getFoundationClientId() {
        return config.getString("idm.clientId");
    }

    void validateCredentials(final Credentials trParam) {
        ApiError error = null;

        if (trParam == null || trParam.getGrantType() == null) {
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

    String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds", 86400);
    }

    int getDefaultRackerTokenExpirationSeconds() {
        return config.getInt("token.rackerExpirationSeconds", 43200);
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
