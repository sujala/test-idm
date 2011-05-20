package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.unboundid.ldap.sdk.LDAPException;

public class DefaultScopeAccessService implements ScopeAccessService {

    private static final String PASSWORD_RESET_CLIENT_ID = "PASSWORDRESET";

    private final AuthHeaderHelper authHeaderHelper;
    private final ClientDao clientDao;
    private final Configuration config;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScopeAccessDao scopeAccessDao;
    private final UserDao userDao;

    public DefaultScopeAccessService(UserDao userDao, ClientDao clientDao,
        ScopeAccessDao scopeAccessDao, AuthHeaderHelper authHeaderHelper,
        Configuration config) {
        this.userDao = userDao;
        this.clientDao = clientDao;
        this.scopeAccessDao = scopeAccessDao;
        this.authHeaderHelper = authHeaderHelper;
        this.config = config;
    }

    @Override
    public ScopeAccess addDelegateScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info("Adding scopeAccess {}", scopeAccess);
        ScopeAccess newScopeAccess = this.scopeAccessDao
            .addDelegateScopeAccess(parentUniqueId, scopeAccess);
        logger.info("Added scopeAccess {}", scopeAccess);
        return newScopeAccess;
    }

    @Override
    public ScopeAccess addDirectScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info("Adding scopeAccess {}", scopeAccess);
        ScopeAccess newScopeAccess = this.scopeAccessDao.addDirectScopeAccess(
            parentUniqueId, scopeAccess);
        logger.info("Added scopeAccess {}", scopeAccess);
        return newScopeAccess;
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        final ScopeAccess scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(accessTokenStr);
        if (scopeAccess instanceof hasAccessToken) {
            if (!((hasAccessToken) scopeAccess)
                .isAccessTokenExpired(new DateTime())) {
                authenticated = true;
                MDC.put(Audit.WHO, scopeAccess.getAuditContext());
            }
        }

        logger
            .debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }

    @Override
    public DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission) {
        logger.info("Delegating Permssion {} to {}", permission,
            scopeAccessUniqueId);
        DelegatedPermission perm = this.scopeAccessDao.delegatePermission(
            scopeAccessUniqueId, permission);
        logger.info("Delegated Permssion {} to {}", permission,
            scopeAccessUniqueId);
        return perm;
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        logger.info("Deleting ScopeAccess {}", scopeAccess);
        if (scopeAccess == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        this.scopeAccessDao.deleteScopeAccess(scopeAccess);
        logger.info("Deleted ScopeAccess {}", scopeAccess);
    }

    @Override
    public boolean doesAccessTokenHavePermission(ScopeAccess token,
        Permission permission) {
        if (permission == null) {
            String errMsg = String.format("Null argument passed in.");
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
        logger.debug("Checking whether access token {} has service {}", token,
            clientId);

        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(clientId);

        String parentUniqueId = null;

        if (token instanceof DelegatedClientScopeAccess) {
            parentUniqueId = token.getUniqueId();
        } else {
            try {
                token.getLDAPEntry().getParentDNString();
            } catch (LDAPException e) {
                // noop
            }
        }

        boolean hasService = this.scopeAccessDao.doesParentHaveScopeAccess(
            parentUniqueId, sa);

        logger.debug("Checked whether access token has service - {}",
            hasService);
        return hasService;
    }
    
    @Override
    public boolean doesUserHavePermissionForClient(User user, Permission permission, Client client) {
        logger.debug("Checking whether user {} has permission {}", user,
            permission);
        Permission poSearchParam = new Permission();
        poSearchParam.setClientId(client.getClientId());
        poSearchParam.setPermissionId(permission.getPermissionId());

        DefinedPermission definedPermission = (DefinedPermission) getPermissionForParent(client.getUniqueId(), poSearchParam);

        if (definedPermission == null || !definedPermission.getEnabled()) {
            // No such permission defined. Not granted.
            return false;
        }

        if (definedPermission.getGrantedByDefault()) {
            // Granted by default, but has the user been provisioned for this
            // service?
            ScopeAccess provisionedSa = scopeAccessDao.getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                    client.getClientId());
            if (provisionedSa != null) {
                // Provisioned, so granted.
                return true;
            }
        } else {
            Permission grantedPermission = scopeAccessDao.getPermissionByParentAndPermission(user.getUniqueId(), poSearchParam);
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
        final ScopeAccess scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }

        if (scopeAccess instanceof hasAccessToken) {
            ((hasAccessToken) scopeAccess).setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
        logger.debug("Done expiring access token {}", tokenString);
    }

    @Override
    public void expireAllTokensForClient(String clientId) {
        logger.debug("Expiring all tokens for client {}", clientId);
        final Client client = this.clientDao.getClientByClientId(clientId);
        if (client == null) {
            return;
        }
        List<ScopeAccess> saList = this.scopeAccessDao
            .getScopeAccessesByParent(client.getUniqueId());

        for (ScopeAccess sa : saList) {
            if (sa instanceof hasAccessToken) {
                ((hasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
        logger.debug("Done expiring all tokens for client {}", clientId);
    }

    @Override
    public void expireAllTokensForCustomer(String customerId) {
        logger.debug("Expiring all tokens for client {}", customerId);
        List<Client> clients = getAllClientsForCustomerId(customerId);
        List<User> users = getAllUsersForCustomerId(customerId);
        for (Client client : clients) {
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
            if (sa instanceof hasAccessToken) {
                ((hasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
        logger.debug("Done expiring all tokens for user {}", username);
    }

    @Override
    public ScopeAccess getAccessTokenByAuthHeader(String authHeader) {
        logger.debug("Getting access token by auth header {}", authHeader);
        final String tokenStr = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        final ScopeAccess scopeAccess = scopeAccessDao
            .getScopeAccessByAccessToken(tokenStr);
        logger.debug("Done getting access token by auth header {}", authHeader);
        return scopeAccess;
    }

    @Override
    public ClientScopeAccess getClientScopeAccessForClientId(
        String clientUniqueId, String clientId) {
        logger.debug("Getting Client ScopeAccess by clientId", clientId);
        final ClientScopeAccess scopeAccess = (ClientScopeAccess) this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(clientUniqueId, clientId);
        logger.debug("Got Client ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public List<ScopeAccess> getDelegateScopeAccessesForParent(
        String parentUniqueId) {
        logger.debug("Getting Delegate ScopeAccess by parent {}",
            parentUniqueId);
        List<ScopeAccess> sa = this.scopeAccessDao
            .getDelegateScopeAccessesByParent(parentUniqueId);
        logger.debug("Got {} Delegate ScopeAccess by parent {}", sa.size(),
            parentUniqueId);
        return sa;
    }

    @Override
    public ScopeAccess getDelegateScopeAccessForParentByClientId(
        String parentUniqueID, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccess sa = this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(parentUniqueID, clientId);

        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public ScopeAccess getDirectScopeAccessForParentByClientId(
        String parentUniqueID, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccess sa = this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(parentUniqueID, clientId);

        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(
        BaseUser user) {

        if (user == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        logger.debug(
            "Getting or creating password reset scope access for user {}",
            user.getUsername());
        PasswordResetScopeAccess prsa = (PasswordResetScopeAccess) this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                PASSWORD_RESET_CLIENT_ID);
        if (prsa == null) {
            prsa = new PasswordResetScopeAccess();
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
                prsa.setAccessTokenExp(new DateTime().plusSeconds(
                    this.getDefaultTokenExpirationSeconds()).toDate());
                prsa.setAccessTokenString(this.generateToken());
                this.scopeAccessDao.updateScopeAccess(prsa);
            }
        }
        logger.debug(
            "Done getting or creating password reset scope access for user {}",
            user.getUsername());
        return prsa;
    }

    @Override
    public Permission getPermissionForParent(String scopeAccessUniqueId,
        Permission permission) {
        if (permission == null) {
            String errorMsg = String
                .format("Null scope access object instance.");
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        Permission perm = this.scopeAccessDao
            .getPermissionByParentAndPermission(scopeAccessUniqueId, permission);
        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        return perm;
    }

    @Override
    public List<Permission> getPermissionsForParent(String scopeAccessUniqueId,
        Permission permission) {
        if (permission == null) {
            String errorMsg = String
                .format("Null scope access object instance.");
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        List<Permission> perms = this.scopeAccessDao
            .getPermissionsByParentAndPermission(scopeAccessUniqueId,
                permission);
        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        return perms;
    }

    @Override
    public RackerScopeAccess getRackerScopeAccessForClientId(
        String rackerUniqueId, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        final RackerScopeAccess scopeAccess = (RackerScopeAccess) this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(rackerUniqueId, clientId);
        logger.debug("Got Racker ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        logger.debug("Getting ScopeAccess by Access Token {}", accessToken);
        final ScopeAccess scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(accessToken);
        logger.debug("Got ScopeAccess {} by Access Token {}", scopeAccess,
            accessToken);
        return scopeAccess;
    }

    @Override
    public DelegatedClientScopeAccess getScopeAccessByAuthCode(
        String authorizationCode) {
        logger.debug("Getting ScopeAccess by Authorization Code {}",
            authorizationCode);
        final DelegatedClientScopeAccess scopeAccess = this.scopeAccessDao
            .getScopeAccessByAuthorizationCode(authorizationCode);
        logger.debug("Got ScopeAccess {} by Authorization Code {}",
            scopeAccess, authorizationCode);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        logger.debug("Getting ScopeAccess by Refresh Token {}", refreshToken);
        final ScopeAccess scopeAccess = this.scopeAccessDao
            .getScopeAccessByRefreshToken(refreshToken);
        logger.debug("Got ScopeAccess {} by Refresh Token {}", scopeAccess,
            refreshToken);
        return scopeAccess;
    }

    @Override
    public List<ScopeAccess> getScopeAccessesForParentByClientId(
        String parentUniqueId, String clientId) {
        logger.debug("Getting ScopeAccesses by parent {} and clientId",
            parentUniqueId, clientId);
        List<ScopeAccess> sa = this.scopeAccessDao.getScopeAccessesByParentAndClientId(parentUniqueId, clientId);
        logger.debug("Got {} ScopeAccesses for parent",sa);
        return sa;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientId(String userUniqueId,
        String clientId) {
        logger.debug("Getting User ScopeAccess by clientId {}", clientId);
        final UserScopeAccess scopeAccess = (UserScopeAccess) this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(userUniqueId, clientId);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }
    
    @Override
    public List<UserScopeAccess> getUserScopeAccessForUsername(String username) {
        logger.debug("Getting User ScopeAccess by username {}", username);
        final List<UserScopeAccess> scopeAccessList = this.scopeAccessDao.getScopeAccessByUsername(username);
        if ( scopeAccessList == null) {
            String errMsg = String.format("Could not find scope accesses for the user {}",username);
            logger.error(errMsg);
            throw new NotFoundException(errMsg);
        }
        logger.debug("Got User ScopeAccesses {} by username {}", scopeAccessList,
            username);
        return scopeAccessList;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByMossoIdAndApiCredentials(
        int mossoId, String apiKey, String clientId) {
        logger.debug("Getting mossoId {} ScopeAccess by clientId {}", mossoId,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByMossoIdAndAPIKey(mossoId, apiKey);

        handleAuthenticationFailure((new Integer(mossoId)).toString(), result);

        final UserScopeAccess scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser());

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(
                getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByNastIdAndApiCredentials(
        String nastId, String apiKey, String clientId) {
        logger.debug("Getting nastId {} ScopeAccess by clientId {}", nastId,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByNastIdAndAPIKey(nastId, apiKey);

        handleAuthenticationFailure(nastId, result);

        final UserScopeAccess scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser());
        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(
                getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(
        String username, String apiKey, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByAPIKey(username, apiKey);

        handleAuthenticationFailure(username, result);

        final UserScopeAccess scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser());

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {
            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(
                getDefaultCloudAuthTokenExpirationSeconds()).toDate());
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }

        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(
        String username, String password, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username,
            clientId);

        final UserAuthenticationResult result = this.userDao.authenticate(
            username, password);

        handleAuthenticationFailure(username, result);

        final UserScopeAccess scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser());

        if (scopeAccess.isAccessTokenExpired(new DateTime())) {

            scopeAccess.setAccessTokenString(this.generateToken());
            scopeAccess.setAccessTokenExp(new DateTime().plusSeconds(
                getDefaultCloudAuthTokenExpirationSeconds()).toDate());

            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }

        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);

        return scopeAccess;
    }

    @Override
    public GrantedPermission grantPermissionToClient(String parentUniqueId,
        GrantedPermission permission) {
        if (permission == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.info("Granting permission {} to client {}", parentUniqueId,
            permission.getPermissionId());
        Client dClient = this.clientDao.getClientByCustomerIdAndClientId(
            permission.getCustomerId(), permission.getClientId());

        if (dClient == null) {
            String errMsg = String.format("Client %s not found",
                permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        Permission perm = this.scopeAccessDao
            .getPermissionByParentAndPermission(dClient.getUniqueId(),
                permission);
        if (perm == null) {
            String errMsg = String.format(
                "Permission %s not found for client %s",
                permission.getPermissionId(), permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccess sa = this.getDirectScopeAccessForParentByClientId(
            parentUniqueId, perm.getClientId());

        if (sa == null) {
            sa = new ScopeAccess();
            sa.setClientId(permission.getClientId());
            sa.setClientRCN(permission.getCustomerId());
            sa = this.addDirectScopeAccess(parentUniqueId, sa);
        }

        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        grantedPerm = this.scopeAccessDao.grantPermission(sa.getUniqueId(),
            permission);

        logger.info("Done granting permission {} to client {}", parentUniqueId,
            permission.getPermissionId());
        return grantedPerm;
    }

    @Override
    public GrantedPermission grantPermissionToUser(User user,
        GrantedPermission permission) {
        if (permission == null) {
            String errMsg = String.format("Null argument passed in.");
            logger.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        logger.info("Granting permission {} to user {}", user.getUsername(),
            permission.getPermissionId());
        
        Client dClient = this.clientDao.getClientByCustomerIdAndClientId(
            permission.getCustomerId(), permission.getClientId());
        if (dClient == null) {
            String errMsg = String.format("Client %s not found",
                permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        Permission perm = this.scopeAccessDao
            .getPermissionByParentAndPermission(dClient.getUniqueId(),
                permission);
        if (perm == null) {
            String errMsg = String.format(
                "Permission %s not found for client %s",
                permission.getPermissionId(), permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        UserScopeAccess sa = (UserScopeAccess) this
            .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                perm.getClientId());

        if (sa == null) {
            sa = new UserScopeAccess();
            sa.setClientId(permission.getClientId());
            sa.setClientRCN(permission.getCustomerId());
            sa.setUsername(user.getUsername());
            sa.setUserRCN(user.getCustomerId());
            sa = (UserScopeAccess) this.addDirectScopeAccess(
                user.getUniqueId(), sa);
        }

        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        grantedPerm = this.scopeAccessDao.grantPermission(sa.getUniqueId(),
            permission);

        logger.info("Done granting permission {} to user {}",
            user.getUsername(), permission.getPermissionId());
        return grantedPerm;
    }

    @Override
    public void removePermission(Permission permission) {
        if (permission == null) {
            String errorMsg = String
                .format("Null scope access object instance.");
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
                .format("Null scope access object instance.");
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
                .format("Null scope access object instance.");
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        logger.info("Updating ScopeAccess {}", scopeAccess);

        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        logger.info("Updated ScopeAccess {}", scopeAccess);
    }

    private UserScopeAccess checkAndGetUserScopeAccess(String clientId,
        BaseUser user) {
        final UserScopeAccess scopeAccess = this.getUserScopeAccessForClientId(
            user.getUniqueId(), clientId);

        if (scopeAccess == null) {
            String errMsg = "Scope access not found.";
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return scopeAccess;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // private functions
    private List<Client> getAllClientsForCustomerId(final String customerId) {
        logger.debug("Getting all clients for customer {}", customerId);
        final List<Client> clientsList = new ArrayList<Client>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Clients clientsObj = clientDao.getClientsByCustomerId(
                customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        logger.debug("Done getting all clients for customer {}", customerId);
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(final String customerId) {
        logger.debug("Getting all users for customer {}", customerId);
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userDao.getUsersByCustomerId(customerId,
                offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        logger.debug("Done getting all users for customer {}", customerId);
        return usersList;
    }

    private int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    private int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }
    
    private void handleAuthenticationFailure(String username,
        final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("User %s not authenticated.",
                username);
            logger.warn(errorMessage);
            throw new NotAuthenticatedException();
        }
    }
}
