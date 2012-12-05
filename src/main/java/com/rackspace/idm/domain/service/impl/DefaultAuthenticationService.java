package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.RSAClient;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.validation.groups.Default;
import java.sql.DataTruncation;
import java.util.List;
import java.util.UUID;

import static com.rackspace.idm.domain.entity.OAuthGrantType.*;

@Component
public class DefaultAuthenticationService implements AuthenticationService {

    public static final int YEARS = 100;

    @Autowired
    private ApplicationDao clientDao;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private Configuration config;
    @Autowired
    private UserDao userDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private InputValidator inputValidator;

    @Autowired
    private RSAClient rsaClient;

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
    public void setApplicationDao(ApplicationDao applicationDao) {
        this.clientDao = applicationDao;
    }

    @Override
    public void setConfig(Configuration appConfig) {
        this.config = appConfig;
    }

    @Override
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void setCustomerDao(CustomerDao customerDao) {
        this.customerDao = customerDao;
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
    AuthData getAuthData(ScopeAccess scopeAccess) {
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
    void setClient(ScopeAccess scopeAccess, AuthData authData) {
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

            DateTime rotationDate = getUserPasswordExpirationDate(uaResult.getUser().getUsername());
            if (rotationDate != null && rotationDate.isBefore(currentTime)) {
                PasswordResetScopeAccess prsa = this.scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(uaResult.getUser());
                prsa.setUserPasswordExpirationDate(rotationDate);
                return prsa;
            }

            UserScopeAccess usa = this.getAndUpdateUserScopeAccessForClientId(uaResult.getUser(), caResult.getClient());
            usa.setUserPasswordExpirationDate(rotationDate);
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
                String username = ((UserScopeAccess) scopeAccess).getUsername();
                String userId = ((UserScopeAccess) scopeAccess).getUserRsId();
                User user = this.userDao.getUserById(userId);
                if (user == null || user.isDisabled()) {
                    String errMsg = String.format("User %S is disabled", username);
                    logger.info(errMsg);
                    throw new UserDisabledException(errMsg);
                }
                ((UserScopeAccess) scopeAccess).setUserRsId(userId);
                ((UserScopeAccess) scopeAccess).setUsername(username);
            }

            scopeAccessToAdd.setAccessTokenString(this
                    .generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime()
                    .plusSeconds(this.getDefaultTokenExpirationSeconds())
                    .toDate());

            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
            scopeAccessToAdd.setClientId(caResult.getClient().getClientId());
            scopeAccessToAdd.setClientRCN(caResult.getClient().getRCN());

            String parentUniqueId = null;
            try {
                parentUniqueId = new DN(scopeAccess.getUniqueId()).getParentString();
            } catch (LDAPException e) {
                throw new IllegalStateException("ScopeAccess has an invalid dn");
            }

            this.scopeAccessService.addDirectScopeAccess(parentUniqueId, scopeAccessToAdd);

            this.scopeAccessService.deleteScopeAccess(scopeAccess);
            return scopeAccessToAdd;
        }

        if (CLIENT_CREDENTIALS == grantType) {
            return this.getAndUpdateClientScopeAccessForClientId(caResult.getClient());
        }

        if (AUTHORIZATION_CODE == grantType) {
            DelegatedClientScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAuthCode(trParam.getAuthorizationCode());
            DelegatedClientScopeAccess scopeAccessToAdd = new DelegatedClientScopeAccess();
            if (scopeAccess == null
                    || scopeAccess.isAuthorizationCodeExpired(currentTime)
                    || !scopeAccess.getClientId().equalsIgnoreCase(
                    caResult.getClient().getClientId())) {
                final String msg = String.format("Unauthorized Authorization Code: %s", trParam.getAuthorizationCode());
                logger.warn(msg);
                throw new NotAuthenticatedException(msg);
            }

            scopeAccessToAdd.setRefreshTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(currentTime.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
            scopeAccessToAdd.setClientId(caResult.getClient().getClientId());
            scopeAccessToAdd.setClientRCN(caResult.getClient().getRCN());
            scopeAccessToAdd.setAuthCode(null);
            scopeAccessToAdd.setAuthCodeExp(null);

            String parentUniqueId = null;
            try {
                parentUniqueId = new DN(scopeAccess.getUniqueId()).getParentString();
            } catch (LDAPException e) {
                throw new IllegalStateException("ScopeAccess has an invalid dn");
            }

            this.scopeAccessService.addDirectScopeAccess(parentUniqueId, scopeAccessToAdd);

            this.scopeAccessService.deleteScopeAccess(scopeAccess);
            return scopeAccessToAdd;
        }

        final String message = String.format("Unsupported GrantType: %s",
                grantType);
        logger.warn(message);
        throw new NotAuthenticatedException(message);
    }

    UserScopeAccess getAndUpdateUserScopeAccessForClientId(User user, Application client) {
        if(user == null || client == null){
            throw new IllegalArgumentException("Argument(s) cannot be null.");
        }

        logger.debug("Get and Update ScopeAccess for User: {} and ClientId: {}", user.getUsername(), client.getClientId());

        UserScopeAccess scopeAccess = scopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), client.getClientId());
        UserScopeAccess scopeAccessToAdd = new UserScopeAccess();

        if (scopeAccess == null) {
            // provision scopeAccess with defaults
            scopeAccessToAdd.setUsername(user.getUsername());
            scopeAccessToAdd.setUserRsId(user.getId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRCN());
        } else {
            scopeAccessToAdd.setUsername(scopeAccess.getUsername());
            scopeAccessToAdd.setUserRsId(scopeAccess.getUserRsId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRCN());
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
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
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
            scopeAccessService.addDirectScopeAccess(user.getUniqueId(), scopeAccessToAdd);
        } else {
            scopeAccessService.addDirectScopeAccess(user.getUniqueId(), scopeAccessToAdd);
            scopeAccessService.deleteScopeAccessByDn(scopeAccess.getUniqueId());
        }

        logger.debug("Returning ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        return scopeAccessToAdd;
    }

    ClientScopeAccess getAndUpdateClientScopeAccessForClientId(Application client) {
        if(client == null){
            throw new IllegalArgumentException("Argument cannot be null.");
        }

        logger.debug("Get and Update Client ScopeAccess for ClientId: {}", client.getClientId());

        ClientScopeAccess scopeAccess = this.scopeAccessService.getClientScopeAccessForClientId(client.getUniqueId(), client.getClientId());
        ClientScopeAccess scopeAccessToAdd = new ClientScopeAccess();

        if (scopeAccess == null) {
            scopeAccessToAdd.setClientRCN(client.getRCN());
            scopeAccessToAdd.setClientId(client.getClientId());
            logger.debug("Creating ScopeAccess for Client: {} and ClientId: {}", client.getClientId(), client.getClientId());
        } else {
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRCN());
            scopeAccessToAdd.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
        }

        DateTime current = new DateTime();
        DateTime accessExpiration = scopeAccessToAdd.getAccessTokenExp() == null ? new DateTime().minusDays(1)
                                                                                : new DateTime(scopeAccessToAdd.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
            logger.debug("Updating ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        }

        if (scopeAccess != null && !scopeAccess.isAccessTokenExpired(current)) {
            logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccess.getAccessTokenString(), scopeAccess.getAccessTokenExp());
            return scopeAccess;
        } else if (scopeAccess == null) {
            scopeAccessService.addDirectScopeAccess(client.getUniqueId(), scopeAccessToAdd);
        } else {
            scopeAccessService.addDirectScopeAccess(client.getUniqueId(), scopeAccessToAdd);
            scopeAccessService.deleteScopeAccessByDn(scopeAccess.getUniqueId());
        }

        logger.debug("Found ScopeAccess: {} Expiration {}", scopeAccessToAdd.getAccessTokenString(), scopeAccessToAdd.getAccessTokenExp());
        return scopeAccessToAdd;
    }

    RackerScopeAccess getAndUpdateRackerScopeAccessForClientId(Racker racker, Application client) {
        if(racker == null || client == null){
            throw new IllegalArgumentException("Argument(s) cannot be null.");
        }

        logger.debug("Get and Update ScopeAccess for Racker: {} and ClientId: {}", racker.getRackerId(), client.getClientId());

        RackerScopeAccess scopeAccess = scopeAccessService.getRackerScopeAccessForClientId(racker.getUniqueId(), client.getClientId());
        RackerScopeAccess scopeAccessToAdd = new RackerScopeAccess();

        if (scopeAccess == null) {
            // Auto-Provision Scope Access Objects for Rackers
            scopeAccessToAdd.setRackerId(racker.getRackerId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRCN());
        } else {
            scopeAccessToAdd.setRackerId(scopeAccess.getRackerId());
            scopeAccessToAdd.setClientId(client.getClientId());
            scopeAccessToAdd.setClientRCN(client.getRCN());
            scopeAccessToAdd.setRefreshTokenExp(scopeAccess.getRefreshTokenExp());
            scopeAccessToAdd.setRefreshTokenString(scopeAccess.getAccessTokenString());
            scopeAccessToAdd.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            scopeAccessToAdd.setAccessTokenString(scopeAccess.getAccessTokenString());
        }

        this.validateRackerHasRackerRole(racker, scopeAccess, client);

        DateTime current = new DateTime();
        DateTime accessExpiration = scopeAccessToAdd.getAccessTokenExp() == null ? new DateTime().minusDays(1)
                                                                       : new DateTime(scopeAccessToAdd.getAccessTokenExp());

        if (accessExpiration.isBefore(current)) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(current.plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
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
            scopeAccessService.addDirectScopeAccess(racker.getUniqueId(), scopeAccessToAdd);
        } else {
            scopeAccessService.addDirectScopeAccess(racker.getUniqueId(), scopeAccessToAdd);
            scopeAccessService.deleteScopeAccessByDn(scopeAccess.getUniqueId());
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
            List<ClientRole> clientRoles = clientDao.getClientRolesByClientId(config.getString("idm.clientId"));
            for (ClientRole clientRole : clientRoles) {
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

        if(!authenticated) {
            throw new NotAuthorizedException("Unable to authenticate user with credentials provided.");
        }

        Racker racker = userDao.getRackerByRackerId(username);
        if (racker == null) {
            racker = new Racker();
            racker.setRackerId(username);
            this.userDao.addRacker(racker);
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

    DateTime getUserPasswordExpirationDate(String userName) {
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
