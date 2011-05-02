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
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;

public class DefaultScopeAccessService implements ScopeAccessService {

    private static final String PASSWORD_RESET_CLIENT_ID = "PASSWORDRESET";

    private final AuthHeaderHelper authHeaderHelper;
    private final ClientDao clientDao;
    private final Configuration config;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScopeAccessObjectDao scopeAccessDao;
    private final UserDao userDao;

    public DefaultScopeAccessService(UserDao userDao, ClientDao clientDao,
        ScopeAccessObjectDao scopeAccessDao, AuthHeaderHelper authHeaderHelper,
        Configuration config) {
        this.userDao = userDao;
        this.clientDao = clientDao;
        this.scopeAccessDao = scopeAccessDao;
        this.authHeaderHelper = authHeaderHelper;
        this.config = config;
    }

    @Override
    public ScopeAccessObject addScopeAccess(String parentUniqueId,
        ScopeAccessObject scopeAccess) {
        logger.info("Adding scopeAccess {}", scopeAccess);
        ScopeAccessObject newScopeAccess = this.scopeAccessDao.addScopeAccess(
            parentUniqueId, scopeAccess);
        logger.info("Added scopeAccess {}", scopeAccess);
        return newScopeAccess;
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        final ScopeAccessObject scopeAccess = this.scopeAccessDao
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
    public void deleteScopeAccess(ScopeAccessObject scopeAccess) {
        logger.info("Deleting ScopeAccess {}", scopeAccess);
        this.scopeAccessDao.deleteScopeAccess(scopeAccess);
        logger.info("Deleted ScopeAccess {}", scopeAccess);
    }

    @Override
    public boolean doesAccessTokenHavePermission(String accessTokenString,
        PermissionObject permission) {
        return this.scopeAccessDao.doesAccessTokenHavePermission(
            accessTokenString, permission);
    }

    @Override
    public void expireAccessToken(String tokenString) {
        final ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }

        if (scopeAccess instanceof hasAccessToken) {
            ((hasAccessToken) scopeAccess).setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
    }

    @Override
    public void expireAllTokensForClient(String clientId) {
        final Client client = this.clientDao.getClientByClientId(clientId);
        if (client == null) {
            return;
        }
        List<ScopeAccessObject> saList = this.scopeAccessDao
            .getScopeAccessesByParent(client.getUniqueId());

        for (ScopeAccessObject sa : saList) {
            if (sa instanceof hasAccessToken) {
                ((hasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
    }

    @Override
    public void expireAllTokensForCustomer(String customerId) {
        List<Client> clients = getAllClientsForCustomerId(customerId);
        List<User> users = getAllUsersForCustomerId(customerId);
        for (Client client : clients) {
            this.expireAllTokensForClient(client.getClientId());
        }
        for (User user : users) {
            this.expireAllTokensForUser(user.getUsername());
        }
    }

    @Override
    public void expireAllTokensForUser(String username) {
        final User user = this.userDao.getUserByUsername(username);
        if (user == null) {
            return;
        }

        final List<ScopeAccessObject> saList = this.scopeAccessDao
            .getScopeAccessesByParent(user.getUniqueId());

        for (final ScopeAccessObject sa : saList) {
            if (sa instanceof hasAccessToken) {
                ((hasAccessToken) sa).setAccessTokenExpired();
                this.scopeAccessDao.updateScopeAccess(sa);
            }
        }
    }

    // private functions
    private List<Client> getAllClientsForCustomerId(final String customerId) {
        final List<Client> clientsList = new ArrayList<Client>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Clients clientsObj = clientDao.getClientsByCustomerId(
                customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        return clientsList;
    }

    private List<User> getAllUsersForCustomerId(final String customerId) {
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userDao.getUsersByCustomerId(customerId,
                offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        return usersList;
    }

    @Override
    public ScopeAccessObject getAccessTokenByAuthHeader(String authHeader) {
        final String tokenStr = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        final ScopeAccessObject scopeAccess = scopeAccessDao
            .getScopeAccessByAccessToken(tokenStr);
        return scopeAccess;
    }

    @Override
    public ClientScopeAccessObject getClientScopeAccessForClientId(
        String clientUniqueId, String clientId) {
        logger.debug("Getting Client ScopeAccess by clientId", clientId);
        final ClientScopeAccessObject scopeAccess = (ClientScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(clientUniqueId, clientId);
        logger.debug("Got Client ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public PasswordResetScopeAccessObject getOrCreatePasswordResetScopeAccessForUser(
        String userUniqueId) {
        PasswordResetScopeAccessObject prsa = (PasswordResetScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(userUniqueId,
                PASSWORD_RESET_CLIENT_ID);
        if (prsa == null) {
            prsa = new PasswordResetScopeAccessObject();
            prsa.setAccessTokenExp(new DateTime().plusSeconds(
                this.getDefaultTokenExpirationSeconds()).toDate());
            prsa.setAccessTokenString(this.generateToken());
            prsa.setClientId(PASSWORD_RESET_CLIENT_ID);
            this.scopeAccessDao.addScopeAccess(userUniqueId, prsa);
        } else {
            if (prsa.isAccessTokenExpired(new DateTime())) {
                prsa.setAccessTokenExp(new DateTime().plusSeconds(
                    this.getDefaultTokenExpirationSeconds()).toDate());
                prsa.setAccessTokenString(this.generateToken());
                this.scopeAccessDao.updateScopeAccess(prsa);
            }
        }
        return prsa;
    }

    @Override
    public RackerScopeAccessObject getRackerScopeAccessForClientId(
        String rackerUniqueId, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        final RackerScopeAccessObject scopeAccess = (RackerScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(rackerUniqueId, clientId);
        logger.debug("Got Racker ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public ScopeAccessObject getScopeAccessByAccessToken(String accessToken) {
        logger.debug("Getting ScopeAccess by Access Token {}", accessToken);
        final ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(accessToken);
        logger.debug("Got ScopeAccess {} by Access Token {}", scopeAccess,
            accessToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken) {
        logger.debug("Getting ScopeAccess by Refresh Token {}", refreshToken);
        final ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByRefreshToken(refreshToken);
        logger.debug("Got ScopeAccess {} by Refresh Token {}", scopeAccess,
            refreshToken);
        return scopeAccess;
    }

    @Override
    public UserScopeAccessObject getUserScopeAccessForClientId(
        String userUniqueId, String clientId) {
        logger.debug("Getting User ScopeAccess by clientId {}", clientId);
        final UserScopeAccessObject scopeAccess = (UserScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(userUniqueId, clientId);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccessObject getUserScopeAccessForClientIdByUsernameAndApiCredentials(
        String username, String apiKey, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByAPIKey(username, apiKey);
        
        if (!result.isAuthenticated()) {
            throw new NotAuthenticatedException();
        }
        
        final UserScopeAccessObject scopeAccess = checkAndGetUserScopeAccess(clientId, result.getUser().getUsername());
        
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
    public UserScopeAccessObject getUserScopeAccessForClientIdByMossoIdAndApiCredentials(
        int mossoId, String apiKey, String clientId) {
        logger.debug("Getting mossoId {} ScopeAccess by clientId {}", mossoId,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByMossoIdAndAPIKey(mossoId, apiKey);
        if (!result.isAuthenticated()) {
            throw new NotAuthenticatedException();
        }
        final UserScopeAccessObject scopeAccess = checkAndGetUserScopeAccess(clientId, result.getUser().getUsername());
            
        
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
    public UserScopeAccessObject getUserScopeAccessForClientIdByNastIdAndApiCredentials(
        String nastId, String apiKey, String clientId) {
        logger.debug("Getting nastId {} ScopeAccess by clientId {}", nastId,
            clientId);
        final UserAuthenticationResult result = this.userDao
            .authenticateByNastIdAndAPIKey(nastId, apiKey);
        if (!result.isAuthenticated()) {
            throw new NotAuthenticatedException();
        }
        final UserScopeAccessObject scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser().getUsername());
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
    public UserScopeAccessObject getUserScopeAccessForClientIdByUsernameAndPassword(
        String username, String password, String clientId) {
        logger.debug("Getting User {} ScopeAccess by clientId {}", username,
            clientId);

        final UserAuthenticationResult result = this.userDao.authenticate(
            username, password);

        if (!result.isAuthenticated()) {
            throw new NotAuthenticatedException();
        }

        final UserScopeAccessObject scopeAccess = checkAndGetUserScopeAccess(
            clientId, result.getUser().getUsername());

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

    private UserScopeAccessObject checkAndGetUserScopeAccess(String clientId,
        String userName) {
        final UserScopeAccessObject scopeAccess = this
            .getUserScopeAccessForClientId(userName, clientId);

        if (scopeAccess == null) {
            String errMsg = "Scope access not found.";
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return scopeAccess;
    }

    @Override
    public PermissionObject grantPermission(String parentUniqueId,
        PermissionObject permission) {
        Client dClient = this.clientDao.getClientByCustomerIdAndClientId(
            permission.getCustomerId(), permission.getClientId());

        if (dClient == null) {
            String errMsg = String.format("Client %s not found", permission
                .getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        PermissionObject perm = this.scopeAccessDao
            .getPermissionByParentAndPermissionId(dClient.getUniqueId(),
                permission);
        if (perm == null) {
            String errMsg = String.format(
                "Permission %s not found for client %s", permission
                    .getPermissionId(), permission.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccessObject sa = this.getScopeAccessForParentByClientId(
            parentUniqueId, perm.getClientId());

        if (sa == null) {
            sa = new ScopeAccessObject();
            sa.setClientId(permission.getClientId());
            sa.setClientRCN(permission.getCustomerId());
            sa = this.addScopeAccess(parentUniqueId, sa);
        }

        PermissionObject grantedPerm = new PermissionObject();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        grantedPerm = this.addPermissionToScopeAccess(sa.getUniqueId(),
            grantedPerm);

        return grantedPerm;
    }

    @Override
    public void updateScopeAccess(ScopeAccessObject scopeAccess) {
        logger.info("Updating ScopeAccess {}", scopeAccess);

        if (scopeAccess == null) {
            String errorMsg = String
                .format("Null scope access object instance.");
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        logger.info("Updated ScopeAccess {}", scopeAccess);
    }

    @Override
    public PermissionObject addPermissionToScopeAccess(
        String scopeAccessUniqueId, PermissionObject permission) {
        logger.info("Adding Permission {} to ScopeAccess {}", permission,
            scopeAccessUniqueId);
        Client client = this.clientDao.getClientByClientId(permission
            .getClientId());

        if (client == null) {
            String errMsg = String.format("Client %s not found", permission
                .getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccessObject sa = this.getScopeAccessForParentByClientId(client
            .getUniqueId(), client.getClientId());
        PermissionObject exists = this.scopeAccessDao
            .getPermissionByParentAndPermissionId(sa.getUniqueId(), permission);
        if (exists == null) {
            String errMsg = String
                .format("Permission %s not found", permission);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        PermissionObject perm = this.scopeAccessDao.grantPermission(
            scopeAccessUniqueId, permission);
        logger.info("Adding Permission {} to ScopeAccess {}", permission,
            scopeAccessUniqueId);
        return perm;
    }

    @Override
    public PermissionObject getPermissionForParent(String scopeAccessUniqueId,
        PermissionObject permission) {
        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        PermissionObject perm = this.scopeAccessDao
            .getPermissionByParentAndPermissionId(scopeAccessUniqueId,
                permission);
        logger.debug("Getting Permission {} on ScopeAccess {}", permission,
            scopeAccessUniqueId);
        return perm;
    }

    @Override
    public ScopeAccessObject getScopeAccessForParentByClientId(
        String parentUniqueID, String clientId) {
        logger.debug("Getting by clientId {}", clientId);
        ScopeAccessObject sa = this.scopeAccessDao
            .getScopeAccessForParentByClientId(parentUniqueID, clientId);

        logger.debug("Got by clientId {}", clientId);
        return sa;
    }

    @Override
    public void removePermission(PermissionObject permission) {
        logger.info("Removing Permission {}", permission);
        this.scopeAccessDao.removePermissionFromScopeAccess(permission);
        logger.info("Removing Permission {}", permission);
    }

    @Override
    public void updatePermission(PermissionObject permission) {
        logger.info("Updating Permission {}", permission);
        this.scopeAccessDao.updatePermissionForScopeAccess(permission);
        logger.info("Updated Permission {}", permission);
    }

    private int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    private int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }
}
