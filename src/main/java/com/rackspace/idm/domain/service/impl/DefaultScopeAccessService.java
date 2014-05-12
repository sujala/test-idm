package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultScopeAccessService implements ScopeAccessService {

    public static final String NULL_ARGUMENT_PASSED_IN = "Null argument passed in.";
    public static final String ADDING_SCOPE_ACCESS = "Adding scopeAccess {}";
    public static final String ADDED_SCOPE_ACCESS = "Added scopeAccess {}";
    public static final String NULL_SCOPE_ACCESS_OBJECT_INSTANCE = "Null scope access object instance.";
    public static final String ERROR_DELETING_SCOPE_ACCESS = "Error deleting scope access %s - %s";

    public static final String LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME = "feature.optimize.impersonation.token.cleanup.enabled";
    public static final boolean LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_DEFAULT_VALUE = false;
    public static final String FEATURE_IGNORE_TOKEN_DELETE_FAILURE_PROP_NAME = "feature.ignore.token.delete.failure.enabled";
    public static final boolean FEATURE_IGNORE_TOKEN_DELETE_FAILURE_DEFAULT_VALUE = false;

    public static final String FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME = "feature.ignore.authentication.token.delete.failure.enabled";
    public static final boolean FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_DEFAULT_VALUE = true;
    public static final String FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME = "feature.authentication.token.delete.failure.stops.cleanup.enabled";
    public static final boolean FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_DEFAULT_VALUE = false;


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ScopeAccessDao scopeAccessDao;
    @Autowired
    private UserService userService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private EndpointService endpointService;
    @Autowired
    private AuthHeaderHelper authHeaderHelper;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private Configuration config;
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Override
    public List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user) {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        // First get the tenantRoles for the token
        List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);

        if (roles == null || roles.size() == 0) {
            return endpoints;
        }

        // Second get the tenants from each of those roles
        HashMap<Tenant, String> tenants = getTenants(roles);

        // Third get the endppoints for each tenant
        for (Tenant tenant : tenants.keySet()) {
            OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant, tenants.get(tenant), user.getRegion());
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    @Override
    public List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess token) {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        // First get the tenantRoles for the token
        List<TenantRole> roles = this.tenantService.getTenantRolesForScopeAccess(token);
        if (roles == null || roles.size() == 0) {
            return endpoints;
        }

        // Second get the tenants from each of those roles
        HashMap<Tenant, String> tenants = getTenants(roles);

        // Third get the endpoints for each tenant
        for (Tenant tenant : tenants.keySet()) {
            OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant, tenants.get(tenant), getRegion(token));
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    private HashMap<Tenant, String> getTenants(List<TenantRole> roles) {
        HashMap<Tenant, String> tenants = new HashMap<Tenant, String>();
        List<String> tenantIdList = new ArrayList<String>();
        for (TenantRole role : roles) {
            if (role.getTenantIds() != null) {
                for (String tenantId : role.getTenantIds()) {
                    if(!tenantIdList.contains(tenantId)){
                        tenantIdList.add(tenantId);
                        Tenant tenant = this.tenantService.getTenant(tenantId);
                        if (tenant != null) {
                            tenants.put(tenant, getOpenStackType(role));
                        }
                    }
                }
            }
        }
        return tenants;
    }

    private boolean optimizeImpersonatedTokenCleanup() {
        return config.getBoolean(LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME, LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_DEFAULT_VALUE);
    }

    private boolean ignoreTokenDeleteFailures() {
        return config.getBoolean(FEATURE_IGNORE_TOKEN_DELETE_FAILURE_PROP_NAME, FEATURE_IGNORE_TOKEN_DELETE_FAILURE_DEFAULT_VALUE);
    }

    private boolean ignoreAuthenticationTokenDeleteFailures() {
        return config.getBoolean(FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_DEFAULT_VALUE);
    }

    private boolean authenticationTokenDeleteFailuresStopsCleanup() {
        return config.getBoolean(FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_DEFAULT_VALUE);
    }

    private String getOpenStackType(TenantRole role) {
        String type = null;

        Application client = applicationService.getById(role.getClientId());
        if (client != null) {
            type = client.getOpenStackType();
            if (type != null) {
                if (type.equalsIgnoreCase("object-store")) {
                    type = "NAST";
                } else if (type.equalsIgnoreCase("compute")) {
                    type = "MOSSO";
                }
            }
        }
        return type;
    }

    private String getRegion(ScopeAccess token) {
        String region = null;
        BaseUser baseUser = userService.getUserByScopeAccess(token);
        if (baseUser != null && baseUser instanceof User) {
            User user = (User) baseUser;
            region = user.getRegion();
        }
        return region;
    }

    @Override
    public ImpersonatedScopeAccess addImpersonatedScopeAccess(BaseUser user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest) {
        String impersonatingUsername = impersonationRequest.getUser().getUsername();

        ImpersonatedScopeAccess mostRecent = (ImpersonatedScopeAccess) scopeAccessDao.getMostRecentImpersonatedScopeAccessForUser(user, impersonatingUsername);

        try {
            DateTime now = new DateTime();
            Iterable<ScopeAccess> scopeAccessToCheckForExpired;
            if (optimizeImpersonatedTokenCleanup()) {
                //only clean up those impersonated tokens for the impersonatingUsername
                scopeAccessToCheckForExpired = scopeAccessDao.getAllImpersonatedScopeAccessForUserOfUser(user, impersonatingUsername);
            } else {
                //clean up all impersonated tokens regardless of who it's for
                scopeAccessToCheckForExpired = scopeAccessDao.getAllImpersonatedScopeAccessForUser(user);
            }

            for (ScopeAccess scopeAccess : scopeAccessToCheckForExpired) {
                if (scopeAccess.isAccessTokenExpired(now)) {
                    scopeAccessDao.deleteScopeAccess(scopeAccess);
                }
            }
        } catch (RuntimeException ex) {
            if (ignoreTokenDeleteFailures()) {
                //if error deleting token, just log, and exit deletion routine
                logger.warn(String.format("Encountered an error deleting expired impersonated scope accesses for user '%s'. Exiting expired scope access cleanup routine for this user.", user.getUniqueId()), ex);
            } else {
                //just rethrow error as if it was never caught
                throw ex;
            }
        }

        ImpersonatedScopeAccess scopeAccessToAdd = new ImpersonatedScopeAccess();
        scopeAccessToAdd = setImpersonatedScopeAccess(user, impersonationRequest, scopeAccessToAdd);
        scopeAccessToAdd.setAccessTokenString(this.generateToken());
        scopeAccessToAdd.setImpersonatingToken(impersonatingToken);

        if (mostRecent == null) {
            if(user instanceof Racker){
                scopeAccessToAdd.setUsername(((Racker)user).getRackerId());
            } else {
                scopeAccessToAdd.setUsername(((User)user).getUsername());
            }
            scopeAccessToAdd.setClientId(clientId);
            scopeAccessToAdd.setImpersonatingUsername(impersonatingUsername);
        } else {
            scopeAccessToAdd.setUsername(mostRecent.getUsername());
            scopeAccessToAdd.setClientId(mostRecent.getClientId());
            scopeAccessToAdd.setImpersonatingUsername(mostRecent.getImpersonatingUsername());

            if (!mostRecent.isAccessTokenExpired(new DateTime())) {
                /*
                not sure why we create a new token with same token string, delete the old token, then add a the new token
                back. This is inefficient and introduces concurrency issue where if 2 threads call this method to add a new token
                for the same impersonatingUsername and
                there is an existing valid token for that impersonating username, each thread will first try to delete the
                same "valid" token - resulting in a failure. Even if the deletion failure was ignored, each thread would
                then try to add a new token with the same token string (the second request will fail since the token string is part of
                the scope access rdn). Ultimately, this means there's no point in making the deleteScopeAccess in this block
                fail safe since we'd just get an error later on anyway.

                Instead, we should introduce refresh windows or whatnot for impersonated tokens and return existing impersonated tokens
                rather than always create new ones. Within refresh windows new ones should be created with NEW token strings
                */
                scopeAccessToAdd.setAccessTokenString(mostRecent.getAccessTokenString());
                try {
                    scopeAccessDao.deleteScopeAccess(mostRecent);
                } catch (RuntimeException ex) {
                    //log the issue, but ultimately, we need to rethrow the error
                    logger.warn(String.format("Encountered an error deleting a valid impersonated token for user '%s' impersonating user '%s'.", user.getUniqueId(), impersonatingUsername), ex);
                    throw ex;
                }
            }
        }

        logger.info(ADDING_SCOPE_ACCESS, scopeAccessToAdd);
        scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccessToAdd);

        return scopeAccessToAdd;
    }

    ImpersonatedScopeAccess setImpersonatedScopeAccess(BaseUser caller, ImpersonationRequest impersonationRequest, ImpersonatedScopeAccess impersonatedScopeAccess) {
        validateExpireInElement(caller, impersonationRequest);

        int expirationSeconds;
        if (impersonationRequest.getExpireInSeconds() == null) {
            if (caller instanceof Racker) {
                impersonatedScopeAccess.setRackerId(((Racker) caller).getRackerId());
                expirationSeconds = getTokenExpirationSeconds(config.getInt("token.impersonatedByRackerDefaultSeconds"));
            } else {
                expirationSeconds = getTokenExpirationSeconds(config.getInt("token.impersonatedByServiceDefaultSeconds"));
            }
        } else {
            if (caller instanceof Racker) {
                impersonatedScopeAccess.setRackerId(((Racker) caller).getRackerId());
            }

            expirationSeconds = impersonationRequest.getExpireInSeconds();
        }

        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
        return impersonatedScopeAccess;
    }

    void validateExpireInElement(BaseUser caller, ImpersonationRequest impersonationRequest) {
        if(impersonationRequest==null || impersonationRequest.getExpireInSeconds()==null){
            return;
        }
        if (impersonationRequest.getExpireInSeconds() < 1) {
            throw new BadRequestException("Expire in element cannot be less than 1.");
        }
        if (caller instanceof Racker) {
            int rackerMax = config.getInt("token.impersonatedByRackerMaxSeconds");

            if (impersonationRequest.getExpireInSeconds() > rackerMax) {
                throw new BadRequestException("Expire in element cannot be more than " + rackerMax);
            }
        } else {
            int serviceMax = config.getInt("token.impersonatedByServiceMaxSeconds");

            if (impersonationRequest.getExpireInSeconds() > serviceMax) {
                throw new BadRequestException("Expire in element cannot be more than " + serviceMax);
            }
        }
    }

    @Override
    public void addUserScopeAccess(BaseUser user, ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info(ADDING_SCOPE_ACCESS, scopeAccess);
        this.scopeAccessDao.addScopeAccess(user, scopeAccess);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccess);
    }

    @Override
    public void addApplicationScopeAccess(Application application, ScopeAccess scopeAccess) {
        if(scopeAccess == null) {
            logger.error(NULL_ARGUMENT_PASSED_IN);
            throw new IllegalArgumentException(NULL_ARGUMENT_PASSED_IN);
        }
        logger.info(ADDING_SCOPE_ACCESS, scopeAccess);
        this.scopeAccessDao.addScopeAccess(application, scopeAccess);
        logger.info(ADDED_SCOPE_ACCESS);
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(accessTokenStr);
        if (scopeAccess != null && !(scopeAccess.isAccessTokenExpired(new DateTime()))) {
            authenticated = true;
            MDC.put(Audit.WHO, scopeAccess.getAuditContext());
        }
        logger.debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        logger.info("Deleting ScopeAccess {}", scopeAccess);
        if (scopeAccess == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        this.scopeAccessDao.deleteScopeAccess(scopeAccess);
        logger.info("Deleted ScopeAccess {}", scopeAccess);
    }

    @Override
    public void expireAccessToken(String tokenString) {
        logger.debug("Expiring access token {}", tokenString);
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }

        Date expireDate;
        expireDate =  scopeAccess.getAccessTokenExp();
        scopeAccess.setAccessTokenExpired();
        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        if(user != null && user instanceof User && !StringUtils.isBlank(scopeAccess.getAccessTokenString()) && !isExpired(expireDate)){
            logger.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost((User) user, tokenString);
        }
        logger.debug("Done expiring access token {}", tokenString);
    }

    @Override
    public void expireAllTokensForClient(String clientId) {
        logger.debug("Expiring all tokens for client {}", clientId);
        final Application client = this.applicationService.getById(clientId);
        if (client == null) {
            return;
        }

        for (ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(client)) {
            sa.setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(sa);
        }
        logger.debug("Done expiring all tokens for client {}", clientId);
    }

    @Override
    public void expireAllTokensForUser(String username) {
        logger.debug("Expiring all tokens for user {}", username);
        final User user = this.userService.getUser(username);
        if (user == null) {
            return;
        }

        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            Date expireDate =  sa.getAccessTokenExp();
            sa.setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(sa);
            if(!StringUtils.isBlank(sa.getAccessTokenString()) && !isExpired(expireDate)){
                logger.warn("Sending token feed to atom hopper.");
                atomHopperClient.asyncTokenPost(user, sa.getAccessTokenString());
            }
        }
        logger.debug("Done expiring all tokens for user {}", username);
    }

    private boolean isExpired(Date date) {
        if(date != null){
            return date.before(new Date());
        }else{
            return true;
        }
    }

    @Override
    public void expireAllTokensForUserById(String userId) {
        logger.debug("Expiring all tokens for user {}", userId);
        final User user = this.userService.getUserById(userId);
        if (user == null) {
            return;
        }

        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            Date expireDate =  sa.getAccessTokenExp();
            sa.setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(sa);
            if(!StringUtils.isBlank(sa.getAccessTokenString()) && !isExpired(expireDate)){
                logger.warn("Sending token feed to atom hopper.");
                atomHopperClient.asyncTokenPost(user, sa.getAccessTokenString());
            }
        }
        logger.debug("Done expiring all tokens for user {}", userId);
    }

    @Override
    public ScopeAccess getAccessTokenByAuthHeader(String authHeader) {
        logger.debug("Getting access token by auth header {}", authHeader);
        final String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        final ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByAccessToken(tokenStr);
        logger.debug("Done getting access token by auth header {}", authHeader);
        return scopeAccess;
    }

    @Override
    public ClientScopeAccess getApplicationScopeAccess(Application application) {
        logger.debug("Getting Client ScopeAccess by clientId", application.getClientId());
        ClientScopeAccess scopeAccess;
        scopeAccess = (ClientScopeAccess) this.scopeAccessDao.getMostRecentScopeAccessByClientId(application, application.getClientId());
        logger.debug("Got Client ScopeAccess {} by clientId {}", scopeAccess, application.getClientId());
        return scopeAccess;
    }

    @Override
    public ScopeAccess getMostRecentDirectScopeAccessForUserByClientId(User user, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccess sa = scopeAccessDao.getMostRecentScopeAccessByClientId(user, clientId);
        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public RackerScopeAccess getRackerScopeAccessByClientId(Racker racker, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        RackerScopeAccess scopeAccess;
        scopeAccess = (RackerScopeAccess) scopeAccessDao.getMostRecentScopeAccessByClientId(racker, clientId);
        logger.debug("Got Racker ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        logger.debug("Getting ScopeAccess by Access Token {}", accessToken);
        if (accessToken == null) {
            throw new NotFoundException("Invalid accessToken; Token cannot be null");
        }
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(accessToken);
        logger.debug("Got ScopeAccess {} by Access Token {}", scopeAccess, accessToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessForUser(User user) {
        logger.debug("Getting ScopeAccess by user id {}", user);
        if (user == null) {
            throw new NotFoundException("Invalid user id; user id cannot be null");
        }
        final ScopeAccess scopeAccess = this.scopeAccessDao.getMostRecentScopeAccessForUser(user);
        logger.debug("Got ScopeAccess {} by user id {}", scopeAccess, user);
        return scopeAccess;
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessListByUserId(String userId) {
        logger.debug("Getting ScopeAccess list by user id {}", userId);
        if (userId == null) {
            throw new NotFoundException("Invalid user id; user id cannot be null");
        }
        return this.scopeAccessDao.getScopeAccessesByUserId(userId);
    }

    @Override
    public ScopeAccess loadScopeAccessByAccessToken(String accessToken) {
        // Attempts to load the token. If the token is not found or expired
        // return a not found exception
        ScopeAccess scopeAccess = getScopeAccessByAccessToken(accessToken);
        if (scopeAccess == null) {
            String errorMsg = String.format("Token not found : %s", accessToken);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            String errorMsg = String.format("Token expired : %s", accessToken);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        logger.debug("Getting ScopeAccess by Refresh Token {}", refreshToken);
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByRefreshToken(refreshToken);
        logger.debug("Got ScopeAccess {} by Refresh Token {}", scopeAccess, refreshToken);
        return scopeAccess;
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesForUserByClientId(User user, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", user, clientId);
        return this.scopeAccessDao.getScopeAccessesByClientId(user, clientId);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesForApplicationByClientId(Application application, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", application, clientId);
        return this.scopeAccessDao.getScopeAccessesByClientId(application, clientId);
    }

    // Return UserScopeAccess from the directory, valid, expired or null
    @Override
    public UserScopeAccess getUserScopeAccessByClientId(User user, String clientId) {
        return (UserScopeAccess) scopeAccessDao.getMostRecentScopeAccessByClientId(user, clientId);
    }

    // Return UserScopeAccess from directory, refreshes expired
    @Override
    public UserScopeAccess getValidUserScopeAccessForClientId(User user, String clientId, List<String> authenticateBy) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        //if expired update with new token
        UserScopeAccess scopeAccess = updateExpiredUserScopeAccess(user, clientId, authenticateBy);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public RackerScopeAccess getValidRackerScopeAccessForClientId(Racker racker, String clientId, List<String> authenticatedBy) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        RackerScopeAccess scopeAccess = getRackerScopeAccessByClientId(racker, clientId);
        if (scopeAccess == null){
            int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthRackerTokenExpirationSeconds());
            scopeAccess = new RackerScopeAccess();
            scopeAccess.setClientId(clientId);
            scopeAccess.setRackerId(racker.getRackerId());
            scopeAccess.setAccessTokenString(generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccess.setAuthenticatedBy(authenticatedBy);
            scopeAccessDao.addScopeAccess(racker, scopeAccess);
        }
        //if expired update with new token
        scopeAccess = updateExpiredRackerScopeAccess(scopeAccess, authenticatedBy);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username,
                                                                                    String apiKey, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = userService.authenticateWithApiKey(username, apiKey);
        handleApiKeyUsernameAuthenticationFailure(username, result);

        return this.getValidUserScopeAccessForClientId((User) result.getUser(), clientId, authenticateBy(GlobalConstants.AUTHENTICATED_BY_APIKEY));
    }

    private List<String> authenticateBy(String type) {
        List<String> authenticatedBy = new ArrayList<String>();
        authenticatedBy.add(type);
        return authenticatedBy;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username,
                                                                              String password, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = this.userService.authenticate(username, password);
        handleAuthenticationFailure(username, result);

        return this.getValidUserScopeAccessForClientId((User) result.getUser(), clientId, authenticateBy(GlobalConstants.AUTHENTICATED_BY_PASSWORD));
    }

    @Override
    public void updateScopeAccess(ScopeAccess scopeAccess) {

        if (scopeAccess == null) {
            String errorMsg = String
                    .format(NULL_SCOPE_ACCESS_OBJECT_INSTANCE);
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.info("Updating ScopeAccess {}", scopeAccess);

        //The only uses of this function are expiring a token (not changing the value of the token itself)
        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        logger.info("Updated ScopeAccess {}", scopeAccess);
    }


    @Override
    public void deleteScopeAccessesForApplication(Application application, String clientId) {
        for (ScopeAccess sa : getScopeAccessesForApplicationByClientId(application, clientId)) {
            deleteScopeAccess(sa);
        }
    }

    @Override
    public void deleteScopeAccessesForUser(User user, String clientId) {
        for (ScopeAccess sa : getScopeAccessesForUserByClientId(user, clientId)) {
            deleteScopeAccess(sa);
        }
    }

    @Override
    public void deleteExpiredTokens(User user) {
        Iterable<ScopeAccess> tokenIterator = this.getScopeAccessListByUserId(user.getId());
        if (tokenIterator != null) {
            for (ScopeAccess token : tokenIterator) {
                if (token.isAccessTokenExpired(new DateTime())) {
                    this.deleteScopeAccess(token);
                }
            }
        }
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy) {
        Iterable<ScopeAccess> scopeAccessList = scopeAccessDao.getScopeAccesses(user);
        if (! scopeAccessList.iterator().hasNext()) {
            UserScopeAccess scopeAccess = provisionUserScopeAccess(user, clientId);
            if (authenticatedBy != null) {
                scopeAccess.setAuthenticatedBy(authenticatedBy);
            }
            this.scopeAccessDao.addScopeAccess(user, scopeAccess);
            return scopeAccess;
        }

        ScopeAccess mostRecent = scopeAccessDao.getMostRecentScopeAccessByClientId(user, clientId);


        boolean exitDeletionRoutineOnError = authenticationTokenDeleteFailuresStopsCleanup();
        boolean ignoreDeletionFailures = ignoreAuthenticationTokenDeleteFailures();
        for (ScopeAccess scopeAccess : scopeAccessList) {
            if (!scopeAccess.getAccessTokenString().equals(mostRecent.getAccessTokenString())) {
                if (scopeAccess.isAccessTokenExpired(new DateTime())) {
                    if (ignoreDeletionFailures) {
                        boolean success = deleteScopeAccessQuietly(scopeAccess);
                        if (!success && exitDeletionRoutineOnError) {
                            break;
                        }
                    }
                    else {
                        scopeAccessDao.deleteScopeAccess(scopeAccess);
                    }
                }
            }
        }
        if (authenticatedBy != null) {
            mostRecent.setAuthenticatedBy(authenticatedBy);
        }
        return updateExpiredUserScopeAccess((UserScopeAccess) mostRecent, false);
    }

    /**
     * Catches any exceptions, but notes whether an exception was actually caught
     * @param scopeAccess
     * @return false if exception was caught; true otherwise
     */
    private boolean deleteScopeAccessQuietly(ScopeAccess scopeAccess) {
        try {
            scopeAccessDao.deleteScopeAccess(scopeAccess);
            return true;
        } catch (Exception e) {
            logger.error(String.format("Error deleting expired scope access token '%s'. This error does not necessarily require manual intervention unless it is a frequent occurrence. It is for notification purposes only.", scopeAccess.getAccessTokenString()), e);
            return false;
        }
    }

    private UserScopeAccess provisionUserScopeAccess(User user, String clientId) {
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());

        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername(user.getUsername());
        userScopeAccess.setUserRsId(user.getId());
        userScopeAccess.setClientId(clientId);
        userScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
        userScopeAccess.setAccessTokenString(generateToken());

        return userScopeAccess;
    }

    @Override
    public int getTokenExpirationSeconds(int value) {
        Double entropy = getTokenEntropy();
        Integer min = (int)Math.floor(value * (1 - entropy));
        Integer max = (int)Math.ceil(value * (1 + entropy));
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        return scopeAccessDao.getClientIdForParent(scopeAccess);
    }

    @Override
    public String getUserIdForParent(ScopeAccess scopeAccess) {
        return scopeAccessDao.getUserIdForParent(scopeAccess);
    }

    @Override
    public UserScopeAccess createInstanceOfUserScopeAccess(User user, String clientId, String clientRCN) {
        UserScopeAccess usa = new UserScopeAccess();
        usa.setUsername(user.getUsername());
        usa.setUserRsId(user.getId());
        usa.setUserRCN(user.getCustomerId());
        usa.setClientId(clientId);
        usa.setClientRCN(clientRCN);
        usa.setAccessTokenString(UUID.randomUUID().toString().replace("-", ""));
        usa.setAccessTokenExp(new DateTime().toDate());
        return usa;
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess, boolean impersonated) {
        UserScopeAccess scopeAccessToAdd = new UserScopeAccess();
        scopeAccessToAdd.setClientId(scopeAccess.getClientId());
        scopeAccessToAdd.setClientRCN(scopeAccess.getClientRCN());
        scopeAccessToAdd.setUsername(scopeAccess.getUsername());
        scopeAccessToAdd.setUserRCN(scopeAccess.getUserRCN());
        scopeAccessToAdd.setUserRsId(scopeAccess.getUserRsId());
        scopeAccessToAdd.setAuthenticatedBy(scopeAccess.getAuthenticatedBy());

        int expirationSeconds;
        if (impersonated) {
            expirationSeconds = getTokenExpirationSeconds(getDefaultImpersonatedTokenExpirationSeconds());
        } else {
            expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());
        }
        scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
        BaseUser user = userService.getUserByScopeAccess(scopeAccess, false);

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());

            scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);

            if (ignoreAuthenticationTokenDeleteFailures()) {
                deleteScopeAccessQuietly(scopeAccess);
            }
            else {
                scopeAccessDao.deleteScopeAccess(scopeAccess);
            }

            return scopeAccessToAdd;
        } else if (scopeAccess.isAccessTokenWithinRefreshWindow(getRefreshTokenWindow())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);
            return scopeAccessToAdd;
        }
        return scopeAccess;
    }

    @Override
    public boolean isScopeAccessExpired(ScopeAccess scopeAccess) {
        return scopeAccess == null || scopeAccess.isAccessTokenExpired(new DateTime());
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesForUser(User user) {
        return scopeAccessDao.getScopeAccesses(user);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesForApplication(Application application) {
        return scopeAccessDao.getScopeAccesses(application);
    }

    @Override
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds", 86400);
    }

    int getDefaultCloudAuthRackerTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthRackerExpirationSeconds", 43200);
    }

    Double getTokenEntropy(){
        return config.getDouble("token.entropy");
    }

    int getRefreshTokenWindow() {
        return config.getInt("token.refreshWindowHours");
    }

    int getDefaultImpersonatedTokenExpirationSeconds() {
        return config.getInt("token.impersonatedExpirationSeconds");
    }

    void handleAuthenticationFailure(String username, final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.", username);
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage);
        }
    }

    void handleApiKeyUsernameAuthenticationFailure(String username, UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("Username or api key is invalid.", username);
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage);
        }
    }

    private RackerScopeAccess updateExpiredRackerScopeAccess(RackerScopeAccess scopeAccess, List<String> authenticatedBy) {
        RackerScopeAccess scopeAccessToAdd = new RackerScopeAccess();
        scopeAccessToAdd.setRackerId(scopeAccess.getRackerId());
        scopeAccessToAdd.setRefreshTokenString(scopeAccess.getRefreshTokenString());
        scopeAccessToAdd.setRefreshTokenExp(scopeAccess.getRefreshTokenExp());
        scopeAccessToAdd.setClientId(scopeAccess.getClientId());
        scopeAccessToAdd.setClientRCN(scopeAccess.getClientRCN());
        scopeAccessToAdd.setAuthenticatedBy(authenticatedBy);

        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthRackerTokenExpirationSeconds());
        Racker racker = (Racker) userService.getUserByScopeAccess(scopeAccess);

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccessDao.addScopeAccess(racker, scopeAccessToAdd);
            if (ignoreAuthenticationTokenDeleteFailures()) {
                deleteScopeAccessQuietly(scopeAccess);
            }
            else {
                scopeAccessDao.deleteScopeAccess(scopeAccess);
            }
            return scopeAccessToAdd;
        } else if (scopeAccess.isAccessTokenWithinRefreshWindow(getRefreshTokenWindow())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccessDao.addScopeAccess(racker, scopeAccessToAdd);
            return scopeAccessToAdd;
        }
        return scopeAccess;
    }
}
