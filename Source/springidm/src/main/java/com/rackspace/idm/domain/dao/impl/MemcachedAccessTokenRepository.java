package com.rackspace.idm.domain.dao.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.util.PingableService;

public class MemcachedAccessTokenRepository implements AccessTokenDao, PingableService {
    public static final DateTimeFormatter DATE_PARSER = DateTimeFormat.forPattern("yyyyMMddHHmmss.SSS'Z");
    private static final String USER_TOKEN_KEY_POSTFIX = "@ENDUSER";

    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private MemcachedClient memcached;
    private Configuration config;

    public MemcachedAccessTokenRepository(MemcachedClient memcached, Configuration config) {
        this.memcached = memcached;
        this.config = config;
    }

    @Override
    public void save(AccessToken accessToken) {
        // Validate params
        if (accessToken == null) {
            logger.warn("Token is null");
            throw new IllegalArgumentException("Token is null");
        }
        String tokenString = accessToken.getTokenString();
        if (StringUtils.isBlank(tokenString) || accessToken.getExpirationTime() == null) {
            String errMsg = "Token string and/or expiration time values are not present in the given token.";
            logger.warn(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        logger.debug("Adding token: {}", accessToken);

        // Since we will need to look up by both token string and user name, the
        // add operation will make 2 entries.

        // Try adding the token with the token string as the key
        Future<Boolean> resultByTokenStr = memcached.set(tokenString, accessToken.getExpiration(),
            accessToken);
        boolean addedByTokenStr = evaluateCacheOperation(resultByTokenStr, "set token by token string",
            accessToken);
        if (!addedByTokenStr) {
            String errMsg = String.format("Failed to add token by token string with parameter %s",
                accessToken);
            logger.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }
        // do not overwrite client token if password reset flow
        if (accessToken.isRestrictedToSetPassword()) {
            logger.debug("Added restricted token: {}", accessToken);
            return;
        }

        Future<Boolean> resultByOwner;
        if (accessToken.isClientToken()) {
            resultByOwner = memcached.set(accessToken.getOwner(), accessToken.getExpiration(),
                accessToken.getTokenString());
        } else {
            // Try adding the tokenString with the owner as the key
            UserTokenStrings userTokenStrings = getOrCreateLookupMap(accessToken.getOwner(),
                accessToken.isTrusted());
            userTokenStrings.put(accessToken.getRequestor(), accessToken.getExpiration(), tokenString);

            String keyByOwner = getKeyByOwner(accessToken.getOwner(), accessToken.isTrusted());
            int expiration = userTokenStrings.getExpiration(new DateTime());
            resultByOwner = memcached.set(keyByOwner, expiration, userTokenStrings);
        }

        boolean addedByOwner = evaluateCacheOperation(resultByOwner, "set token by owner", accessToken);
        if (!addedByOwner) {
            // Attempt a rollback of the previous operation before bailing
            // with an exception
            memcached.delete(tokenString);
            String errMsg = String.format("Failed to add token by owner with parameter %s", accessToken);
            logger.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }

        logger.debug("Added token: {}", accessToken);
    }

    @Override
    public void delete(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            logger.warn("Token string is null or empty");
            throw new IllegalArgumentException("Token string is null or empty");
        }
        logger.debug("Deleting token with value {}", tokenString);
        AccessToken token = findByTokenString(tokenString);
        if (token == null) {
            logger.debug("Token {} has expired.", tokenString);
            return;
        }

        // delete primary token
        Future<Boolean> resultByTokenStr = memcached.delete(tokenString);
        boolean deletedByTokenStr = evaluateCacheOperation(resultByTokenStr, "delete token by token string",
            tokenString);

        if (token.isRestrictedToSetPassword()) {
            return;
        }

        Future<Boolean> deleteResult;
        if (token.isClientToken()) {
            deleteResult = memcached.delete(token.getOwner());
        } else {
            // delete pointer to token
            UserTokenStrings userTokenStrings = getOrCreateLookupMap(token.getOwner(), token.isTrusted());
            String keyByOwner = getKeyByOwner(token.getOwner(), token.isTrusted());
            deleteResult = removeUserTokensByRequestor(userTokenStrings, keyByOwner, token.getRequestor());
        }
        boolean deletedByOwner = evaluateCacheOperation(deleteResult, "delete token by owner", tokenString);

        if (!deletedByTokenStr || !deletedByOwner) {
            String errMsg = String.format("Failed to delete token %s", tokenString);
            logger.warn(errMsg);
            throw new IllegalStateException(errMsg);
        }

    }

    /**
     * Will NOT find expired tokens.
     */
    @Override
    public AccessToken findByTokenString(String tokenString) {

        if (StringUtils.isBlank(tokenString)) {
            logger.warn("Token string is null or empty");
            throw new IllegalArgumentException("Token string is null or empty");
        }
        logger.debug("Finding token with value {}", tokenString);
        AccessToken token = (AccessToken) memcached.get(tokenString);
        if (token == null) {
            logger.debug("No token found with value {}", tokenString);
            return null;
        }

        if (token.isRestrictedToSetPassword()) {
            return token;
        }

        // Do a consistency check to make sure there is also the same token
        // stored by owner/requestor IDs.
        String tokenStrByRequestor;
        UserTokenStrings userTokenStrings = null;
        if (token.isClientToken()) {
            tokenStrByRequestor = (String) memcached.get(token.getOwner());
        } else {
            userTokenStrings = getOrCreateLookupMap(token.getOwner(), token.isTrusted());
            tokenStrByRequestor = userTokenStrings.get(token.getRequestor());
        }

        if (token.getTokenString().equals(tokenStrByRequestor)) {
            logger.debug("Found token with value {}", token);
            return token;
        }

        // The corresponding entry stored with key of owner_requestor was not
        // found or did not match. Therefore, the data is in an inconsistent
        // state.
        logger.warn("Token cache in an inconsistent state. Deleting {}", token);
        if (token.isClientToken()) {
            memcached.delete(token.getOwner());
        } else {
            String keyByOwner = getKeyByOwner(token.getOwner(), token.isTrusted());
            removeUserTokensByRequestor(userTokenStrings, keyByOwner, token.getRequestor());
        }
        memcached.delete(tokenString);

        logger.debug("No token found with value {}", tokenString);
        return null;
    }

    /**
     * Will NOT find expired tokens.
     */
    @Override
    public AccessToken findTokenForOwner(String owner, String requestor) {
        if (StringUtils.isBlank(owner)) {
            logger.warn("Owner value is null or empty");
            throw new IllegalArgumentException("Owner value is null or empty");
        }
        logger.debug("Finding token with owner {} and requestor {}", owner, requestor);
        String tokenStrByRequestor;
        UserTokenStrings userTokenStrings = null;
        boolean isClientToken = owner.equals(requestor);
        if (isClientToken) {
            tokenStrByRequestor = (String) memcached.get(owner);
        } else {
            userTokenStrings = getOrCreateLookupMap(owner, isRackerRequestor(requestor));
            tokenStrByRequestor = userTokenStrings.get(requestor);
        }

        if (StringUtils.isBlank(tokenStrByRequestor)) {
            logger.debug("No token found for owner {} and requestor {}", owner, requestor);
            return null;
        }

        AccessToken token = (AccessToken) memcached.get(tokenStrByRequestor);
        if (token == null) {
            logger.debug("No token found for owner {} and requestor {}", owner, requestor);
            // Keep things consistent by removing stray token string that points
            // to a non-existing token.
            if (isClientToken) {
                memcached.delete(owner);
            } else {
                String keyByOwner = getKeyByOwner(owner, isRackerRequestor(requestor));
                removeUserTokensByRequestor(userTokenStrings, keyByOwner, requestor);
            }
            return null;
        }

        return token;
    }

    @Override
    public void deleteAllTokensForOwner(String owner) {
        // NOTE: Will assume that the owner is not a Racker. Find another way to
        // revoke tokens for all Rackers.

        if (StringUtils.isBlank(owner)) {
            throw new IllegalArgumentException("Null or empty owner value given.");
        }

        // At this point, we don't know whether this is a client token or an
        // user token.

        boolean isClientOwner;
        Object byOwnerObj = memcached.get(owner);
        if (byOwnerObj == null) {
            // Try with user prefix; remember, we are not dealing with Racker
            // tokens for this method call.
            byOwnerObj = memcached.get(getKeyByOwner(owner, false));
            if (byOwnerObj == null) {
                // There's nothing for this owner
                logger.debug("There is no token associated with owner {}.", owner);
                return;
            } else {
                // Is a user.
                isClientOwner = false;
            }
        } else {
            // Don't know yet.
            if (byOwnerObj instanceof String) {
                isClientOwner = true;
            } else {
                // Is the UserTokenStrings type, i.e., the owner is a user.
                isClientOwner = false;
            }
        }

        boolean isSuccessfulForAllOps = true;
        if (isClientOwner) {
            Future<Boolean> result = memcached.delete((String) byOwnerObj);
            isSuccessfulForAllOps = evaluateCacheOperation(result,
                "delete the client access token by owner ID", owner);
        } else {
            UserTokenStrings userTokenStrings = (UserTokenStrings) byOwnerObj;
            for (String tokenStr : userTokenStrings.getTokenStrings()) {
                Future<Boolean> result = memcached.delete(tokenStr);
                boolean successful = evaluateCacheOperation(result, "delete a token from a list for owner",
                    tokenStr);
                isSuccessfulForAllOps = isSuccessfulForAllOps && successful;
            }

            Future<Boolean> result = memcached.delete(getKeyByOwner(owner, false));
            boolean success = evaluateCacheOperation(result, "delete all tokens associated with owner", owner);
            isSuccessfulForAllOps = isSuccessfulForAllOps && success;
        }

        if (!isSuccessfulForAllOps) {
            String errMsg = String.format("Failed to delete some or all tokens for owner %s", owner);
            logger.warn(errMsg);
        }
    }

    public boolean isAlive() {

        try {
            // this call and subsequent give us correct answer if any of the
            // server is down.
            memcached.getStats();
            if (memcached.getUnavailableServers().size() > 0) {
                return false;
            } else {
                return true;
            }
        } catch (RuntimeException runtimeExp) {
            return false;
        }
    }

    private UserTokenStrings getOrCreateLookupMap(String tokenOwnerId, boolean isTrusted) {
        String keyByOwner = getKeyByOwner(tokenOwnerId, isTrusted);
        UserTokenStrings userTokenStrings = (UserTokenStrings) memcached.get(keyByOwner);
        if (userTokenStrings == null) {
            userTokenStrings = new UserTokenStrings(tokenOwnerId);
        }

        return userTokenStrings;
    }

    private Future<Boolean> removeUserTokensByRequestor(UserTokenStrings userTokenStrings, String owner,
        String requestor) {
        userTokenStrings.remove(requestor);
        List<String> tokenStrings = userTokenStrings.getTokenStrings();
        if (tokenStrings.size() == 0) {
            return memcached.delete(owner);
        }
        return memcached.set(owner, userTokenStrings.getExpiration(new DateTime()), userTokenStrings);
    }

    private boolean evaluateCacheOperation(Future<Boolean> result, String operation, Object... operationParam) {
        try {
            return result.get();
        } catch (InterruptedException iex) {
            String threadErrMsg = String.format(
                "Could not confirm success of the operation \"%s\" with prameters %s", operation,
                operationParam);
            logger.warn(threadErrMsg);
            return false;
        } catch (ExecutionException eex) {
            String failedErrMsg = String.format("Failed to perform %s with parameter(s): %s", operation,
                operationParam);
            logger.warn(failedErrMsg);
            return false;
        }
    }

    private String getKeyByOwner(String owner, boolean isTrusted) {
        if (isTrusted) {
            return owner;
        }

        return owner + USER_TOKEN_KEY_POSTFIX;
    }

    private boolean isRackerRequestor(String requestor) {
        if (requestor.equals(config.getString("racker.client_id"))) {
            return true;
        }

        return false;
    }
}