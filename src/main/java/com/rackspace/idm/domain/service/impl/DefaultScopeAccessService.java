package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultScopeAccessService implements ScopeAccessService {

    private static final String PASSWORD_RESET_CLIENT_ID = "PASSWORDRESET";
    public static final String NULL_ARGUMENT_PASSED_IN = "Null argument passed in.";
    public static final String ADDING_SCOPE_ACCESS = "Adding scopeAccess {}";
    public static final String ADDED_SCOPE_ACCESS = "Added scopeAccess {}";
    public static final String NULL_SCOPE_ACCESS_OBJECT_INSTANCE = "Null scope access object instance.";
    public static final String GETTING_PERMISSION_ON_SCOPE_ACCESS = "Getting Permission {} on ScopeAccess {}";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ScopeAccessDao scopeAccessDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private EndpointDao endpointDao;
    @Autowired
    private AuthHeaderHelper authHeaderHelper;
    @Autowired
    private ApplicationDao clientDao;
    @Autowired
    private Configuration config;
    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Override
    public List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess token) {

        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        // First get the tenantRoles for the token
        List<TenantRole> roles = this.tenantRoleDao.getTenantRolesForScopeAccess(token);

        if (roles == null || roles.size() == 0) {
            return endpoints;
        }

        // Second get the tenants from each of those roles
        List<Tenant> tenants = new ArrayList<Tenant>();
        for (TenantRole role : roles) {
            if (role.getTenantIds() != null) {
                for (String tenantId : role.getTenantIds()) {
                    Tenant tenant = this.tenantDao.getTenant(tenantId);
                    if (tenant != null) {
                        tenants.add(tenant);
                    }
                }
            }
        }

        // Third get the endppoints for each tenant
        for (Tenant tenant : tenants) {
            OpenstackEndpoint endpoint = this.endpointDao.getOpenstackEndpointsForTenant(tenant);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    @Override
    public ScopeAccess addDelegateScopeAccess(String parentUniqueId,
                                              ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info(ADDING_SCOPE_ACCESS, scopeAccess);
        ScopeAccess newScopeAccess = this.scopeAccessDao.addDelegateScopeAccess(parentUniqueId, scopeAccess);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccess);
        return newScopeAccess;
    }

    @Override
    public ImpersonatedScopeAccess addImpersonatedScopeAccess(User user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest) {
        String impersonatingUsername = impersonationRequest.getUser().getUsername();
        ImpersonatedScopeAccess existingAccess = (ImpersonatedScopeAccess)this.scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(user.getUniqueId(), impersonatingUsername);

        if (existingAccess != null) {
            if (existingAccess.isAccessTokenExpired(new DateTime()) && existingAccess.getImpersonatingToken() != null &&
                    existingAccess.getImpersonatingToken().equals(impersonatingToken)) {
                scopeAccessDao.deleteScopeAccess(existingAccess);
            } else {
                return existingAccess;
            }
        }

        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess();
        scopeAccess = setImpersonatedScopeAccess(user, impersonationRequest, scopeAccess);
        scopeAccess.setUsername(user.getUsername());
        scopeAccess.setClientId(clientId);
        scopeAccess.setImpersonatingUsername(impersonatingUsername);
        scopeAccess.setImpersonatingToken(impersonatingToken);
        scopeAccess.setAccessTokenString(this.generateToken());

        logger.info(ADDING_SCOPE_ACCESS, scopeAccess);
        scopeAccess = (ImpersonatedScopeAccess) this.scopeAccessDao.addImpersonatedScopeAccess(user.getUniqueId(), scopeAccess);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccess);

        return scopeAccess;
    }

    ImpersonatedScopeAccess setImpersonatedScopeAccess(User caller, ImpersonationRequest impersonationRequest, ImpersonatedScopeAccess impersonatedScopeAccess) {
        validateExpireInElement(caller, impersonationRequest);
        if (impersonationRequest.getExpireInSeconds() == null) {
            if (caller instanceof Racker) {
                impersonatedScopeAccess.setRackerId(((Racker) caller).getRackerId());
                impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(config.getInt("token.impersonatedByRackerDefaultSeconds")).toDate());
            } else {
                impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(config.getInt("token.impersonatedByServiceDefaultSeconds")).toDate());
            }
        } else {
            if (caller instanceof Racker) {
                impersonatedScopeAccess.setRackerId(((Racker) caller).getRackerId());
                impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(impersonationRequest.getExpireInSeconds()).toDate());
            } else {
                impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(impersonationRequest.getExpireInSeconds()).toDate());
            }
        }
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
    public ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info(ADDING_SCOPE_ACCESS, scopeAccess);
        ScopeAccess newScopeAccess = this.scopeAccessDao.addDirectScopeAccess(parentUniqueId, scopeAccess);
        logger.info(ADDED_SCOPE_ACCESS, scopeAccess);
        return newScopeAccess;
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(accessTokenStr);
        if (scopeAccess instanceof HasAccessToken && !((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            authenticated = true;
            MDC.put(Audit.WHO, scopeAccess.getAuditContext());
        }

        logger.debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }

    @Override
    public DelegatedPermission delegatePermission(String scopeAccessUniqueId,
                                                  DelegatedPermission permission) {
        logger.info("Delegating Permssion {} to {}", permission, scopeAccessUniqueId);
        DelegatedPermission perm = this.scopeAccessDao.delegatePermission(scopeAccessUniqueId, permission);
        logger.info("Delegated Permssion {} to {}", permission, scopeAccessUniqueId);
        return perm;
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
    public void deleteScopeAccessByDn(String scopeAccessDn) {
        logger.info("Deleting ScopeAccess {}", scopeAccessDn);
        if (scopeAccessDn == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        this.scopeAccessDao.deleteScopeAccessByDn(scopeAccessDn);
        logger.info("Deleted ScopeAccess {}", scopeAccessDn);
    }

    @Override
    public void deleteDelegatedToken(User user, String tokenString) {

        if(user == null){
            throw new IllegalArgumentException("Null argument passed in");
        }

        List<DelegatedClientScopeAccess> scopeAccessList = this.getDelegatedUserScopeAccessForUsername(user.getUsername());

        if (scopeAccessList != null && scopeAccessList.size() == 0) {
            String errMsg = String.format(
                    "No delegated access tokens available for the user %s",
                    user.getUsername());
            logger.error(errMsg);
            throw new NotFoundException(errMsg);
        }

        DelegatedClientScopeAccess scopeAccessToDelete = null;

        for (DelegatedClientScopeAccess l : scopeAccessList) {
            if (l.getRefreshTokenString() != null
                    && l.getRefreshTokenString().equals(tokenString)) {
                scopeAccessToDelete = l;
                break;
            }
        }

        // Validate Token exists and is valid
        if (scopeAccessToDelete == null) {
            String errorMsg = String
                    .format("Token not found : %s", tokenString);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Got Delegated ScopeAccess {} by Access Token {}",
                scopeAccessToDelete, tokenString);
        deleteScopeAccess(scopeAccessToDelete);
    }

    @Override
    public boolean doesAccessTokenHavePermission(ScopeAccess token,
                                                 Permission permission) {
        if (permission == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.debug("Checking whether access token {} has permisison {}",
                token, permission.getPermissionId());
        return this.scopeAccessDao.doesAccessTokenHavePermission(token,
                permission);
    }

    @Override
    public boolean doesAccessTokenHaveService(ScopeAccess token, String clientId) {
        logger.debug("Checking whether access token {} has application {}", token,
                clientId);

        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(clientId);

        String parentUniqueId = null;

        if (token instanceof DelegatedClientScopeAccess) {
            parentUniqueId = token.getUniqueId();
        } else {
            try {
                parentUniqueId = token.getLDAPEntry().getParentDNString();
            } catch (LDAPException e) {
                // noop
            }
        }

        boolean hasService = this.scopeAccessDao.doesParentHaveScopeAccess(
                parentUniqueId, sa);

        logger.debug("Checked whether access token has application - {}",
                hasService);
        return hasService;
    }

    @Override
    public boolean doesUserHavePermissionForClient(User user,
                                                   Permission permission, Application client) {

        if(user == null || permission == null || client == null){
            throw new IllegalArgumentException("Null argument(s) passed in.");
        }

        logger.debug("Checking whether user {} has permission {}", user,
                permission);
        Permission poSearchParam = new Permission();
        poSearchParam.setClientId(client.getClientId());
        poSearchParam.setPermissionId(permission.getPermissionId());

        DefinedPermission definedPermission = (DefinedPermission) getPermissionForParent(
                client.getUniqueId(), poSearchParam);

        if (definedPermission == null || !definedPermission.getEnabled()) {
            // No such permission defined. Not granted.
            return false;
        }

        if (definedPermission.getGrantedByDefault()) {
            // Granted by default, but has the user been provisioned for this
            // defaultApplicationService?
            List<ScopeAccess> scopeAccessList = scopeAccessDao
                    .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                            client.getClientId());
            if (scopeAccessList.size() > 0) {
                // Provisioned, so granted.
                return true;
            }
        } else {
            GrantedPermission gpSearchParam = new GrantedPermission();
            gpSearchParam.setClientId(client.getClientId());
            gpSearchParam.setPermissionId(permission.getPermissionId());

            Permission grantedPermission = scopeAccessDao
                    .getPermissionByParentAndPermission(user.getUniqueId(),
                            gpSearchParam);
            if (grantedPermission != null) {
                // The permission has not been granted.
                return true;
            }
        }

        // Not granted.
        return false;
    }

    @Override
    public void expireAccessToken(String tokenString) {
        logger.debug("Expiring access token {}", tokenString);
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }

        if (scopeAccess instanceof HasAccessToken) {
            ((HasAccessToken) scopeAccess).setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
        logger.debug("Done expiring access token {}", tokenString);
    }

    @Override
    public void expireAllTokensForClient(String clientId) {
        logger.debug("Expiring all tokens for client {}", clientId);
        final Application client = this.clientDao.getClientByClientId(clientId);
        if (client == null) {
            return;
        }
        List<ScopeAccess> saList = this.scopeAccessDao.getScopeAccessesByParent(client.getUniqueId());

        for (ScopeAccess sa : saList) {
            if (sa instanceof HasAccessToken) {
                ((HasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
        logger.debug("Done expiring all tokens for client {}", clientId);
    }

    @Override
    public void expireAllTokensForCustomer(String customerId) {
        logger.debug("Expiring all tokens for client {}", customerId);
        List<Application> clients = getAllClientsForCustomerId(customerId);
        List<User> users = getAllUsersForCustomerId(customerId);
        for (Application client : clients) {
            this.expireAllTokensForClient(client.getClientId());
        }
        for (User user : users) {
            this.expireAllTokensForUser(user.getUsername());
        }
        logger.debug("Done expiring all tokens for client {}", customerId);
    }

    @Override
    public void expireAllTokensForUser(String username) {
        logger.debug("Expiring all tokens for user {}", username);
        final User user = this.userDao.getUserByUsername(username);
        if (user == null) {
            return;
        }

        final List<ScopeAccess> saList = this.scopeAccessDao
                .getScopeAccessesByParent(user.getUniqueId());

        for (final ScopeAccess sa : saList) {
            if (sa instanceof HasAccessToken) {
                ((HasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
        logger.debug("Done expiring all tokens for user {}", username);
    }
    
    @Override
    public void expireAllTokensForUserById(String userId) {
        logger.debug("Expiring all tokens for user {}", userId);
        final User user = this.userDao.getUserById(userId);
        if (user == null) {
            return;
        }

        final List<ScopeAccess> saList = this.scopeAccessDao
                .getScopeAccessesByParent(user.getUniqueId());

        for (final ScopeAccess sa : saList) {
            if (sa instanceof HasAccessToken) {
                ((HasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
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
    public ClientScopeAccess getClientScopeAccessForClientId(String clientUniqueId, String clientId) {
        logger.debug("Getting Client ScopeAccess by clientId", clientId);
        final ClientScopeAccess scopeAccess = (ClientScopeAccess) this.scopeAccessDao
                .getMostRecentDirectScopeAccessForParentByClientId(clientUniqueId, clientId);
        logger.debug("Got Client ScopeAccess {} by clientId {}", scopeAccess,
                clientId);
        return scopeAccess;
    }

    @Override
    public List<ScopeAccess> getDelegateScopeAccessesForParent(String parentUniqueId) {
        logger.debug("Getting Delegate ScopeAccess by parent {}",
                parentUniqueId);
        List<ScopeAccess> sa = this.scopeAccessDao
                .getDelegateScopeAccessesByParent(parentUniqueId);
        logger.debug("Got {} Delegate ScopeAccess by parent {}", sa.size(),
                parentUniqueId);
        return sa;
    }

    @Override
    public ScopeAccess getDelegateScopeAccessForParentByClientId(String parentUniqueID, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccess sa = this.scopeAccessDao
                .getDelegateScopeAccessForParentByClientId(parentUniqueID, clientId);

        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public ScopeAccess getMostRecentDirectScopeAccessForParentByClientId(String parentUniqueID, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccess sa = scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(parentUniqueID, clientId);
        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(User user) {
        if (user == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        logger.debug("Getting or creating password reset scope access for user {}", user.getUsername());

        PasswordResetScopeAccess prsa = (PasswordResetScopeAccess) this.scopeAccessDao
                .getMostRecentDirectScopeAccessForParentByClientId(user.getUniqueId(), PASSWORD_RESET_CLIENT_ID);

        if (prsa == null) {
            prsa = new PasswordResetScopeAccess();
            prsa.setUserRsId(user.getId());
            prsa.setUsername(user.getUsername());
            prsa.setUserRCN(user.getCustomerId());
            prsa.setAccessTokenExp(new DateTime().plusSeconds(
                    this.getDefaultTokenExpirationSeconds()).toDate());
            prsa.setAccessTokenString(this.generateToken());
            prsa.setClientId(PASSWORD_RESET_CLIENT_ID);
            prsa.setClientRCN(PASSWORD_RESET_CLIENT_ID);
            this.scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), prsa);
        } else {
            if (prsa.isAccessTokenExpired(new DateTime())) {
                prsa.setAccessTokenExp(new DateTime().plusSeconds(this.getDefaultTokenExpirationSeconds()).toDate());
                prsa.setAccessTokenString(this.generateToken());

                String existingScopeAccessDn = prsa.getUniqueId();
                this.scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), prsa);
                this.scopeAccessDao.deleteScopeAccessByDn(existingScopeAccessDn);
            }
        }
        logger.debug("Done getting or creating password reset scope access for user {}", user.getUsername());
        return prsa;
    }

    @Override
    public Permission getPermissionForParent(String scopeAccessUniqueId,
                                             Permission permission) {
        if (permission == null) {
            String errorMsg = String
                    .format(NULL_SCOPE_ACCESS_OBJECT_INSTANCE);
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.debug(GETTING_PERMISSION_ON_SCOPE_ACCESS, permission,
                scopeAccessUniqueId);
        Permission perm = this.scopeAccessDao
                .getPermissionByParentAndPermission(scopeAccessUniqueId, permission);
        logger.debug(GETTING_PERMISSION_ON_SCOPE_ACCESS, permission,
                scopeAccessUniqueId);
        return perm;
    }

    @Override
    public List<Permission> getPermissionsForParent(String scopeAccessUniqueId,
                                                    Permission permission) {
        if (permission == null) {
            String errorMsg = String
                    .format(NULL_SCOPE_ACCESS_OBJECT_INSTANCE);
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.debug(GETTING_PERMISSION_ON_SCOPE_ACCESS, permission,
                scopeAccessUniqueId);
        List<Permission> perms = this.scopeAccessDao
                .getPermissionsByParentAndPermission(scopeAccessUniqueId,
                        permission);
        logger.debug(GETTING_PERMISSION_ON_SCOPE_ACCESS, permission,
                scopeAccessUniqueId);
        return perms;
    }

    @Override
    public List<Permission> getPermissionsForParent(String scopeAccessUniqueId) {
        logger.debug("Getting Permissions on ScopeAccess {}",
                scopeAccessUniqueId);
        List<Permission> perms = this.scopeAccessDao
                .getPermissionsByParent(scopeAccessUniqueId);
        logger.debug("Done Getting Permissions on ScopeAccess {}",
                scopeAccessUniqueId);
        return perms;
    }

    @Override
    public RackerScopeAccess getRackerScopeAccessForClientId(String rackerUniqueId, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        final RackerScopeAccess scopeAccess = (RackerScopeAccess) scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(rackerUniqueId, clientId);
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
    public ScopeAccess getScopeAccessByUserId(String userId) {
        logger.debug("Getting ScopeAccess by user id {}", userId);
        if (userId == null) {
            throw new NotFoundException("Invalid user id; user id cannot be null");
        }
        final ScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByUserId(userId);
        logger.debug("Got ScopeAccess {} by user id {}", scopeAccess, userId);
        return scopeAccess;
    }

    @Override
    public List<ScopeAccess> getScopeAccessListByUserId(String userId) {
        logger.debug("Getting ScopeAccess list by user id {}", userId);
        if (userId == null) {
            throw new NotFoundException("Invalid user id; user id cannot be null");
        }
        final List<ScopeAccess> scopeAccessList = this.scopeAccessDao.getScopeAccessListByUserId(userId);
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

        if (scopeAccess instanceof HasAccessToken) {
            HasAccessToken hasAccessToken = (HasAccessToken) scopeAccess;
            if (hasAccessToken.isAccessTokenExpired(new DateTime())) {
                String errorMsg = String.format("Token expired : %s", accessToken);
                logger.warn(errorMsg);
                throw new NotFoundException(errorMsg);
            }
        }

        return scopeAccess;
    }

    @Override
    public DelegatedClientScopeAccess getDelegatedScopeAccessByRefreshToken(User user, String accessToken) {
        logger.debug("Getting Delegated ScopeAccess by Access Token {}", accessToken);

        ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByRefreshToken(accessToken);

        if (!(scopeAccess instanceof DelegatedClientScopeAccess)) {
            return null;
        }

        DelegatedClientScopeAccess returned = null;
        DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
        if (dcsa.getUsername().equals(user.getUsername())) {
            returned = dcsa;
        }

        logger.debug("Got Delegated ScopeAccess {} by Access Token {}",
                returned, accessToken);
        return returned;
    }

    @Override
    public DelegatedClientScopeAccess getScopeAccessByAuthCode(String authorizationCode) {
        logger.debug("Getting ScopeAccess by Authorization Code {}", authorizationCode);
        final DelegatedClientScopeAccess scopeAccess = this.scopeAccessDao.getScopeAccessByAuthorizationCode(authorizationCode);
        logger.debug("Got ScopeAccess {} by Authorization Code {}", scopeAccess, authorizationCode);
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
    public List<ScopeAccess> getScopeAccessesForParentByClientId(String parentUniqueId, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId", parentUniqueId, clientId);
        List<ScopeAccess> sa = this.scopeAccessDao.getScopeAccessesByParentAndClientId(parentUniqueId, clientId);
        logger.debug("Got {} ScopeAccesses for parent", sa);
        return sa;
    }

    // Return UserScopeAccess from the directory, valid, expired or null
    @Override
    public UserScopeAccess getUserScopeAccessForClientId(String userUniqueId, String clientId) {
        return (UserScopeAccess) scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(userUniqueId, clientId);
    }

    // Return UserScopeAccess from directory, refreshes expired
    @Override
    public UserScopeAccess getValidUserScopeAccessForClientId(String userUniqueId, String clientId) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        //if expired update with new token
        UserScopeAccess scopeAccess = updateExpiredUserScopeAccess(userUniqueId, clientId);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public RackerScopeAccess getValidRackerScopeAccessForClientId(String uniqueId, String rackerId, String clientId) {
        logger.debug("Getting ScopeAccess by clientId {}", clientId);
        RackerScopeAccess scopeAccess = getRackerScopeAccessForClientId(uniqueId, clientId);
        if (scopeAccess == null){
            scopeAccess = new RackerScopeAccess();
            scopeAccess.setClientId(clientId);
            scopeAccess.setRackerId(rackerId);
            scopeAccess.setAccessTokenString(generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            scopeAccessDao.addDirectScopeAccess(uniqueId, scopeAccess);
        }
        //if expired update with new token
        scopeAccess = updateExpiredRackerScopeAccess(scopeAccess);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess, clientId);
        return scopeAccess;
    }

    @Override
    public List<DelegatedClientScopeAccess> getDelegatedUserScopeAccessForUsername(String username) {
        logger.debug("Getting User ScopeAccess by username {}", username);
        final List<DelegatedClientScopeAccess> scopeAccessList = this.scopeAccessDao
                .getDelegatedClientScopeAccessByUsername(username);
        if (scopeAccessList == null) {
            String errMsg = String.format("Could not find scope accesses for the user {}", username);
            logger.error(errMsg);
            throw new NotFoundException(errMsg);
        }
        logger.debug("Got User ScopeAccesses {} by username {}", scopeAccessList, username);
        return scopeAccessList;
    }

    @Override
    public void updateUserScopeAccessTokenForClientIdByUser(User user, String clientId, String token, Date expires) {

        if(user == null){
            throw new IllegalArgumentException("Null user object instance.");
        }

        final UserScopeAccess scopeAccess = this.getUserScopeAccessForClientId(user.getUniqueId(), clientId);
        if (scopeAccess != null) {
            scopeAccess.setAccessTokenString(token);
            scopeAccess.setAccessTokenExp(expires);
            String existingScopeAccessDn = scopeAccess.getUniqueId();
            scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), scopeAccess);
            scopeAccessDao.deleteScopeAccessByDn(existingScopeAccessDn);
            logger.debug("Updated ScopeAccess {} by clientId {}", scopeAccess, clientId);
        }
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username,
                                                                                    String apiKey, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = userDao.authenticateByAPIKey(username, apiKey);
        handleApiKeyUsernameAuthenticationFailure(username, result);

        return this.getValidUserScopeAccessForClientId(result.getUser().getUniqueId(), clientId);
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username,
                                                                              String password, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username, clientId);
        final UserAuthenticationResult result = this.userDao.authenticate(username, password);
        handleAuthenticationFailure(username, result);

        return this.getValidUserScopeAccessForClientId(result.getUser().getUniqueId(), clientId);
    }

    @Override
    public GrantedPermission grantPermissionToClient(String parentUniqueId,
                                                     GrantedPermission permission) {
        if (permission == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info("Granting permission {} to client {}", parentUniqueId,
                permission.getPermissionId());
        Application dClient = this.clientDao.getClientByClientId(permission
                .getClientId());

        if (dClient == null) {
            String errMsg = String.format("Client %s not found",
                    permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        DefinedPermission dp = new DefinedPermission();
        dp.setClientId(dClient.getClientId());
        dp.setCustomerId(dClient.getRCN());
        dp.setPermissionId(permission.getPermissionId());

        Permission perm = this.scopeAccessDao
                .getPermissionByParentAndPermission(dClient.getUniqueId(), dp);
        if (perm == null) {
            String errMsg = String.format(
                    "Permission %s not found for client %s",
                    permission.getPermissionId(), permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccess sa = this.getMostRecentDirectScopeAccessForParentByClientId(
                parentUniqueId, perm.getClientId());

        if (sa == null) {
            sa = new ScopeAccess();
            sa.setClientId(perm.getClientId());
            sa.setClientRCN(perm.getCustomerId());
            sa = this.addDirectScopeAccess(parentUniqueId, sa);
        }

        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        grantedPerm = this.scopeAccessDao.grantPermission(sa.getUniqueId(),
                grantedPerm);

        logger.info("Done granting permission {} to client {}", parentUniqueId,
                permission.getPermissionId());
        return grantedPerm;
    }

    @Override
    public GrantedPermission grantPermissionToUser(User user,
                                                   GrantedPermission permission) {
        if (permission == null || user == null) {
            String errMsg = String.format(NULL_ARGUMENT_PASSED_IN);
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        logger.info("Granting permission {} to user {}", user.getUsername(),
                permission.getPermissionId());

        Application dClient = this.clientDao.getClientByCustomerIdAndClientId(
                permission.getCustomerId(), permission.getClientId());
        if (dClient == null) {
            String errMsg = String.format("Client %s not found",
                    permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        DefinedPermission dp = new DefinedPermission();
        dp.setClientId(permission.getClientId());
        dp.setCustomerId(permission.getCustomerId());
        dp.setPermissionId(permission.getPermissionId());

        Permission perm = this.scopeAccessDao
                .getPermissionByParentAndPermission(dClient.getUniqueId(), dp);
        if (perm == null) {
            String errMsg = String.format(
                    "Permission %s not found for client %s",
                    permission.getPermissionId(), permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        UserScopeAccess sa = (UserScopeAccess) this
                .getMostRecentDirectScopeAccessForParentByClientId(user.getUniqueId(),
                        perm.getClientId());

        if (sa == null) {
            sa = new UserScopeAccess();
            sa.setClientId(permission.getClientId());
            sa.setClientRCN(permission.getCustomerId());
            sa.setUsername(user.getUsername());
            sa.setUserRCN(user.getCustomerId());
            sa.setUserRsId(user.getId());
            sa = (UserScopeAccess) this.addDirectScopeAccess(
                    user.getUniqueId(), sa);
        }

        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        grantedPerm = this.scopeAccessDao.grantPermission(sa.getUniqueId(),
                grantedPerm);

        logger.info("Done granting permission {} to user {}",
                user.getUsername(), permission.getPermissionId());
        return grantedPerm;
    }

    @Override
    public void removePermission(Permission permission) {
        if (permission == null) {
            String errorMsg = String
                    .format(NULL_SCOPE_ACCESS_OBJECT_INSTANCE);
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        logger.info("Removing Permission {}", permission);
        this.scopeAccessDao.removePermissionFromScopeAccess(permission);
        logger.info("Removing Permission {}", permission);
    }

    @Override
    public void updatePermission(Permission permission) {
        if (permission == null) {
            String errorMsg = String
                    .format(NULL_SCOPE_ACCESS_OBJECT_INSTANCE);
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.info("Updating Permission {}", permission);
        this.scopeAccessDao.updatePermissionForScopeAccess(permission);
        logger.info("Updated Permission {}", permission);
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
        String parentDn = null;
        try {
            parentDn = new DN(scopeAccess.getUniqueId()).getParentString();
        } catch (LDAPException e) {
            logger.warn("ScopeAccess has illegal DN");
            throw new IllegalStateException("ScopeAccess has illegal DN");
        }

        this.scopeAccessDao.deleteScopeAccessByDn(scopeAccess.getUniqueId());
        this.scopeAccessDao.addDirectScopeAccess(parentDn, scopeAccess);
        logger.info("Updated ScopeAccess {}", scopeAccess);
    }


    @Override
    public void deleteScopeAccessesForParentByApplicationId(
            String parentUniqueId, String applicationId) {

        List<ScopeAccess> saList = getScopeAccessesForParentByClientId(parentUniqueId, applicationId);
        for (ScopeAccess sa : saList) {
            deleteScopeAccess(sa);
        }
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(String parentUniqueId, String clientId) {
        List<ScopeAccess> scopeAccessList = scopeAccessDao.getDirectScopeAccessForParentByClientId(parentUniqueId, clientId);
        ScopeAccess mostRecent = scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(parentUniqueId, clientId);

        for (ScopeAccess scopeAccess : scopeAccessList) {
            if (!scopeAccess.getAccessTokenString().equals(mostRecent.getAccessTokenString())) {
                if (scopeAccess.isAccessTokenExpired(new DateTime())) {
                    scopeAccessDao.deleteScopeAccess(scopeAccess);
                }
            }
        }
        return updateExpiredUserScopeAccess((UserScopeAccess) mostRecent, false);
    }

    @Override
    public UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess, boolean impersonated) {
        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            UserScopeAccess newScopeAccess = createUpdatedScopeAccess(scopeAccess, impersonated);
            scopeAccessDao.addScopeAccess(getParentDn(scopeAccess.getUniqueId()), newScopeAccess);
            scopeAccessDao.deleteScopeAccess(scopeAccess);
            return newScopeAccess;
        } else if (scopeAccess.isAccessTokenWithinRefreshWindow(getRefreshTokenWindow())) {
            UserScopeAccess newScopeAccess = createUpdatedScopeAccess(scopeAccess, impersonated);
            scopeAccessDao.addScopeAccess(getParentDn(scopeAccess.getUniqueId()), newScopeAccess);
            return newScopeAccess;
        }
        return scopeAccess;
    }

    private UserScopeAccess createUpdatedScopeAccess(UserScopeAccess scopeAccess, boolean impersonated) {
        String token = generateToken();
        Date expDate;
        if (impersonated) {
            expDate = new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate();
        } else {
            expDate = new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate();
        }

        UserScopeAccess copy = new UserScopeAccess();
        copy.setClientId(scopeAccess.getClientId());
        copy.setClientRCN(scopeAccess.getClientRCN());
        copy.setUsername(scopeAccess.getUsername());
        copy.setUserRCN(scopeAccess.getUserRCN());
        copy.setUserRsId(scopeAccess.getUserRsId());
        copy.setAccessTokenExp(expDate);
        copy.setAccessTokenString(token);

        return copy;
    }

    private String getParentDn(String entryDnString) {
        try {
            DN entryDn = new DN(entryDnString);
            return entryDn.getParentString();
        } catch (LDAPException e) {
            return null;
        }
    }

    @Override
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void setApplicationDao(ApplicationDao applicationDao) {
        this.clientDao = applicationDao;
    }

    @Override
    public void setTenantDao(TenantDao tenantDao) {
        this.tenantDao = tenantDao;
    }

    @Override
    public void setEndpointDao(EndpointDao endpointDao) {
        this.endpointDao = endpointDao;
    }

    @Override
    public void setAuthHeaderHelper(AuthHeaderHelper authHeaderHelper) {
        this.authHeaderHelper = authHeaderHelper;
    }

    @Override
    public void setAppConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public void setScopeAcessDao(ScopeAccessDao scopeAccessDao) {
        this.scopeAccessDao = scopeAccessDao;
    }

    String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // private functions
    private List<Application> getAllClientsForCustomerId(final String customerId) {
        logger.debug("Getting all clients for customer {}", customerId);
        final List<Application> clientsList = new ArrayList<Application>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Applications clientsObj = clientDao.getClientsByCustomerId(
                    customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        logger.debug("Done getting all clients for customer {}", customerId);
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(final String customerId) {
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.RCN, customerId)};
        logger.debug("Getting all users for customer {}", customerId);
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userDao.getAllUsers(filters, offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        logger.debug("Done getting all users for customer {}", customerId);
        return usersList;
    }

    int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    int getRefreshTokenWindow() {
        return config.getInt("token.refreshWindowHours");
    }

    int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    int getDefaultImpersonatedTokenExpirationSeconds() {
        return config.getInt("token.impersonatedExpirationSeconds");
    }

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
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
        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            scopeAccessDao.addScopeAccess(getParentDn(scopeAccess.getUniqueId()), scopeAccess);
            scopeAccessDao.deleteScopeAccess(scopeAccess);
            return scopeAccess;
        } else if (scopeAccess.isAccessTokenWithinRefreshWindow(getRefreshTokenWindow())) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            scopeAccessDao.addScopeAccess(getParentDn(scopeAccess.getUniqueId()), scopeAccess);
            return scopeAccess;
        }
        return scopeAccess;
    }

}
