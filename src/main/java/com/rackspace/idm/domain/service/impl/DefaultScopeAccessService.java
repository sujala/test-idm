package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
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

    public static final String TOKEN_IMPERSONATED_BY_SERVICE_DEFAULT_SECONDS_PROP_NAME = "token.impersonatedByServiceDefaultSeconds";
    public static final String TOKEN_IMPERSONATED_BY_RACKER_DEFAULT_SECONDS_PROP_NAME = "token.impersonatedByRackerDefaultSeconds";
    public static final String TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME = "token.impersonatedByRackerMaxSeconds";
    public static final String TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME = "token.impersonatedByServiceMaxSeconds";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("scopeAccessDao")
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

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;
    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired
    @Qualifier("tokenRevocationService")
    private TokenRevocationService tokenRevocationService;

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
        final List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        // First get the tenantRoles for the token
        final List<TenantRole> roles = this.tenantService.getTenantRolesForScopeAccess(token);
        if (roles == null || roles.size() == 0) {
            return endpoints;
        }

        // Second get the tenants from each of those roles
        final HashMap<Tenant, String> tenants = getTenants(roles);

        // Third get the endpoints for each tenant
        final String region = getRegion(token);
        for (Tenant tenant : tenants.keySet()) {
            final OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant, tenants.get(tenant), region);
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
        BaseUser baseUser = userService.getUserByScopeAccess(token, false);
        if (baseUser != null && EndUser.class.isAssignableFrom(baseUser.getClass())) {
            EndUser user = (EndUser) baseUser;
            region = user.getRegion();
        }
        return region;
    }

    /**
     * Creates and saves a new impersonation token to use against the user and cleans up any expired scope accesses.
     */
    @Override
    public ImpersonatedScopeAccess processImpersonatedScopeAccessRequest(BaseUser impersonator, EndUser userBeingImpersonated, ImpersonationRequest impersonationRequest, ImpersonatorType impersonatorType, List<String> impersonatorAuthByMethods) {
        DateTime requestInstant = new DateTime();

        DateTime desiredImpersonationTokenExpiration = calculateDesiredImpersonationTokenExpiration(impersonationRequest, impersonatorType, requestInstant);

        UserScopeAccess userTokenForImpersonation = getUserScopeAccessForImpersonationRequest(impersonator, userBeingImpersonated, desiredImpersonationTokenExpiration, requestInstant);

        //get the most recent scope access prior to cleaning up since the most recent could be expired (and will be removed by subsequent clean up code)
        ImpersonatedScopeAccess mostRecent;
        if(userBeingImpersonated instanceof FederatedUser) {
            //impersonation tokens impersonating federated users should always have the rsImpersonatingRsId attribute
            //Note: it is possible that the federated user's token that is being impersonated could have been deleted.
            //    This is not a major concern because the impersonating token will then become invalid and eventually be
            //    cleaned up.
            mostRecent = (ImpersonatedScopeAccess) scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(impersonator, userBeingImpersonated.getId(), impersonatorAuthByMethods);
        } else {
            /*
            The user is a provisioned user. This means that there might be existing impersonation tokens in the directory that do not have the
            rsImpersonatingRsId attribute and only the impersonated user's username (attribute impersonatingUsername). We will use the following
            steps to try to find the most recent impersonating scope access for the user.

            1) Search for an impersonating scope access with an rsImpersonatingRsId equal to the impersonated user's rsId
            2) If scope access tokens are found, find the most recent from them
            3) If no scope access is found, we will need to search for scope access tokens that match the impersonated user's username.
            This is complicated by the fact that federated users can also have the same username.
                a) Search for impersonating scope access tokens with no rsImpersonatingRsId attribute and impersonatingUsername equal to
                the impersonated user's username. By limiting our search for tokens that do not have the rsImpersonatingRsId attribute
                we prevent finding impersonation tokens for federated users.
                b) Find the most recent token from this list of tokens
             */

            //1) Search for an impersonating scope access with an rsImpersonatingRsId equal to the user being impersonated rsId
            //2) If scope access tokens are found, find the most recent from them
            mostRecent = (ImpersonatedScopeAccess) scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(impersonator, userBeingImpersonated.getId(), impersonatorAuthByMethods);
            if(mostRecent == null) {
                //3) Legacy - No scope access found, fall back to searching by username and not trying to match on auth by
                //TODO: delete this in 2.11+ codebase as by then all tokens will have the user rsid and auth by set
                mostRecent = (ImpersonatedScopeAccess) scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserOfUser(impersonator, userBeingImpersonated.getUsername());
            }
        }
        cleanUpImpersonatorsExpiredImpersonationTokens(impersonator, userBeingImpersonated);

        /*
        I don't understand this logic at all.

        1. Why would we copy the username/clientid/impersonatingusername from the old token
          rather than just leave the values set in the new token? Theoretically the old values could be different (e.g. changed username, etc)
          but not sure why we wouldn't want to use the most recent information over the old.

        2. I am not sure why we create a new token with same token string, delete the old token, then add a the new token
        back. This is inefficient and introduces concurrency issue where if 2 threads call this method to add a new token
        for the same impersonatingUsername and there is an existing valid token for that impersonating username,
        each thread will first try to delete the
        same "valid" token - resulting in a failure. Even if the deletion failure was ignored, each thread would
        then try to add a new token with the same token string (the second request will fail since the token string is part of
        the scope access rdn). Ultimately, this means there's no point in making the deleteScopeAccess in this block
        fail safe since we'd just get an error on the add anyway.

        If the purpose is to limit the number of valid impersonation tokens against a given user, we should introduce refresh
        windows or whatnot for impersonated tokens and return existing impersonated tokens
        rather than always creating a new one and deleting the old. Within refresh windows new ones should be created with NEW token strings.

        However, all this is legacy logic so I'm not going to change it without an explicit story.
        */
        ImpersonatedScopeAccess scopeAccessToUse;
        if(!(userBeingImpersonated instanceof FederatedUser)) {
            //create a shiny new token
            ImpersonatedScopeAccess scopeAccessToAdd = createImpersonatedScopeAccess(impersonator, userBeingImpersonated, userTokenForImpersonation, desiredImpersonationTokenExpiration, impersonatorAuthByMethods);

            if (mostRecent != null) {
                if(impersonator instanceof Racker) {
                    scopeAccessToAdd.setUserRsId(((Racker) impersonator).getRackerId());
                } else if(impersonator instanceof EndUser) {
                    scopeAccessToAdd.setUserRsId(((EndUser) impersonator).getId());
                }
                scopeAccessToAdd.setUsername(mostRecent.getUsername());
                scopeAccessToAdd.setClientId(mostRecent.getClientId());
                scopeAccessToAdd.setImpersonatingUsername(mostRecent.getImpersonatingUsername());
                if (!mostRecent.isAccessTokenExpired(new DateTime())) {
                    scopeAccessToAdd.setAccessTokenString(mostRecent.getAccessTokenString());
                    try {
                        scopeAccessDao.deleteScopeAccess(mostRecent);
                    } catch (RuntimeException ex) {
                        //log the issue, but ultimately, we need to rethrow the error
                        logger.warn(String.format("Encountered an error deleting a valid impersonated token for user '%s' impersonating user '%s'.",
                                impersonator.getUniqueId(), userBeingImpersonated.getUsername()), ex);
                        throw ex;
                    }
                }
            }

            logger.info(ADDING_SCOPE_ACCESS, scopeAccessToAdd);
            scopeAccessDao.addScopeAccess(impersonator, scopeAccessToAdd);
            logger.info(ADDED_SCOPE_ACCESS, scopeAccessToAdd);
            scopeAccessToUse = scopeAccessToAdd;
        } else {
            //federated users
            if(mostRecent == null || mostRecent.isAccessTokenExpired(new DateTime())
                    || desiredImpersonationTokenExpiration.isAfter(new DateTime(mostRecent.getAccessTokenExp()))) {
                //create a new impersonation token if the most recent impersonation token for this user is expired or
                //will expired before the requested time
                ImpersonatedScopeAccess scopeAccessToAdd = createImpersonatedScopeAccess(impersonator, userBeingImpersonated, userTokenForImpersonation, desiredImpersonationTokenExpiration, impersonatorAuthByMethods);

                logger.info(ADDING_SCOPE_ACCESS, scopeAccessToAdd);
                scopeAccessDao.addScopeAccess(impersonator, scopeAccessToAdd);
                logger.info(ADDED_SCOPE_ACCESS, scopeAccessToAdd);
                scopeAccessToUse = scopeAccessToAdd;
            } else {
                //the most recent scope access will expire on or after the requested time. Just use that token, no reason to create another
                scopeAccessToUse = mostRecent;
            }
        }

        return scopeAccessToUse;
    }

    private void cleanUpImpersonatorsExpiredImpersonationTokens(BaseUser impersonator, EndUser userBeingImpersonated) {
        /*
        We will need to clean up scope access tokens in two ways.
        1) Clean up all scope access tokens with impersonatedRsId equal to impersonated user's rsId
        2) Clean up all scope access tokens with impersonatedUsername equal to impersonated user's username that do not
        have the rsImpersonatingRsId attribute.
         */

        Iterable<ScopeAccess> scopeAccessToCheckForExpired;
        try {
            DateTime now = new DateTime();
            if (optimizeImpersonatedTokenCleanup()) {
                scopeAccessToCheckForExpired = new ArrayList<ScopeAccess>();
                //1) Clean up all scope access tokens with impersonatedRsId equal to impersonated user's rsId
                Iterable<ScopeAccess> scopeAccessesByUserRsId = scopeAccessDao.getAllImpersonatedScopeAccessForUserOfUserByRsId(impersonator, userBeingImpersonated.getId());
                for(ScopeAccess sa : scopeAccessesByUserRsId) {
                    ((List) scopeAccessToCheckForExpired).add(sa);
                }

                //2) Clean up all scope access tokens with impersonatedUsername equal to impersonated user's username.
                Iterable<ScopeAccess> scopeAccessesByUsername = scopeAccessDao.getAllImpersonatedScopeAccessForUserOfUserByUsername(impersonator, userBeingImpersonated.getUsername());
                for(ScopeAccess sa : scopeAccessesByUsername) {
                    ((List) scopeAccessToCheckForExpired).add(sa);
                }
            } else {
                //The optimizeImpersonatedTokenCleanup is the preferred mechanism. This cleanup block that removes all
                // expired imp tokens regardless of the user being impersonated is here for legacy reasons as part of a
                // feature flag. When the feature flag is removed, this block should be removed and the optimization always
                // used.
                scopeAccessToCheckForExpired = scopeAccessDao.getAllImpersonatedScopeAccessForUser(impersonator);
            }

            for (ScopeAccess scopeAccess : scopeAccessToCheckForExpired) {
                if (scopeAccess.isAccessTokenExpired(now)) {
                    scopeAccessDao.deleteScopeAccess(scopeAccess);
                }
            }
        } catch (RuntimeException ex) {
            if (ignoreTokenDeleteFailures()) {
                //if error deleting token, just log, and exit deletion routine
                logger.warn(String.format("Encountered an error deleting expired impersonated scope accesses for user '%s'. Exiting expired scope access cleanup routine for this user.", impersonator.getUniqueId()), ex);
            } else {
                //just rethrow error as if it was never caught
                throw ex;
            }
        }
    }

    /**
     * Create a new impersonated scope access based on the passed in information
     *
     * @param impersonator
     * @param userTokenForImpersonation
     * @param impersonationTokenExpirationDate
     * @return
     */
    private ImpersonatedScopeAccess createImpersonatedScopeAccess(BaseUser impersonator, EndUser userBeingImpersonated, UserScopeAccess userTokenForImpersonation, DateTime impersonationTokenExpirationDate, List<String> impersonatorAuthByMethods) {
        String clientId = getCloudAuthClientId();

        ImpersonatedScopeAccess newImpersonatedScopeAccess = new ImpersonatedScopeAccess();
        if (impersonator instanceof Racker) {
            newImpersonatedScopeAccess.setRackerId(((Racker) impersonator).getRackerId());
            newImpersonatedScopeAccess.setUsername(((Racker) impersonator).getRackerId());
        } else {
            //federated users are not allowed to impersonate so safe to cast to user at this point
            newImpersonatedScopeAccess.setUserRsId(((User) impersonator).getId());
            newImpersonatedScopeAccess.setUsername(((User) impersonator).getUsername());
        }
        newImpersonatedScopeAccess.setClientId(clientId);
        newImpersonatedScopeAccess.setAccessTokenString(this.generateToken());
        newImpersonatedScopeAccess.setImpersonatingToken(userTokenForImpersonation.getAccessTokenString());
        newImpersonatedScopeAccess.setAccessTokenExp(impersonationTokenExpirationDate.toDate());
        newImpersonatedScopeAccess.setImpersonatingUsername(userBeingImpersonated.getUsername());
        newImpersonatedScopeAccess.setRsImpersonatingRsId(userTokenForImpersonation.getUserRsId());
        newImpersonatedScopeAccess.setAuthenticatedBy(new ArrayList<String>(impersonatorAuthByMethods));

        return newImpersonatedScopeAccess;
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
        tokenRevocationService.revokeToken(tokenString);
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

        tokenRevocationService.revokeAllTokensForBaseUser(user);

        logger.debug("Done expiring all tokens for user {}", username);
    }

    @Override
    public void expireAllTokensForUserById(String userId) {
        logger.debug("Expiring all tokens for user {}", userId);
        final EndUser user = identityUserService.getEndUserById(userId);
        if (user == null) {
            return;
        }

        tokenRevocationService.revokeAllTokensForBaseUser(user);

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
    public void deleteExpiredTokens(EndUser user) {
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
    public void deleteExpiredTokensQuietly(EndUser user) {
        try {
            Iterable<ScopeAccess> tokenIterator = this.getScopeAccessListByUserId(user.getId());
            DateTime now = new DateTime();
            if (tokenIterator != null) {
                for (ScopeAccess token : tokenIterator) {
                    if (token.isAccessTokenExpired(now)) {
                        try {
                            this.deleteScopeAccess(token);
                        } catch (Exception e) {
                            String tokenStr = token.getAccessTokenString();
                            String maskedToken = tokenStr != null ? tokenStr.substring(Math.max(tokenStr.length() - 4, 0)) : "";
                            logger.warn(String.format("Error deleting user '%s' expired token ending in '%s'.", user.getAuditContext(), maskedToken), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("Error deleting expired tokens from user '%s'", user.getAuditContext(), e));
        }
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy) {

        Iterable<ScopeAccess> scopeAccessList = scopeAccessDao.getScopeAccesses(user);
        ScopeAccess mostRecent = scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(user, clientId, authenticatedBy);

        if (! scopeAccessList.iterator().hasNext() || mostRecent == null) {
            UserScopeAccess scopeAccess = provisionUserScopeAccess(user, clientId);
            if (authenticatedBy != null) {
                scopeAccess.setAuthenticatedBy(authenticatedBy);
            }
            this.scopeAccessDao.addScopeAccess(user, scopeAccess);
            deleteExpiredScopeAccessesExceptForMostRecent(scopeAccessList, scopeAccess);
            return scopeAccess;
        }

        deleteExpiredScopeAccessesExceptForMostRecent(scopeAccessList, mostRecent);

        return updateExpiredUserScopeAccess((UserScopeAccess) mostRecent);
    }

    @Override
    public ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, int expirationSeconds, String scope) {

        ScopeAccess scopeAccessToAdd = null;

        if (user instanceof User) {
            User provisionedUser = (User)user;
            UserScopeAccess userScopeAccess = new UserScopeAccess();
            userScopeAccess.setUsername(provisionedUser.getUsername());
            userScopeAccess.setUserRsId(provisionedUser.getId());
            userScopeAccess.setClientId(clientId);
            userScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            userScopeAccess.setAccessTokenString(generateToken());
            userScopeAccess.getAuthenticatedBy().addAll(authenticatedBy);
            userScopeAccess.setScope(scope);
            scopeAccessToAdd = userScopeAccess;
        } else {
            // We can fully implement this method for Federated Users and Rackers, but
            // for now its just going to be an unsupported method.
            throw new UnsupportedOperationException();
        }

        this.scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);

        return scopeAccessToAdd;
    }

    private void deleteExpiredScopeAccessesExceptForMostRecent(Iterable<ScopeAccess> scopeAccessList, ScopeAccess mostRecent) {
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
    public UserScopeAccess createInstanceOfUserScopeAccess(EndUser user, String clientId, String clientRCN) {
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

    //TODO: Refactor this. Method doesn't really reflect use, not really required anymore since impersonation aspect taken out. Combine with only caller.
    private UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess) {
        UserScopeAccess scopeAccessToAdd = new UserScopeAccess();
        scopeAccessToAdd.setClientId(scopeAccess.getClientId());
        scopeAccessToAdd.setClientRCN(scopeAccess.getClientRCN());
        scopeAccessToAdd.setUsername(scopeAccess.getUsername()); //only here to be backward compatible with 2.9.x tokens
        scopeAccessToAdd.setUserRCN(scopeAccess.getUserRCN());
        scopeAccessToAdd.setUserRsId(scopeAccess.getUserRsId());
        scopeAccessToAdd.setAuthenticatedBy(scopeAccess.getAuthenticatedBy());

        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());
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
    public boolean isSetupMfaScopedToken(ScopeAccess scopeAccess) {
        return scopeAccess == null ? false : GlobalConstants.SETUP_MFA_SCOPE.equals(scopeAccess.getScope());
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

    private DateTime calculateDesiredImpersonationTokenExpiration(ImpersonationRequest impersonationRequest, ImpersonatorType impersonatorType, DateTime requestInstant) {
        Integer desiredImpersonationTokenExpirationInSeconds = impersonationRequest.getExpireInSeconds();
        if (desiredImpersonationTokenExpirationInSeconds == null) {
            if (impersonatorType == ImpersonatorType.RACKER) {
                desiredImpersonationTokenExpirationInSeconds = config.getInt(TOKEN_IMPERSONATED_BY_RACKER_DEFAULT_SECONDS_PROP_NAME);
            } else {
                desiredImpersonationTokenExpirationInSeconds = config.getInt(TOKEN_IMPERSONATED_BY_SERVICE_DEFAULT_SECONDS_PROP_NAME);
            }
        }
        DateTime desiredImpersonationTokenExpiration = requestInstant.plusSeconds(desiredImpersonationTokenExpirationInSeconds);

        return desiredImpersonationTokenExpiration;
    }

    /**
     * Retrieve the user token to which the impersonation token will be linked. Upon creation, impersonated token
     * lifetimes must be set to the requested (or default) expiration. If this
     * is greater than the remaining lifetime of the user's token, a new user
     * token must be generated with the lifetime set to the default lifetime of a user token.
     */
    private UserScopeAccess getUserScopeAccessForImpersonationRequest(BaseUser impersonator, EndUser user, DateTime desiredImpersonationTokenExpiration, DateTime requestInstant) {
        UserScopeAccess scopeAccessForImpersonation = null;

        //format of underlying user token is based on the impersonator, not the user being impersonated
        TokenFormat tFormat = tokenFormatSelector.formatForNewToken(impersonator);

        if (tFormat == TokenFormat.UUID) {
            //get the latest user scope access created solely for impersonation for this user
            UserScopeAccess latestScopeAccessForUser = (UserScopeAccess) scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(user, getCloudAuthClientId(), Arrays.asList(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION));

            //get the expiration time of this token. If user doesn't have current token, mark yesterday as the expiration time
            DateTime curUserTokenExpiration = latestScopeAccessForUser != null ? new DateTime(latestScopeAccessForUser.getAccessTokenExp()) : requestInstant.minusDays(1);

            if (curUserTokenExpiration.isBefore(requestInstant) || curUserTokenExpiration.isBefore(desiredImpersonationTokenExpiration)) {
                //existing user token is expired or it will expire before the requested
                // impersonation expiration so we must create a new token for the user.
                scopeAccessForImpersonation = createNewUserTokenForImpersonation(user);
            } else {
                scopeAccessForImpersonation = latestScopeAccessForUser;
            }
        } else if (tFormat == TokenFormat.AE) {
            //always create a new user token
            scopeAccessForImpersonation = new UserScopeAccess();
            scopeAccessForImpersonation.setAccessTokenExp(desiredImpersonationTokenExpiration.toDate());
            scopeAccessForImpersonation.setUserRsId(user.getId());
            scopeAccessForImpersonation.setClientId(getCloudAuthClientId());
            scopeAccessForImpersonation.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION);
            if (aeTokenService.supportsCreatingTokenFor(user, scopeAccessForImpersonation)) {
                aeTokenService.marshallTokenForUser(user, scopeAccessForImpersonation); //populates access token string
            } else {
                throw new UnsupportedOperationException(String.format("AE Token service does not support creating impersonation tokens against users of type '%s'", user.getClass().getSimpleName()));
            }
        } else {
            throw new IllegalStateException(String.format("Unknown impersonation token format for user '%s'", impersonator.getUsername()));
        }

        return scopeAccessForImpersonation;
    }

    private UserScopeAccess createNewUserTokenForImpersonation(EndUser user) {
        DateTime now = new DateTime();

        UserScopeAccess newUserScopeAccess = createInstanceOfUserScopeAccess(user, getCloudAuthClientId(), getRackspaceCustomerId());
        int tokenLifetimeInSeconds = getDefaultCloudAuthTokenExpirationSeconds();
        DateTime expiration = now.plusSeconds(tokenLifetimeInSeconds);
        newUserScopeAccess.setAccessTokenExp(expiration.toDate());
        newUserScopeAccess.setAuthenticatedBy(Arrays.asList(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION));
        addUserScopeAccess(user, newUserScopeAccess);

        return newUserScopeAccess;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }
}
