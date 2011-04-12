package com.rackspace.idm.domain.service.impl;

import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.XdcAccessTokenDao;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.util.AuthHeaderHelper;

public class DefaultAccessTokenService implements AccessTokenService {
    private AccessTokenDao tokenDao;
    private XdcAccessTokenDao xdcTokenDao;
    private ClientDao clientDao;
    private UserService userService;
    
    private AuthHeaderHelper authHeaderHelper;
    private Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultAccessTokenService(AccessTokenDao tokenDao, ClientDao clientDao, UserService userService,
        XdcAccessTokenDao xdcTokenDao, AuthHeaderHelper authHeaderHelper, Configuration config) {
        this.tokenDao = tokenDao;
        this.clientDao = clientDao;
        this.userService = userService;     
        this.xdcTokenDao = xdcTokenDao;
        this.authHeaderHelper = authHeaderHelper;
        this.config = config;
    }
    
    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        AccessToken accessToken = getAccessTokenByTokenStringGlobally(accessTokenStr);
        if (accessToken != null && !accessToken.isExpired(new DateTime()) )  {
            authenticated = true;
            MDC.put(Audit.WHO, accessToken.getAuditString());
        }

        logger.debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }

    public int getDefaultTokenExpirationSeconds() {
        return config.getInt("token.expirationSeconds");
    }

    public int getCloudAuthDefaultTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    private String getDataCenterPrefix() {
        return config.getString("token.dataCenterPrefix") + "-";
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

    public AccessToken getAccessTokenByAuthHeader(String authHeader) {
        String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        return tokenDao.findByTokenString(tokenStr);
    }

    public AccessToken getAccessTokenByTokenString(String tokenString) {
        return tokenDao.findByTokenString(tokenString);
    }

    @Override
    public AccessToken getAccessTokenForUser(BaseUser user, BaseClient client, DateTime expiresAfter) {
        if (client == null) {
            String error = "No client given.";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        if (user == null) {
            String error = "No user given.";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        AccessToken token = null;
        if (isTrustedServer()) {
            token = tokenDao.findTokenForOwner(user.getUsername(), client.getClientId());
            logger.debug("Got Token For Racker: {} - {}", user.getUsername(), token);
            return token;
        }

        token = tokenDao.findTokenForOwner(user.getUsername(), client.getClientId());
        logger.debug("Got Token For User: {} - {}", user.getUsername(), token);
        return token;
    }

    @Override
    public AccessToken getAccessTokenForClient(BaseClient client, DateTime expiresAfter) {
        logger.debug("Getting Token For Client: {}", client);
        if (client == null) {
            String error = "No client given";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }
        AccessToken token = tokenDao.findTokenForOwner(client.getClientId(), client.getClientId());
        logger.debug("Got Token For Client: {} - {}", client.getClientId(), token);
        return token;
    }
    
    @Override
    public AccessToken getAccessTokenByTokenStringGlobally(String tokenString) {
        AccessToken token = tokenDao.findByTokenString(tokenString);

        // Check if token is from other data center.
        if (token == null && !tokenString.startsWith(getDataCenterPrefix())) {
            try {
                token = xdcTokenDao.findByTokenString(tokenString);
            } catch (Exception e) {
                logger.warn("Exception occurred while attempting xdc token retrieval", e);
            }
            if (token != null) {
                tokenDao.save(token);
            }
        }
        return token;
    }   

    @Override
    public AccessToken createAccessTokenForUser(BaseUser user, BaseClient client, int expirationSeconds) {
        logger.debug("Creating Access Token For User: {}", user);
        if (client == null) {
            String error = "No client given";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        if (user == null) {
            String error = "No user given";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        String requestor = client.getClientId();
        if (StringUtils.isBlank(requestor)) {
            throw new IllegalArgumentException(String.format("Client %s is missing i-number", requestor));
        }

        return createToken(user, client, expirationSeconds);
    }

    @Override
    public AccessToken createPasswordResetAccessTokenForUser(User user, String clientId) {
        return createPasswordResetAccessTokenForUser(user, clientId, getDefaultTokenExpirationSeconds());
    }
    
    @Override
    public AccessToken createPasswordResetAccessTokenForUser(String userName, String clientId) {
        User user = userService.getUser(userName);
        return createPasswordResetAccessTokenForUser(user, clientId, getDefaultTokenExpirationSeconds());
    }   

    @Override
    public AccessToken createPasswordResetAccessTokenForUser(User user, String clientId,
        int expirationTimeInSeconds) {
        logger.debug("Creating Password Reset Access Token For User: {}", user);

        if (user == null) {
            String error = "No user given.";
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        String tokenString = generateTokenWithDcPrefix();

        String owner = user.getUsername();
        if (StringUtils.isBlank(owner)) {
            throw new IllegalArgumentException(String.format("User %s is missing i-number", owner));
        }

        Client tokenRequestor = clientDao.getClientByClientId(clientId);
        if (tokenRequestor == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        if (StringUtils.isBlank(tokenRequestor.getInum())) {
            throw new IllegalArgumentException(String.format("Client %s is missing i-number",
                tokenRequestor.getClientId()));
        }

        AccessToken accessToken = new AccessToken(tokenString,
            new DateTime().plusSeconds(expirationTimeInSeconds), user,
            tokenRequestor.getBaseClientWithoutClientPerms(), IDM_SCOPE.SET_PASSWORD);
        tokenDao.save(accessToken);
        logger.debug("Created Password Reset Access Token For User: {} : {}", owner, accessToken);
        return accessToken;
    }

    @Override
    public AccessToken createAccessTokenForClient(BaseClient client) {
        return createAccessTokenForClient(client, getDefaultTokenExpirationSeconds());
    }

    @Override
    public AccessToken createAccessTokenForClient(BaseClient client, int expirationSeconds) {
        logger.debug("Creating Access Token For Client: {}", client);
        String tokenString = generateTokenWithDcPrefix();
        AccessToken accessToken = new AccessToken(tokenString, new DateTime().plusSeconds(expirationSeconds),
            null, client, IDM_SCOPE.FULL);
        tokenDao.save(accessToken);
        logger.debug("Created Access Token For Client: {} : {}", client.getClientId(), accessToken);
        return accessToken;
    }

    public AccessToken createAccessTokenForUser(String username, String clientId) {
        return createAccessTokenForUser(username, clientId, getDefaultTokenExpirationSeconds());
    }

    public AccessToken createAccessTokenForUser(String username, String clientId, int expirationTimeInSeconds) {
        logger.debug("Creating Access Token For User: {}", username);

        Client tokenRequestor = clientDao.getClientByClientId(clientId);
        if (tokenRequestor == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        String requestor = tokenRequestor.getClientId();
        if (StringUtils.isBlank(requestor)) {
            throw new IllegalArgumentException(String.format("Client %s is missing i-number", clientId));
        }

        BaseUser tokenOwner;
        if (isTrustedServer()) {
            tokenOwner = new BaseUser(username);
        } else {
            tokenOwner = userService.getUser(username);
            if (tokenOwner == null) {
                String error = "No entry found for username " + username;
                logger.debug(error);
                throw new IllegalStateException(error);
            }
        }

        return createToken(tokenOwner, tokenRequestor, expirationTimeInSeconds);
    }

    private AccessToken createToken(BaseUser user, BaseClient client, int expirationTimeInSeconds) {
        String tokenString = generateTokenWithDcPrefix();
        AccessToken accessToken = new AccessToken(tokenString,
            new DateTime().plusSeconds(expirationTimeInSeconds), user, client, IDM_SCOPE.FULL,
            isTrustedServer());

        tokenDao.save(accessToken);

        logger.debug("Created Access Token For User: {} : {}", user.getUsername(), accessToken);

        return accessToken;
    }

    public AccessToken getTokenByUsernameAndPassword(BaseClient client, String username, String password,
        int expirationSeconds, DateTime currentTime) {

        UserAuthenticationResult authResult = userService.authenticate(username, password);

        AccessToken accessToken = getTokenByApiCredentials(client, authResult, expirationSeconds, currentTime);
        return accessToken;
    }

    public AccessToken getAccessTokenForClient(String clientId, DateTime expiresAfter) {
        logger.debug("Getting Token For Client: {}", clientId);
        Client client = clientDao.getClientByClientId(clientId);
        if (client == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }
        AccessToken token = tokenDao.findTokenForOwner(client.getClientId(), client.getClientId());
        logger.debug("Got Token For Client: {} - {}", clientId, token);
        return token;
    }

    

    public AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
        DateTime currentTime) {
        AccessToken token = getAccessTokenForUser(user, client, currentTime);

        if (token == null || token.isExpired(currentTime)) {
            token = createAccessTokenForUser(user, client, expirationSeconds);

            logger.debug(String.format("Access Token Created For User: %s : %s", user.getUsername(), token));
        } else {
            logger.debug(String.format("Access Token Found For User: %s by Client : %s", user.getUsername(),
                token));
        }
        return token;
    }

    public AccessToken getTokenByUsernameAndApiCredentials(BaseClient client, String username, String apiKey,
        int expirationSeconds, DateTime currentTime) {

        UserAuthenticationResult authResult = userService.authenticateWithApiKey(username, apiKey);

        return getTokenByApiCredentials(client, authResult, expirationSeconds, currentTime);
    }

    public AccessToken getTokenByNastIdAndApiCredentials(BaseClient client, String nastId, String apiKey,
        int expirationSeconds, DateTime currentTime) {

        UserAuthenticationResult authResult = userService.authenticateWithNastIdAndApiKey(nastId, apiKey);

        return getTokenByApiCredentials(client, authResult, expirationSeconds, currentTime);
    }

    public AccessToken getTokenByMossoIdAndApiCredentials(BaseClient client, int mossoId, String apiKey,
        int expirationSeconds, DateTime currentTime) {

        UserAuthenticationResult authResult = userService.authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        return getTokenByApiCredentials(client, authResult, expirationSeconds, currentTime);
    }

    private AccessToken getTokenByApiCredentials(BaseClient client, UserAuthenticationResult authResult,
        int expirationSeconds, DateTime currentTime) {

        if (!authResult.isAuthenticated()) {
            logger.warn("Incorrect Credentials");
            throw new NotAuthenticatedException("Incorrect Credentials");
        }

        AccessToken token = getAccessTokenForUser(authResult.getUser(), client, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = createAccessTokenForUser(authResult.getUser(), client, expirationSeconds);
            logger.debug("Access Token Created For User: {} : {}", authResult.getUser().getUsername(), token);
        } else {
            logger.debug("Access Token Found For User: {} by Client : {}",
                authResult.getUser().getUsername(), token);
        }
        return token;
    }

    public AccessToken validateToken(String tokenString) {
        logger.debug("Validating Token: {}", tokenString);

        AccessToken token = getAccessTokenByTokenStringGlobally(tokenString);
        if (token == null || token.getExpiration() <= 0) {
            logger.debug("Token {} Invalid", tokenString);
            return null;
        }

        logger.debug("Token Validated - {}", token);

        return token;
    }

    @Override
    public void delete(String tokenString) {
        tokenDao.delete(tokenString);
    }

    @Override
    public void deleteAllForOwner(String owner) {
        tokenDao.deleteAllTokensForOwner(owner);
    }

    @Override
    public void deleteAllGloballyForOwner(String owner) {
        // Start with the local tokens.
        tokenDao.deleteAllTokensForOwner(owner);
        xdcTokenDao.deleteAllTokensForOwner(owner);
    }

    @Override
    public void deleteAllGloballyForCustomer(String customerId, List<User> users, List<Client> clients) {
        // Start with the local token
        for (User user : users) {
            tokenDao.deleteAllTokensForOwner(user.getUsername());
        }

        for (Client client : clients) {
            tokenDao.deleteAllTokensForOwner(client.getClientId());
        }

        xdcTokenDao.deleteAllTokensForCustomer(customerId);
    }
    
    @Override
    public boolean passwordRotationDurationElapsed(String userName) {
        boolean rotationNeeded = false;
        
        DateTime passwordExpirationDate = userService.getUserPasswordExpirationDate(userName);
         
        if (passwordExpirationDate != null && passwordExpirationDate.isBeforeNow()) {
            return true;
        }
        
        return rotationNeeded;
    }
 
    private String generateTokenWithDcPrefix() {
        String token = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s%s", getDataCenterPrefix(), token);
    }
}
