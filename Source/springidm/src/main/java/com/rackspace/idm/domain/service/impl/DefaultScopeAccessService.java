package com.rackspace.idm.domain.service.impl;

import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;

public class DefaultScopeAccessService implements ScopeAccessService {

    private UserService userService;
    private ClientService clientService;
    private ScopeAccessObjectDao scopeAccessDao;

    private AuthHeaderHelper authHeaderHelper;
    private Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public ScopeAccessObject getAccessTokenByAuthHeader(String authHeader) {

        String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        ScopeAccessObject scopeAccess = scopeAccessDao
            .getScopeAccessByAccessToken(tokenStr);
        return scopeAccess;
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(accessTokenStr);
        if (scopeAccess instanceof hasAccessToken) {
            if (((hasAccessToken) scopeAccess)
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
    public ClientScopeAccessObject getClientScopeAccessForClientId(
        String clientUniqueId, String clientId) {
        logger.debug("Getting Client ScopeAccess by clientId", clientId);
        ClientScopeAccessObject scopeAccess = (ClientScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(clientUniqueId, clientId);
        logger.debug("Got Client ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public RackerScopeAccessObject getRackerScopeAccessForClientId(
        String rackerUniqueId, String clientId) {
        logger.debug("Getting Racker ScopeAccess by clientId", clientId);
        RackerScopeAccessObject scopeAccess = (RackerScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(rackerUniqueId, clientId);
        logger.debug("Got Racker ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public UserScopeAccessObject getUserScopeAccessForClientId(
        String userUniqueId, String clientId) {
        logger.debug("Getting User ScopeAccess by clientId", clientId);
        UserScopeAccessObject scopeAccess = (UserScopeAccessObject) this.scopeAccessDao
            .getScopeAccessForParentByClientId(userUniqueId, clientId);
        logger.debug("Got User ScopeAccess {} by clientId {}", scopeAccess,
            clientId);
        return scopeAccess;
    }

    @Override
    public ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken) {
        logger.debug("Getting ScopeAccess by Refresh Token {}", refreshToken);
        ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByRefreshToken(refreshToken);
        logger.debug("Got ScopeAccess {} by Refresh Token {}", scopeAccess,
            refreshToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccessObject getScopeAccessByAccessToken(String accessToken) {
        logger.debug("Getting ScopeAccess by Access Token {}", accessToken);
        ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(accessToken);
        logger.debug("Got ScopeAccess {} by Access Token {}", scopeAccess,
            accessToken);
        return scopeAccess;
    }

    @Override
    public void updateScopeAccess(ScopeAccessObject scopeAccess) {
        logger.info("Updating ScopeAccess {}", scopeAccess);
        this.scopeAccessDao.updateScopeAccess(scopeAccess);
        logger.info("Updated ScopeAccess {}", scopeAccess);
    }

    @Override
    public void expireAllTokensForUser(String username) {
        User user = this.userService.getUser(username);
        if (user == null) {
            return;
        }
        
        List<ScopeAccessObject> saList = this.scopeAccessDao
            .getScopeAccessesByParent(user.getUniqueId());
        
        for (ScopeAccessObject sa : saList) {
            UserScopeAccessObject usa = (UserScopeAccessObject) sa;
            usa.setAccessTokenExpired();
            usa.setRefreshTokenExpired();
            this.scopeAccessDao.updateScopeAccess(usa);
        }
    }

    @Override
    public void expireAllTokensForClient(String clientId) {
        Client client = this.clientService.getById(clientId);
        if (client == null) {
            return;
        }
        // TODO: Need to implement this method in DAO
        // List<ScopeAccessObject> saList = this.scopeAccessDao.g
    }

    @Override
    public void expireAllTokensForCustomer(String customerId) {
        // TODO Auto-generated method stub

    }

    private int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    private int getCloudAuthDefaultTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void expireAccessToken(String tokenString) {
        ScopeAccessObject scopeAccess = this.scopeAccessDao
            .getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            return;
        }
        
        if (scopeAccess instanceof hasAccessToken) {
            ((hasAccessToken)scopeAccess).setAccessTokenExpired();
            this.scopeAccessDao.updateScopeAccess(scopeAccess);
        }
    }
}
