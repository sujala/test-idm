package com.rackspace.idm.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.entities.TokenDefaultAttributes;
import com.rackspace.idm.entities.BaseUser;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.util.AuthHeaderHelper;

public class DefaultAccessTokenService implements AccessTokenService {
    private AccessTokenDao tokenDao;
    private RefreshTokenDao refreshTokenDao;
    private ClientDao clientDao;
    private Logger logger;
    private UserService userService;
    private int defaultTokenExpirationSeconds;
    private String dataCenterPrefix;
    private boolean isTrustedServer;
    private AuthHeaderHelper authHeaderHelper;

    public DefaultAccessTokenService(TokenDefaultAttributes defaultAttributes,
        AccessTokenDao tokenDao, RefreshTokenDao refreshTokenDao,
        ClientDao clientDao, UserService userService,AuthHeaderHelper authHeaderHelper,
        Logger logger) {

        this.tokenDao = tokenDao;
        this.refreshTokenDao = refreshTokenDao;
        this.clientDao = clientDao;
        this.userService = userService;
        this.logger = logger;
        this.defaultTokenExpirationSeconds = defaultAttributes
            .getExpirationSeconds();
        this.dataCenterPrefix = defaultAttributes.getDataCenterPrefix();
        this.isTrustedServer = defaultAttributes.getIsTrustedServer();
        this.authHeaderHelper = authHeaderHelper;
    }
    
    public AccessToken getAccessTokenByAuthHeader(String authHeader) {
        String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        return (AccessToken) tokenDao.findByTokenString(tokenStr);
    }

    public AccessToken getAccessTokenByTokenString(String tokenString) {
        return tokenDao.findByTokenString(tokenString);
    }

    public AccessToken createAccessTokenForClient(String clientId) {

        return createAccessTokenForClient(clientId,
            this.defaultTokenExpirationSeconds);
    }

    public AccessToken createAccessTokenForClient(String clientId,
        int expirationTimeInSeconds) {
        logger.debug("Creating Access Token For Client: {}", clientId);
        String tokenString;

        tokenString = generateTokenWithDcPrefix();

        Client owner = clientDao.findByClientId(clientId);
        AccessToken accessToken = new AccessToken(tokenString,
            new DateTime().plusSeconds(expirationTimeInSeconds), null,
            owner.getBaseClient(), IDM_SCOPE.FULL);
        tokenDao.save(accessToken);
        logger.debug("Created Access Token For Client: {} : {}", clientId,
            accessToken);
        return accessToken;

    }

    public AccessToken createAccessTokenForUser(String username, String clientId) {
        return createAccessTokenForUser(username, clientId,
            this.defaultTokenExpirationSeconds);
    }

    public AccessToken createAccessTokenForUser(String username,
        String clientId, int expirationTimeInSeconds) {
        logger.debug("Creating Access Token For User: {}", username);

        Client tokenRequestor = clientDao.findByClientId(clientId);
        if (tokenRequestor == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        String requestor = tokenRequestor.getClientId();
        if (StringUtils.isBlank(requestor)) {
            throw new IllegalArgumentException(String.format(
                "Client %s is missing i-number", clientId));
        }

        String owner = username;
        if (!isTrustedServer) {
            User tokenOwner = userService.getUser(username);
            if (tokenOwner == null) {
                String error = "No entry found for username " + username;
                logger.debug(error);
                throw new IllegalStateException(error);
            }
            if (StringUtils.isEmpty(tokenOwner.getInum())) {
                String error = "Inum for user is null: " + username;
                logger.debug(error);
                throw new IllegalStateException(error);
            }

            owner = tokenOwner.getUsername();
        }

        AccessToken accessToken = createToken(username, owner, requestor,
            expirationTimeInSeconds);
        return accessToken;
    }

    private AccessToken createToken(String username, String owner,
        String requestor, int expirationTimeInSeconds) {

        String tokenString = generateTokenWithDcPrefix();

        BaseUser user = new BaseUser();
        BaseClient client = clientDao.findByClientId(requestor).getBaseClient();

        if (isTrustedServer) {
            user.setUsername(username);
        } else {
            user = userService.getUser(username).getBaseUser();
        }

        AccessToken accessToken = new AccessToken(tokenString,
            new DateTime().plusSeconds(expirationTimeInSeconds), user, client,
            IDM_SCOPE.FULL, isTrustedServer);

        tokenDao.save(accessToken);

        logger.debug("Created Access Token For User: {} : {}", username,
            accessToken);

        return accessToken;
    }

    public AccessToken createPasswordResetAccessTokenForUser(String username,
        String clientId) {
        return createPasswordResetAccessTokenForUser(username, clientId,
            this.defaultTokenExpirationSeconds);
    }

    public AccessToken createPasswordResetAccessTokenForUser(String username,
        String clientId, int expirationTimeInSeconds) {

        logger.debug("Creating Password Reset Access Token For User: {}",
            username);

        String tokenString = generateTokenWithDcPrefix();

        User tokenOwner = userService.getUser(username);
        if (tokenOwner == null) {
            String error = "No entry found for username " + username;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        String owner = tokenOwner.getUsername();
        if (StringUtils.isBlank(owner)) {
            throw new IllegalArgumentException(String.format(
                "User %s is missing i-number", username));
        }

        Client tokenRequestor = clientDao.findByClientId(clientId);
        if (tokenRequestor == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        if (StringUtils.isBlank(tokenRequestor.getInum())) {
            throw new IllegalArgumentException(String.format(
                "Client %s is missing i-number", clientId));
        }

        AccessToken accessToken = new AccessToken(tokenString,
            new DateTime().plusSeconds(expirationTimeInSeconds),
            tokenOwner.getBaseUser(), tokenRequestor.getBaseClient(),
            IDM_SCOPE.SET_PASSWORD);
        tokenDao.save(accessToken);
        logger.debug("Created Password Reset Access Token For User: {} : {}",
            username, accessToken);
        return accessToken;
    }

    public int getDefaultTokenExpirationSeconds() {
        return this.defaultTokenExpirationSeconds;
    }

    public AccessToken getAccessTokenForUser(String username, String clientId,
        DateTime expiresAfter) {
        logger.debug("Getting Token For User: {}", username);

        Client client = clientDao.findByClientId(clientId);
        if (client == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        AccessToken token = null;
        if (isTrustedServer) {
            token = tokenDao.findTokenForOwner(username, client.getClientId());
            logger.debug("Got Token For Racker: {} - {}", username, token);
            return token;
        }

        User user = userService.getUser(username);
        if (user == null) {
            String error = "No entry found for username " + username;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        token = tokenDao.findTokenForOwner(username, client.getClientId());
        logger.debug("Got Token For User: {} - {}", username, token);
        return token;
    }

    public AccessToken getAccessTokenForClient(String clientId,
        DateTime expiresAfter) {
        logger.debug("Getting Token For Client: {}", clientId);
        Client client = clientDao.findByClientId(clientId);
        if (client == null) {
            String error = "No entry found for clientId " + clientId;
            logger.debug(error);
            throw new IllegalStateException(error);
        }
        AccessToken token = tokenDao.findTokenForOwner(client.getClientId(),
            client.getClientId());
        logger.debug("Got Token For Client: {} - {}", clientId, token);
        return token;
    }

    public void revokeToken(String tokenStringRequestingDelete,
        String tokenToDelete) {

        logger.info("Deleting Token {}", tokenToDelete);

        AccessToken deletingToken = tokenDao
            .findByTokenString(tokenToDelete);
        if (deletingToken == null) {
            String error = "No entry found for token " + deletingToken;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        AccessToken requestingToken = tokenDao
            .findByTokenString(tokenStringRequestingDelete);
        if (requestingToken == null) {
            String error = "No entry found for token "
                + tokenStringRequestingDelete;
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        // Only CustomerIdm Client and Client that got token or the user of the
        // toke are authorized to revoke token

        boolean isCustomerIdm = requestingToken.isClientToken()
            && requestingToken.getTokenClient().getClientId()
                .equals(GlobalConstants.IDM_CLIENT_ID);
        
        boolean isRequestor = requestingToken.isClientToken()
            && requestingToken.getTokenClient().getClientId()
                .equals(deletingToken.getTokenClient().getClientId());
        
        boolean isOwner = requestingToken.getTokenUser() != null
            && deletingToken.getTokenUser() != null
            && requestingToken.getTokenUser().getUsername()
                .equals(deletingToken.getTokenUser().getUsername());

        boolean authorized = isCustomerIdm || isRequestor || isOwner;

        if (!authorized) {
            String errMsg = String.format(
                "Requesting token %s not authorized to revoke token %s",
                tokenStringRequestingDelete, tokenToDelete);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (deletingToken.getRequestor() == null) {
            String error = String.format("Token %s does not have a requestor",
                deletingToken.getTokenString());
            logger.debug(error);
            throw new IllegalStateException(error);
        }

        deleteRefreshTokenByAccessToken(deletingToken);

        tokenDao.delete(deletingToken.getTokenString());
        logger.info("Deleted Token {}", deletingToken);
    }

    public AccessToken validateToken(String tokenString) {

        logger.debug("Validating Token: {}", tokenString);

        AccessToken token = tokenDao
            .findByTokenString(tokenString);

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

    private void deleteRefreshTokenByAccessToken(AccessToken accessToken) {

        // Clients do not get Refresh Tokens so there is no need to
        // revoke one if the access token is for a client
        if (accessToken.isClientToken()) {
            return;
        }

        // FIXME: With the new AccessToken that includes user and client
        // info we no longer need to do a lot of these calls.
        // So ... Here's the new method we should use
        //
        // String username = accessToken.getOwner();
        // String clientId = accessToken.getRequestor();
        //
        // if (!StringUtils.isEmpty(accessToken.getOwner())
        // && !StringUtils.isEmpty(accessToken.getRequestor())) {
        // Set<String> tokenRequestors = new HashSet<String>();
        // tokenRequestors.add(clientId);
        // refreshTokenDao.deleteAllTokensForUser(username, tokenRequestors);
        // }

        String username = StringUtils.EMPTY;
        String clientId = StringUtils.EMPTY;

        User tokenUser = userService.getUser(accessToken.getOwner());
        if (tokenUser != null) {
            username = tokenUser.getUsername();
        }
        Client tokenClient = clientDao.findByClientId(accessToken
            .getRequestor());
        if (tokenClient != null) {
            clientId = tokenClient.getClientId();
        }
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(clientId)) {
            Set<String> tokenRequestors = new HashSet<String>();
            tokenRequestors.add(clientId);
            refreshTokenDao.deleteAllTokensForUser(username, tokenRequestors);
        }
    }

    private String generateTokenWithDcPrefix() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenString = String.format("%s-%s", this.dataCenterPrefix,
                token);
        return tokenString;
    }
}
