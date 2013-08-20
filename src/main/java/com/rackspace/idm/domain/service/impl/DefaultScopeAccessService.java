package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.*;
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

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

@Component
public class DefaultScopeAccessService implements ScopeAccessService {

    public static final String NULL_ARGUMENT_PASSED_IN = "Null argument passed in.";
    public static final String ADDING_SCOPE_ACCESS = "Adding scopeAccess {}";
    public static final String ADDED_SCOPE_ACCESS = "Added scopeAccess {}";
    public static final String NULL_SCOPE_ACCESS_OBJECT_INSTANCE = "Null scope access object instance.";

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
        List<Tenant> tenants = new ArrayList<Tenant>();
        for (TenantRole role : roles) {
            if (role.getTenantIds() != null) {
                for (String tenantId : role.getTenantIds()) {
                    Tenant tenant = this.tenantService.getTenant(tenantId);
                    if (tenant != null) {
                        tenants.add(tenant);
                    }
                }
            }
        }

        // Third get the endppoints for each tenant
        for (Tenant tenant : tenants) {
            OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant);
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
        List<Tenant> tenants = new ArrayList<Tenant>();
        for (TenantRole role : roles) {
            if (role.getTenantIds() != null) {
                for (String tenantId : role.getTenantIds()) {
                    Tenant tenant = this.tenantService.getTenant(tenantId);
                    if (tenant != null) {
                        tenants.add(tenant);
                    }
                }
            }
        }

        // Third get the endppoints for each tenant
        for (Tenant tenant : tenants) {
            OpenstackEndpoint endpoint = this.endpointService.getOpenStackEndpointForTenant(tenant);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    @Override
    public ImpersonatedScopeAccess addImpersonatedScopeAccess(User user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest) {
        String impersonatingUsername = impersonationRequest.getUser().getUsername();
        List<ScopeAccess> existing = scopeAccessDao.getAllImpersonatedScopeAccessForUser(user);
        ImpersonatedScopeAccess mostRecent = (ImpersonatedScopeAccess) scopeAccessDao.getMostRecentImpersonatedScopeAccessForUser(user, impersonatingUsername);

        for (ScopeAccess scopeAccess : existing) {
            if (scopeAccess.isAccessTokenExpired(new DateTime())) {
                scopeAccessDao.deleteScopeAccess(scopeAccess);
            }
        }

        ImpersonatedScopeAccess scopeAccessToAdd = new ImpersonatedScopeAccess();
        scopeAccessToAdd = setImpersonatedScopeAccess(user, impersonationRequest, scopeAccessToAdd);
        scopeAccessToAdd.setAccessTokenString(this.generateToken());
        scopeAccessToAdd.setImpersonatingToken(impersonatingToken);

        if (mostRecent == null) {
            scopeAccessToAdd.setUsername(user.getUsername());
            scopeAccessToAdd.setClientId(clientId);
            scopeAccessToAdd.setImpersonatingUsername(impersonatingUsername);
        } else {
            scopeAccessToAdd.setUsername(mostRecent.getUsername());
            scopeAccessToAdd.setClientId(mostRecent.getClientId());
            scopeAccessToAdd.setImpersonatingUsername(mostRecent.getImpersonatingUsername());

            if (!mostRecent.isAccessTokenExpired(new DateTime())) {
                scopeAccessToAdd.setAccessTokenString(mostRecent.getAccessTokenString());
                scopeAccessDao.deleteScopeAccess(mostRecent);
            }
        }

        logger.info(ADDING_SCOPE_ACCESS, scopeAccessToAdd);
        scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccessToAdd);

        return scopeAccessToAdd;
    }

    ImpersonatedScopeAccess setImpersonatedScopeAccess(User caller, ImpersonationRequest impersonationRequest, ImpersonatedScopeAccess impersonatedScopeAccess) {
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

            expirationSeconds = getTokenExpirationSeconds(impersonationRequest.getExpireInSeconds());
        }

        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
        return impersonatedScopeAccess;
    }

    void validateExpireInElement(User caller, ImpersonationRequest impersonationRequest) {
        if(impersonationRequest==null || impersonationRequest.getExpireInSeconds()==null){
            return;
        }
        if (impersonationRequest.getExpireInSeconds() < 1) {
            throw new BadRequestException("Expire in element cannot be less than 1.");
        }
        if (caller instanceof Racker) {
            int rackerMax = config.getInt("token.impersonatedByRackerMaxSeconds");
            rackerMax = (int)Math.ceil(rackerMax * (1 + config.getDouble("token.entropy")));

            if (impersonationRequest.getExpireInSeconds() > rackerMax) {
                throw new BadRequestException("Expire in element cannot be more than " + rackerMax);
            }
        } else {
            int serviceMax = config.getInt("token.impersonatedByServiceMaxSeconds");
            serviceMax = (int)Math.ceil(serviceMax * (1 + config.getDouble("token.entropy")));

            if (impersonationRequest.getExpireInSeconds() > serviceMax) {
                throw new BadRequestException("Expire in element cannot be more than " + serviceMax);
            }
        }
    }

    @Override
    public void addUserScopeAccess(User user, ScopeAccess scopeAccess) {
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
    public void expireAccessToken(String tokenString) throws IOException, JAXBException {
        logger.debug("Expiring access token {}", tokenString);
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }

        Date expireDate;
        expireDate =  scopeAccess.getAccessTokenExp();
        scopeAccess.setAccessTokenExpired();
        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        User user = userService.getUserByScopeAccess(scopeAccess);
        if(user != null && !StringUtils.isBlank(scopeAccess.getAccessTokenString()) && !isExpired(expireDate)){
            logger.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost(user, tokenString);
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
        List<ScopeAccess> saList = this.scopeAccessDao.getScopeAccesses(client);

        for (ScopeAccess sa : saList) {
            sa.setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(sa);
        }
        logger.debug("Done expiring all tokens for client {}", clientId);
    }

    @Override
    public void expireAllTokensForUser(String username) throws IOException, JAXBException {
        logger.debug("Expiring all tokens for user {}", username);
        final User user = this.userService.getUser(username);
        if (user == null) {
            return;
        }

        final List<ScopeAccess> saList = this.scopeAccessDao
                .getScopeAccesses(user);

        for (final ScopeAccess sa : saList) {
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
    public void expireAllTokensForUserById(String userId) throws IOException, JAXBException {
        logger.debug("Expiring all tokens for user {}", userId);
        final User user = this.userService.getUserById(userId);
        if (user == null) {
            return;
        }

        final List<ScopeAccess> saList = this.scopeAccessDao
                .getScopeAccesses(user);

        for (final ScopeAccess sa : saList) {
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
    public RackerScopeAccess getRackerScopeAccessByClientId(User user, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        RackerScopeAccess scopeAccess;
        scopeAccess = (RackerScopeAccess) scopeAccessDao.getMostRecentScopeAccessByClientId(user, clientId);
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
    public List<ScopeAccess> getScopeAccessListByUserId(String userId) {
        logger.debug("Getting ScopeAccess list by user id {}", userId);
        if (userId == null) {
            throw new NotFoundException("Invalid user id; user id cannot be null");
        }
        final List<ScopeAccess> scopeAccessList = this.scopeAccessDao.getScopeAccessesByUserId(userId);
        logger.debug("Got ScopeAccess {} by user id {}", scopeAccessList, userId);
        return scopeAccessList;
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
    public List<ScopeAccess> getScopeAccessesForUserByClientId(User user, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", user, clientId);
        List<ScopeAccess> sa = this.scopeAccessDao.getScopeAccessesByClientId(user, clientId);
        logger.debug("Got {} ScopeAccesses for parent", sa);
        return sa;
    }

    @Override
    public List<ScopeAccess> getScopeAccessesForApplicationByClientId(Application application, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", application, clientId);
        List<ScopeAccess> sa = this.scopeAccessDao.getScopeAccessesByClientId(application, clientId);
        logger.debug("Got {} ScopeAccesses for parent", sa);
        return sa;
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
    public RackerScopeAccess getValidRackerScopeAccessForClientId(User user, String clientId, List<String> authenticatedBy) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        RackerScopeAccess scopeAccess = getRackerScopeAccessByClientId(user, clientId);
        if (scopeAccess == null){
            int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());
            scopeAccess = new RackerScopeAccess();
            scopeAccess.setClientId(clientId);
            scopeAccess.setRackerId(user.getId());
            scopeAccess.setAccessTokenString(generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccess.setAuthenticatedBy(authenticatedBy);
            scopeAccessDao.addScopeAccess(user, scopeAccess);
        }
        //if expired update with new token
        scopeAccess = updateExpiredRackerScopeAccess(scopeAccess);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username,
                                                                                    String apiKey, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = userService.authenticateWithApiKey(username, apiKey);
        handleApiKeyUsernameAuthenticationFailure(username, result);

        return this.getValidUserScopeAccessForClientId(result.getUser(), clientId, authenticateBy(GlobalConstants.AUTHENTICATED_BY_APIKEY));
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

        return this.getValidUserScopeAccessForClientId(result.getUser(), clientId, authenticateBy(GlobalConstants.AUTHENTICATED_BY_PASSWORD));
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
        List<ScopeAccess> saList = getScopeAccessesForApplicationByClientId(application, clientId);
        for (ScopeAccess sa : saList) {
            deleteScopeAccess(sa);
        }
    }

    @Override
    public void deleteScopeAccessesForUser(User user, String clientId) {
        List<ScopeAccess> saList = getScopeAccessesForUserByClientId(user, clientId);
        for (ScopeAccess sa : saList) {
            deleteScopeAccess(sa);
        }
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy) {
        List<ScopeAccess> scopeAccessList = scopeAccessDao.getScopeAccesses(user);
        if (scopeAccessList.size() == 0) {
            UserScopeAccess scopeAccess = provisionUserScopeAccess(user, clientId);
            if (authenticatedBy != null) {
                scopeAccess.setAuthenticatedBy(authenticatedBy);
            }
            this.scopeAccessDao.addScopeAccess(user, scopeAccess);
            return scopeAccess;
        }

        ScopeAccess mostRecent = scopeAccessDao.getMostRecentScopeAccessByClientId(user, clientId);

        for (ScopeAccess scopeAccess : scopeAccessList) {
            if (!scopeAccess.getAccessTokenString().equals(mostRecent.getAccessTokenString())) {
                if (scopeAccess.isAccessTokenExpired(new DateTime())) {
                    scopeAccessDao.deleteScopeAccess(scopeAccess);
                }
            }
        }
        if (authenticatedBy != null) {
            mostRecent.setAuthenticatedBy(authenticatedBy);
        }
        return updateExpiredUserScopeAccess((UserScopeAccess) mostRecent, false);
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
        User user = userService.getUserByScopeAccess(scopeAccess);

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());

            scopeAccessDao.addScopeAccess(user, scopeAccessToAdd);
            scopeAccessDao.deleteScopeAccess(scopeAccess);
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
    public List<ScopeAccess> getScopeAccessesForUser(User user) {
        return scopeAccessDao.getScopeAccesses(user);
    }

    @Override
    public List<ScopeAccess> getScopeAccessesForApplication(Application application) {
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
        return config.getInt("token.cloudAuthExpirationSeconds");
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

    private RackerScopeAccess updateExpiredRackerScopeAccess(RackerScopeAccess scopeAccess) {
        RackerScopeAccess scopeAccessToAdd = new RackerScopeAccess();
        scopeAccessToAdd.setRackerId(scopeAccess.getRackerId());
        scopeAccessToAdd.setRefreshTokenString(scopeAccess.getRefreshTokenString());
        scopeAccessToAdd.setRefreshTokenExp(scopeAccess.getRefreshTokenExp());
        scopeAccessToAdd.setClientId(scopeAccess.getClientId());
        scopeAccessToAdd.setClientRCN(scopeAccess.getClientRCN());

        int expirationSeconds = getTokenExpirationSeconds(getDefaultCloudAuthTokenExpirationSeconds());
        Racker racker = (Racker) userService.getUserByScopeAccess(scopeAccess);

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccessToAdd.setAccessTokenString(this.generateToken());
            scopeAccessToAdd.setAccessTokenExp(new DateTime().plusSeconds(expirationSeconds).toDate());
            scopeAccessDao.addScopeAccess(racker, scopeAccessToAdd);
            scopeAccessDao.deleteScopeAccess(scopeAccess);
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
