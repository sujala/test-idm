package com.rackspace.idm.domain.service.impl;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ProvisionedUserDelegate;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.TokenScopeEnum;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.OpenstackType;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenRevocationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.endpointassignment.service.RuleService;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultScopeAccessService implements ScopeAccessService {

    public static final String NULL_ARGUMENT_PASSED_IN = "Null argument passed in.";
    public static final String ADDING_SCOPE_ACCESS = "Adding scopeAccess {}";
    public static final String ADDED_SCOPE_ACCESS = "Added scopeAccess {}";
    public static final String NULL_SCOPE_ACCESS_OBJECT_INSTANCE = "Null scope access object instance.";

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
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user) {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        // First get the tenantRoles for the token
        List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);

        if (roles == null || roles.size() == 0) {
            return endpoints;
        }

        // Second get the tenants from each of those roles
        Map<Tenant, HashSet<OpenstackType>> tenants = getTenantsAndOpenstackTypesForRoles(roles);

        // Third get the endppoints for each tenant
        for (Tenant tenant : tenants.keySet()) {
            OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant, tenants.get(tenant), user.getRegion(), null);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    /**
     * @deprecated Use Identity user service of same name
     * @param baseUser
     * @return
     */
    @Deprecated
    @Trace
    @Override
    public ServiceCatalogInfo getServiceCatalogInfo(BaseUser baseUser) {
        return identityUserService.getServiceCatalogInfo(baseUser);
    }

    /**
     * @deprecated Use Identity user service of same name
     * @param baseUser
     * @return
     */
    @Deprecated
    @Override
    public ServiceCatalogInfo getServiceCatalogInfoApplyRcnRoles(BaseUser baseUser) {
        return identityUserService.getServiceCatalogInfoApplyRcnRoles(baseUser);
    }

    /**
     * Given a Collection of roles, returns a Map containing all referenced tenants (by the role.tenantId relationship)
     * with keys as the tenant and the value as the openstack type (MOSSO, NAST, other) as the value
     */
    private Map<Tenant, HashSet<OpenstackType>> getTenantsAndOpenstackTypesForRoles(Collection<TenantRole> roles) {
        Map<Tenant, HashSet<OpenstackType>> tenants = new HashedMap<>();
        Set<String> tenantIds = new HashSet<>();
        for (TenantRole role : roles) {
            if (role.getTenantIds() != null) {
                for (String tenantId : role.getTenantIds()) {
                    if(!tenantIds.contains(tenantId)) {
                        tenantIds.add(tenantId);
                        Tenant tenant = this.tenantService.getTenant(tenantId);
                        OpenstackType type = getOpenStackType(role);
                        // NOTE: Null check for type can be removed once CID-463 (Add OpenstackType to all applications)
                        // has been completed.
                        if (tenant != null && type != null) {
                            tenants.put(tenant, new HashSet<>(Collections.singletonList(type)));
                        }
                    }
                }
            }
        }
        return tenants;
    }

    private OpenstackType getOpenStackType(TenantRole role) {
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

        try {
            return new OpenstackType(type);
        } catch (IllegalArgumentException e) {
            logger.warn(String.format("Unable to get OpenstackType for Role with ID %s belonging to Application with ID %s.",
                    role.getRoleRsId(), role.getClientId()), e);
            return null;
        }
    }

    /**
     * Creates and saves a new impersonation token to use against the user and cleans up any expired scope accesses.
     */
    @Override
    public ImpersonatedScopeAccess processImpersonatedScopeAccessRequest(BaseUser impersonator, EndUser userBeingImpersonated, ImpersonationRequest impersonationRequest, ImpersonatorType impersonatorType, List<String> impersonatorAuthByMethods) {
        DateTime requestInstant = new DateTime();

        DateTime desiredImpersonationTokenExpiration = calculateDesiredImpersonationTokenExpiration(impersonationRequest, impersonatorType, requestInstant);

        UserScopeAccess userTokenForImpersonation = getUserScopeAccessForImpersonationRequest(impersonator, userBeingImpersonated, desiredImpersonationTokenExpiration, requestInstant);

        ImpersonatedScopeAccess scopeAccessToUse = createImpersonatedScopeAccess(impersonator, userBeingImpersonated, userTokenForImpersonation, desiredImpersonationTokenExpiration, impersonatorAuthByMethods);
        logger.info(ADDING_SCOPE_ACCESS, scopeAccessToUse);
        scopeAccessDao.addScopeAccess(impersonator, scopeAccessToUse);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccessToUse);

        Audit.logSuccessfulImpersonation(scopeAccessToUse);
        return scopeAccessToUse;
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
        } else {
            //federated users are not allowed to impersonate so safe to cast to user at this point
            newImpersonatedScopeAccess.setUserRsId(((User) impersonator).getId());
        }

        newImpersonatedScopeAccess.setClientId(clientId);
        newImpersonatedScopeAccess.setAccessTokenString(this.generateToken());
        newImpersonatedScopeAccess.setImpersonatingToken(userTokenForImpersonation.getAccessTokenString());
        newImpersonatedScopeAccess.setAccessTokenExp(impersonationTokenExpirationDate.toDate());
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
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        logger.debug("Getting ScopeAccess by Access Token {}", accessToken);
        if (accessToken == null) {
            throw new NotFoundException("Invalid accessToken; Token cannot be null");
        }
        if (accessToken.length() < 1) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(accessToken);
        logger.debug("Got ScopeAccess {} by Access Token {}", scopeAccess, accessToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccess unmarshallScopeAccess(String accessToken) {
        if (accessToken == null || accessToken.length() < 1) {
            return null;
        }
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(accessToken);
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
    public Iterable<ScopeAccess> getScopeAccessesForUserByClientId(User user, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", user, clientId);
        return this.scopeAccessDao.getScopeAccessesByClientId(user, clientId);
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
        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthRackerTokenExpirationSeconds());
        RackerScopeAccess scopeAccess = new RackerScopeAccess();
        scopeAccess.setClientId(clientId);
        scopeAccess.setRackerId(racker.getRackerId());
        scopeAccess.setAccessTokenString(generateToken());
        scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
        scopeAccess.setAuthenticatedBy(authenticatedBy);
        scopeAccessDao.addScopeAccess(racker, scopeAccess);

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
    public void deleteExpiredTokensQuietly(BaseUser user) {
        Iterable<ScopeAccess> tokenIterator = Collections.EMPTY_LIST;
        if (user instanceof EndUser) {
            //little weird that we search for all tokens assigned to this user across entire DIT rather than just under
            //the user, but this is legacy code
            tokenIterator = getScopeAccessListByUserId(user.getId());
        } else if (user instanceof Racker) {
            tokenIterator = scopeAccessDao.getScopeAccesses(user);
        }

        try {
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

    //TODO: Rename this method as it no longer updates anything.
    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy) {
        UserScopeAccess scopeAccess = provisionUserScopeAccess(user, clientId);
        if (authenticatedBy != null) {
            scopeAccess.setAuthenticatedBy(authenticatedBy);
        }
        this.scopeAccessDao.addScopeAccess(user, scopeAccess);
        return scopeAccess;
    }

    @Override
    public ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, int expirationSeconds, String scope) {
        DateTime expiration = new DateTime().plusSeconds(expirationSeconds);
        return addScopedScopeAccess(user, clientId, authenticatedBy, expiration.toDate(), scope);
    }

    // TODO: Refactor all the scope access creation methods. THis is getting unwieldy.
    @Override
    public ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, Date expirationDate, String scope) {
        ScopeAccess scopeAccessToAdd = null;

        boolean isProvisionedUser = user instanceof User;
        boolean isProvisionedUserDelegate = user instanceof ProvisionedUserDelegate;

        if (isProvisionedUser || isProvisionedUserDelegate) {
            UserScopeAccess userScopeAccess = new UserScopeAccess();
            userScopeAccess.setUserRsId(user.getId());
            userScopeAccess.setClientId(clientId);
            userScopeAccess.setAccessTokenExp(expirationDate);
            userScopeAccess.setAccessTokenString(generateToken());
            userScopeAccess.getAuthenticatedBy().addAll(authenticatedBy);
            userScopeAccess.setScope(scope);
            scopeAccessToAdd = userScopeAccess;

            if (isProvisionedUserDelegate) {
                userScopeAccess.setDelegationAgreementId(((ProvisionedUserDelegate)user).getDelegationAgreement().getId());
            }
        }  else {
            // We can fully implement this method for Federated Users and Rackers, but
            // for now its just going to be an unsupported method.
            throw new UnsupportedOperationException();
        }

        this.scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);

        return scopeAccessToAdd;
    }

    //TODO - little smelly that result is a AuthResponseTuple, but not able to do a full refactor here
    @Override
    public AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult) {

        UserScopeAccess sa;

        // If the token is going to be scoped we create it directly, otherwise we'll go through the usual call
        if (StringUtils.isNotBlank(userAuthenticationResult.getScope())) {
            TokenScopeEnum tokenScope = TokenScopeEnum.fromScope(userAuthenticationResult.getScope());

            int expirationSeconds;
            if (tokenScope == TokenScopeEnum.PWD_RESET) {
                expirationSeconds = identityConfig.getReloadableConfig().getForgotPasswordTokenLifetime();
            } else if (tokenScope == TokenScopeEnum.SETUP_MFA) {
                expirationSeconds = identityConfig.getStaticConfig().getSetupMfaScopedTokenExpirationSeconds();
            } else {
                throw new UnsupportedOperationException(
                        String.format("Token scope '%s' is not supported as a response to UserAuthenticationResult", tokenScope));
            }

            sa = (UserScopeAccess) addScopedScopeAccess(userAuthenticationResult.getUser(),
                    identityConfig.getCloudAuthClientId(),
                    userAuthenticationResult.getAuthenticatedBy(),
                    expirationSeconds,
                    userAuthenticationResult.getScope());
        }  else {
            sa = getValidUserScopeAccessForClientId((User) userAuthenticationResult.getUser(),
                    identityConfig.getCloudAuthClientId(),
                    userAuthenticationResult.getAuthenticatedBy());
        }

        return new AuthResponseTuple((User)userAuthenticationResult.getUser(), sa);
    }

    private void deleteExpiredScopeAccessesExceptForMostRecent(Iterable<ScopeAccess> scopeAccessList, ScopeAccess mostRecent) {
        for (ScopeAccess scopeAccess : scopeAccessList) {
            if (!scopeAccess.getAccessTokenString().equals(mostRecent.getAccessTokenString())) {
                if (scopeAccess.isAccessTokenExpired(new DateTime())) {
                    boolean success = deleteScopeAccessQuietly(scopeAccess);
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
        usa.setUserRsId(user.getId());
        usa.setClientId(clientId);
        usa.setClientRCN(clientRCN);
        usa.setAccessTokenString(UUID.randomUUID().toString().replace("-", ""));
        usa.setAccessTokenExp(new DateTime().toDate());
        return usa;
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

    @Override
    public String generateToken() {
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

        if (tFormat == TokenFormat.AE) {
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

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
