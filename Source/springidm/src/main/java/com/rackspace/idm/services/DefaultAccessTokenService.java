package com.rackspace.idm.services;

import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.UUID;

public class DefaultAccessTokenService implements AccessTokenService {
    private AccessTokenDao tokenDao;
    private RefreshTokenDao refreshTokenDao;
    private ClientDao clientDao;
    private Logger logger;
    private UserService userService;
    private int defaultTokenExpirationSeconds;
    private int defaultCloudAuthTokenExpirationSeconds;
    private String dataCenterPrefix;
    private boolean isTrustedServer;
    private AuthHeaderHelper authHeaderHelper;

    public DefaultAccessTokenService(TokenDefaultAttributes defaultAttributes, AccessTokenDao tokenDao,
                                     ClientDao clientDao, UserService userService, AuthHeaderHelper authHeaderHelper,
                                     Logger logger) {
        this.tokenDao = tokenDao;
        this.clientDao = clientDao;
        this.userService = userService;
        this.logger = logger;
        this.defaultTokenExpirationSeconds = defaultAttributes.getExpirationSeconds();
        this.defaultCloudAuthTokenExpirationSeconds = defaultAttributes.getCloudAuthExpirationSeconds();
        this.dataCenterPrefix = defaultAttributes.getDataCenterPrefix();
        this.isTrustedServer = defaultAttributes.getIsTrustedServer();
        this.authHeaderHelper = authHeaderHelper;
    }

    @Deprecated
    public DefaultAccessTokenService(TokenDefaultAttributes defaultAttributes, AccessTokenDao tokenDao,
                                     RefreshTokenDao refreshTokenDao, ClientDao clientDao, UserService userService,
                                     AuthHeaderHelper authHeaderHelper, Logger logger) {

        this.tokenDao = tokenDao;
        this.refreshTokenDao = refreshTokenDao;
        this.clientDao = clientDao;
        this.userService = userService;
        this.logger = logger;
        this.defaultTokenExpirationSeconds = defaultAttributes.getExpirationSeconds();
        this.defaultCloudAuthTokenExpirationSeconds = defaultAttributes.getCloudAuthExpirationSeconds();
        this.dataCenterPrefix = defaultAttributes.getDataCenterPrefix();
        this.isTrustedServer = defaultAttributes.getIsTrustedServer();
        this.authHeaderHelper = authHeaderHelper;
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
        if (isTrustedServer) {
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
        return createPasswordResetAccessTokenForUser(user, clientId, defaultTokenExpirationSeconds);
    }

    @Override
    public AccessToken createPasswordResetAccessTokenForUser(User user, String clientId, int expirationTimeInSeconds) {
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

        Client tokenRequestor = clientDao.findByClientId(clientId);
        if (tokenRequestor == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        if (StringUtils.isBlank(tokenRequestor.getInum())) {
            throw new IllegalArgumentException(
                    String.format("Client %s is missing i-number", tokenRequestor.getClientId()));
        }

        AccessToken accessToken =
                new AccessToken(tokenString, new DateTime().plusSeconds(expirationTimeInSeconds), user,
                        tokenRequestor.getBaseClientWithoutClientPerms(), IDM_SCOPE.SET_PASSWORD);
        tokenDao.save(accessToken);
        logger.debug("Created Password Reset Access Token For User: {} : {}", owner, accessToken);
        return accessToken;
    }

    @Override
    public AccessToken createAccessTokenForClient(BaseClient client) {
        return createAccessTokenForClient(client, defaultTokenExpirationSeconds);
    }

    @Override
    public AccessToken createAccessTokenForClient(BaseClient client, int expirationSeconds) {
        logger.debug("Creating Access Token For Client: {}", client);
        String tokenString = generateTokenWithDcPrefix();
        AccessToken accessToken =
                new AccessToken(tokenString, new DateTime().plusSeconds(expirationSeconds), null, client,
                        IDM_SCOPE.FULL);
        tokenDao.save(accessToken);
        logger.debug("Created Access Token For Client: {} : {}", client.getClientId(), accessToken);
        return accessToken;
    }

    public AccessToken createAccessTokenForUser(String username, String clientId) {
        return createAccessTokenForUser(username, clientId, this.defaultTokenExpirationSeconds);
    }

    public AccessToken createAccessTokenForUser(String username, String clientId, int expirationTimeInSeconds) {
        logger.debug("Creating Access Token For User: {}", username);

        Client tokenRequestor = clientDao.findByClientId(clientId);
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
        if (isTrustedServer) {
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
        AccessToken accessToken =
                new AccessToken(tokenString, new DateTime().plusSeconds(expirationTimeInSeconds), user, client,
                        IDM_SCOPE.FULL, isTrustedServer);

        tokenDao.save(accessToken);

        logger.debug("Created Access Token For User: {} : {}", user.getUsername(), accessToken);

        return accessToken;
    }

    public int getDefaultTokenExpirationSeconds() {
        return this.defaultTokenExpirationSeconds;
    }

    public int getCloudAuthDefaultTokenExpirationSeconds() {
        return this.defaultCloudAuthTokenExpirationSeconds;
    }

    public AccessToken getAccessTokenForClient(String clientId, DateTime expiresAfter) {
        logger.debug("Getting Token For Client: {}", clientId);
        Client client = clientDao.findByClientId(clientId);
        if (client == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }
        AccessToken token = tokenDao.findTokenForOwner(client.getClientId(), client.getClientId());
        logger.debug("Got Token For Client: {} - {}", clientId, token);
        return token;
    }

    @Override
    public boolean authenticateAccessToken(String accessTokenStr) {
        logger.debug("Authorizing Token: {}", accessTokenStr);
        Boolean authenticated = false;

        // check token is valid and not expired
        AccessToken accessToken = getAccessTokenByTokenString(accessTokenStr);
        if (accessToken != null && !accessToken.isExpired(new DateTime())) {
            authenticated = true;
        }
        logger.debug("Authorized Token: {} : {}", accessTokenStr, authenticated);
        return authenticated;
    }

    public AccessToken getTokenByBasicCredentials(BaseClient client, BaseUser user, int expirationSeconds,
                                                  DateTime currentTime) {
        AccessToken token = getAccessTokenForUser(user, client, currentTime);

        if (token == null || token.isExpired(currentTime)) {
            token = createAccessTokenForUser(user, client, expirationSeconds);

            logger.debug(String.format("Access Token Created For User: %s : %s", user.getUsername(), token));
        } else {
            logger.debug(String.format("Access Token Found For User: %s by Client : %s", user.getUsername(), token));
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
            logger.error("Incorrect Credentials");
            throw new NotAuthenticatedException("Incorrect Credentials");
        }

        AccessToken token = getAccessTokenForUser(authResult.getUser(), client, currentTime);
        if (token == null || token.isExpired(currentTime)) {
            token = createAccessTokenForUser(authResult.getUser(), client, expirationSeconds);
            logger.debug("Access Token Created For User: {} : {}", authResult.getUser().getUsername(), token);
        } else {
            logger.debug("Access Token Found For User: {} by Client : {}", authResult.getUser().getUsername(), token);
        }
        return token;
    }

    public AccessToken validateToken(String tokenString) {

        logger.debug("Validating Token: {}", tokenString);

        AccessToken token = tokenDao.findByTokenString(tokenString);

        // Check if token is from other data center. Leaving this here.
        // We may need it later. - Dev, October 20, 2010.
        // if (token == null) {
        // token = checkIfTokenIsFromOtherDC(tokenString);
        // }

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

    private String generateTokenWithDcPrefix() {
        String token = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s-%s", this.dataCenterPrefix, token);
    }
}
