package com.rackspace.idm.domain.service.impl;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultScopeAccessService implements ScopeAccessService {

    public static final String NULL_ARGUMENT_PASSED_IN = "Null argument passed in.";
    public static final String ADDING_SCOPE_ACCESS = "Adding scopeAccess {}";
    public static final String ADDED_SCOPE_ACCESS = "Added scopeAccess {}";

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

        AuthenticatedByMethodGroup impersonatorAuthByGroup = AuthenticatedByMethodGroup.getGroup(impersonatorAuthByMethods);
        ImpersonationTokenRequest impTokenRequest = ImpersonationTokenRequest.builder().issuedToUser(impersonator)
                .authenticatedByMethodGroup(impersonatorAuthByGroup)
                .authenticationDomainId(impersonator.getDomainId()) // Impersonation always done under user's domain
                .clientId(identityConfig.getStaticConfig().getCloudAuthClientId())
                .creationDate(requestInstant.toDate().toInstant())
                .expirationDate(desiredImpersonationTokenExpiration.toDate().toInstant())
                .userToImpersonate(userBeingImpersonated)
                .build();


        EndUserTokenRequest userBeingImpersonatedTokenRequest = EndUserTokenRequest.builder().issuedToUser(userBeingImpersonated)
                .clientId(identityConfig.getStaticConfig().getCloudAuthClientId())
                .expirationDate(desiredImpersonationTokenExpiration.toDate().toInstant())
                .authenticationDomainId(userBeingImpersonated.getDomainId())
                .authenticatedByMethodGroup(AuthenticatedByMethodGroup.IMPERSONATION)
                .build();

        UserScopeAccess userBeingImpersonatedToken = (UserScopeAccess) createToken(userBeingImpersonatedTokenRequest);
        ImpersonatedScopeAccess scopeAccessToUse = (ImpersonatedScopeAccess) createToken(impTokenRequest);
        scopeAccessToUse.setImpersonatingToken(userBeingImpersonatedToken.getAccessTokenString());

        Audit.logSuccessfulImpersonation(scopeAccessToUse);
        return scopeAccessToUse;
    }

    /**
     * This is only used by legacy Fed v1.0 API. Will be removed in future release.
     * @param user
     * @param scopeAccess
     */
    @Override
    @Deprecated
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

    /**
     * @deprecated use createToken
     *
     * @param racker
     * @param clientId
     * @param authenticatedBy
     * @return
     */
    @Override
    public RackerScopeAccess getValidRackerScopeAccessForClientId(Racker racker, String clientId, List<String> authenticatedBy) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        int expirationSeconds = getTokenExpirationSeconds(identityConfig.getStaticConfig().getTokenLifetimeRackerDefault());

        // Convert list to method group
        AuthenticatedByMethodGroup authGroup = CollectionUtils.isNotEmpty(authenticatedBy) ? AuthenticatedByMethodGroup.getGroup(authenticatedBy) : null;

        RackerTokenRequest tokenRequest = RackerTokenRequest.builder().issuedToUser(racker).clientId(clientId)
                .expireAfterCreation(expirationSeconds)
                .authenticatedByMethodGroup(authGroup)
                .build();

        RackerScopeAccess scopeAccess = (RackerScopeAccess) createToken(tokenRequest);

        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    /**
     * @deprecated use createToken
     * @param username
     * @param password
     * @param clientId
     * @return
     */
    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username,
                                                                              String password, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = this.userService.authenticate(username, password);
        handleAuthenticationFailure(username, result);

        return this.addScopeAccess((User) result.getUser(), clientId, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD));
    }

    /**
     * Ideally this will be the sole mechanism to create a new token while returning in the standard "ScopeAccess" format
     *
     * @param tokenRequest
     * @return
     */
    @Override
    public ScopeAccess createToken(TokenRequest tokenRequest) {
        ScopeAccess sa = tokenRequest.generateShellScopeAccessForRequest();
        this.scopeAccessDao.addScopeAccess(tokenRequest.getIssuedToUser(), sa);
        return sa;
    }

    /**
     * @deprecated use createToken
     * @param user
     * @param clientId
     * @param authenticatedBy
     * @return
     */
    @Override
    public UserScopeAccess addScopeAccess(User user, String clientId, List<String> authenticatedBy) {
        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());

        // Convert list to method group
        AuthenticatedByMethodGroup authGroup = CollectionUtils.isNotEmpty(authenticatedBy) ? AuthenticatedByMethodGroup.getGroup(authenticatedBy) : null;

        EndUserTokenRequest tokenRequest = EndUserTokenRequest.builder().issuedToUser(user)
                .clientId(clientId)
                .authenticatedByMethodGroup(authGroup)
                .authenticationDomainId(user.getDomainId()) // Initially only support authenticating to user's domain
                .expireAfterCreation(expirationSeconds)
                .build();

        return (UserScopeAccess) createToken(tokenRequest);
    }

    /**
     * @deprecated use createToken
     * @param user
     * @param clientId
     * @param authenticatedBy
     * @param expirationSeconds
     * @param scope
     * @return
     */
    @Override
    public ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, int expirationSeconds, String scope) {
        DateTime expiration = new DateTime().plusSeconds(expirationSeconds);
        return addScopedScopeAccess(user, clientId, authenticatedBy, expiration.toDate(), scope);
    }

    /**
     * @deprecated use createToken
     * @param user
     * @param clientId
     * @param authenticatedBy
     * @param expirationDate
     * @param scope
     * @return
     */
    @Override
    public ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, Date expirationDate, String scope) {
        boolean isEndUser = user instanceof EndUser;
        boolean isRacker = user instanceof Racker;

        // Convert list to method group
        AuthenticatedByMethodGroup authGroup = CollectionUtils.isNotEmpty(authenticatedBy) ? AuthenticatedByMethodGroup.getGroup(authenticatedBy) : null;

        // Convert scope to available
        TokenScopeEnum scopeType = scope != null ? TokenScopeEnum.fromScope(scope) : null;

        TokenRequest tokenRequest;
        if (isEndUser) {
            EndUser endUser = (EndUser) user;
            tokenRequest = EndUserTokenRequest.builder().issuedToUser(endUser).clientId(clientId)
                    .expirationDate(expirationDate.toInstant())
                    .authenticatedByMethodGroup(authGroup)
                    .authenticationDomainId(user.getDomainId()) // Initially only support authenticating to user's domain
                    .scope(scopeType)
                    .build();

        }  else if (isRacker) {
            Racker racker = (Racker) user;
            tokenRequest = RackerTokenRequest.builder().issuedToUser(racker).clientId(clientId)
                    .expirationDate(expirationDate.toInstant())
                    .authenticatedByMethodGroup(authGroup)
                    .build();
        } else {
            throw new UnsupportedOperationException("Unrecognized user type.");
        }

        return createToken(tokenRequest);
    }

    //TODO - little smelly that result is a AuthResponseTuple, but not able to do a full refactor here
    @Override
    public AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult) {
        int expirationSeconds = 0;
        String scope = userAuthenticationResult.getScope();

        if (StringUtils.isNotBlank(scope)) {
            TokenScopeEnum tokenScope = TokenScopeEnum.fromScope(userAuthenticationResult.getScope());

            if (tokenScope == TokenScopeEnum.PWD_RESET) {
                expirationSeconds = identityConfig.getReloadableConfig().getForgotPasswordTokenLifetime();
            } else if (tokenScope == TokenScopeEnum.SETUP_MFA) {
                expirationSeconds = identityConfig.getStaticConfig().getSetupMfaScopedTokenExpirationSeconds();
            } else {
                throw new UnsupportedOperationException(
                        String.format("Token scope '%s' is not supported as a response to UserAuthenticationResult", tokenScope));
            }
        }  else {
            expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());
        }

        UserScopeAccess token = (UserScopeAccess) addScopedScopeAccess(userAuthenticationResult.getUser(), identityConfig.getStaticConfig().getCloudAuthClientId(), userAuthenticationResult.getAuthenticatedBy(), expirationSeconds, scope);

        return new AuthResponseTuple((User)userAuthenticationResult.getUser(), token);
    }

    private int getTokenExpirationSeconds(int value) {
        Double entropy = identityConfig.getStaticConfig().getTokeLifetimeEntropy();
        Integer min = (int)Math.floor(value * (1 - entropy));
        Integer max = (int)Math.ceil(value * (1 + entropy));
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
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
    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    int getDefaultCloudAuthTokenExpirationSeconds() {
        return identityConfig.getStaticConfig().getTokenLifetimeEndUserDefault();
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
}
