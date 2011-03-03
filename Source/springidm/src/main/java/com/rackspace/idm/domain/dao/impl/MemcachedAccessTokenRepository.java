package com.rackspace.idm.domain.dao.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.util.PingableService;

public class MemcachedAccessTokenRepository implements AccessTokenDao, PingableService {
    private MemcachedClient memcached;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final DateTimeFormatter DATE_PARSER = DateTimeFormat.forPattern("yyyyMMddHHmmss.SSS'Z");

    public MemcachedAccessTokenRepository(MemcachedClient memcached) {

        this.memcached = memcached;
    }

    @Override
    public void save(AccessToken accessToken) {
        // Validate params
        if (accessToken == null) {
            logger.error("Token is null");
            throw new IllegalArgumentException("Token is null");
        }
        String tokenString = accessToken.getTokenString();
        if (StringUtils.isBlank(tokenString) || accessToken.getExpirationTime() == null) {
            String errMsg = "Token string and/or expiration time values are not present in the given token.";
            logger.error(errMsg);
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
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        // do not overwrite client token if password reset flow
        if (!accessToken.isRestrictedToSetPassword()) {
            // Try adding the tokenString with the owner_requestor as the key
            UserTokenStrings userTokenStrings = getOrCreateLookupMap(accessToken.getOwner());
            userTokenStrings.put(accessToken.getRequestor(), accessToken.getExpiration(), tokenString);
            Future<Boolean> resultByOwner = memcached.set(userTokenStrings.getOwnerId(),
                userTokenStrings.getExpiration(), userTokenStrings);
            boolean addedByOwner = evaluateCacheOperation(resultByOwner, "set token by owner", accessToken);
            if (!addedByOwner) {
                // Attempt a rollback of the previous operation before bailing
                // with an exception
                memcached.delete(tokenString);
                String errMsg = String.format("Failed to add token by owner with parameter %s", accessToken);
                logger.error(errMsg);
                throw new IllegalStateException(errMsg);
            }
        }
        logger.debug("Added token: {}", accessToken);
    }

    @Override
    public void delete(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            logger.error("Token string is null or empty");
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

        if (!token.isRestrictedToSetPassword()) {
            // delete pointer to token
            UserTokenStrings userTokenStrings = getOrCreateLookupMap(token.getOwner());
            Future<Boolean> deleteResult = removeByRequestor(userTokenStrings, token.getOwner(),
                token.getRequestor());
            boolean deletedByOwner = evaluateCacheOperation(deleteResult, "delete token by owner",
                tokenString);

            if (!deletedByTokenStr || !deletedByOwner) {
                String errMsg = String.format("Failed to delete token %s", tokenString);
                logger.error(errMsg);
                throw new IllegalStateException(errMsg);
            }
        }
    }

    /**
     * Will NOT find expired tokens.
     */
    @Override
    public AccessToken findByTokenString(String tokenString) {

        if (StringUtils.isBlank(tokenString)) {
            logger.error("Token string is null or empty");
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
        UserTokenStrings userTokenStrings = getOrCreateLookupMap(token.getOwner());
        String tokenStrByRequestor = userTokenStrings.get(token.getRequestor());
        if (token.getTokenString().equals(tokenStrByRequestor)) {
            logger.debug("Found token with value {}", token);
            return token;
        }

        // The corresponding entry stored with key of owner_requestor was not
        // found or did not match. Therefore, the data is in an inconsistent
        // state.
        logger.error("Token cache in an inconsistent state. Deleting {}", token);
        removeByRequestor(userTokenStrings, token.getOwner(), token.getRequestor());
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
            logger.error("Owner value is null or empty");
            throw new IllegalArgumentException("Owner value is null or empty");
        }
        logger.debug("Finding token with owner {} and requestor {}", owner, requestor);
        UserTokenStrings userTokenStrings = getOrCreateLookupMap(owner);
        String tokenStrByRequestor = userTokenStrings.get(requestor);
        if (StringUtils.isBlank(tokenStrByRequestor)) {
            logger.debug("No token found for owner {} and requestor {}", owner, requestor);
            return null;
        }

        AccessToken token = (AccessToken) memcached.get(tokenStrByRequestor);
        if (token == null) {
            logger.debug("No token found for owner {} and requestor {}", owner, requestor);
            // Keep things consistent by removing stray token string that points
            // to a non-existing token.
            removeByRequestor(userTokenStrings, owner, requestor);
            return null;
        }

        return token;
    }

    @Override
    public void deleteAllTokensForOwner(String owner) {
        UserTokenStrings userTokenStrings = getOrCreateLookupMap(owner);
        boolean isAllSuccessful = true;
        for (String tokenStr : userTokenStrings.getTokenStrings()) {
            Future<Boolean> result = memcached.delete(tokenStr);
            boolean successful = evaluateCacheOperation(result, "delete a token from a list for owner",
                tokenStr);
            isAllSuccessful = isAllSuccessful && successful;
        }

        Future<Boolean> result = memcached.delete(owner);
        boolean success = evaluateCacheOperation(result, "delete all tokens associated with owner", owner);
        isAllSuccessful = isAllSuccessful && success;
        if (!isAllSuccessful) {
            String errMsg = String.format("Failed to delete some or all tokens for owner %s", owner);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
    }

    @Override
    public void deleteAllTokensForOwner(String owner, Set<String> tokenRequestors) {
        throw new UnsupportedOperationException("This method is no longer supported!");
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

    private UserTokenStrings getOrCreateLookupMap(String tokenOwnerId) {
        @SuppressWarnings("unchecked")
        UserTokenStrings userTokenStrings = (UserTokenStrings) memcached.get(tokenOwnerId);
        if (userTokenStrings == null) {
            userTokenStrings = new UserTokenStrings(tokenOwnerId);
        }

        return userTokenStrings;
    }

    private Future<Boolean> removeByRequestor(UserTokenStrings userTokenStrings, String owner,
        String requestor) {
        userTokenStrings.remove(requestor);
        List<String> tokenStrings = userTokenStrings.getTokenStrings();
        if (tokenStrings.size() == 0) {
            return memcached.delete(owner);
        }
        return memcached.set(owner, userTokenStrings.getExpiration(), userTokenStrings);
    }

    private boolean evaluateCacheOperation(Future<Boolean> result, String operation, Object... operationParam) {
        try {
            return result.get();
        } catch (InterruptedException iex) {
            String threadErrMsg = String.format(
                "Could not confirm success of the operation \"%s\" with prameters %s", operation,
                operationParam);
            logger.error(threadErrMsg);
            return false;
        } catch (ExecutionException eex) {
            String failedErrMsg = String.format("Failed to perform %s with parameter(s): %s", operation,
                operationParam);
            logger.error(failedErrMsg);
            return false;
        }
    }
}
