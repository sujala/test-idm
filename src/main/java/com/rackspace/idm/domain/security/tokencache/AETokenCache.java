package com.rackspace.idm.domain.security.tokencache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableList;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.RecreatableCaffeineCache;
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider;
import com.rackspace.idm.exception.IdmException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class AETokenCache implements RecreatableCaffeineCache, TokenCache {
    private static final Logger LOG = LoggerFactory.getLogger(AETokenCache.class);

    public static final String CACHE_INFO_CONFIGURATION_MAP_KEY = "configuration";
    public static final String CACHE_INFO_STATISTICS_MAP_KEY = "statistics";
    public static final String CONFIGURATION_CACHE_ENABLED = "cacheEnabled";
    public static final String CONFIGURATION_CACHE_MAX_SIZE = "cacheMaxSize";
    public static final String CONFIGURATION_CACHE_INITIAL_CAPACITY = "cacheInitialCapacity";
    public static final String CONFIGURATION_CACHE_RECORDING_STATS = "cacheRecordingStats";
    public static final String STATISTICS_ESTIMATED_SIZE = "estimatedSize";
    public static final String STATISTICS_HIT_RATE = "hitRate";
    public static final String STATISTICS_MISS_RATE = "missRate";
    public static final String STATISTICS_REQUEST_COUNT = "requestCount";
    public static final String STATISTICS_HIT_COUNT = "hitCount";
    public static final String STATISTICS_MISS_COUNT = "missCount";
    public static final String STATISTICS_LOAD_COUNT = "loadCount";
    public static final String STATISTICS_EVICTION_COUNT = "evictionCount";
    public static final String STATISTICS_LOAD_SUCCESS_COUNT = "loadSuccessCount";
    public static final String STATISTICS_LOAD_FAILURE_COUNT = "loadFailureCount";
    public static final String STATISTICS_LOAD_FAILURE_RATE = "loadFailureRate";
    public static final String STATISTICS_AVERAGE_LOAD_PENALTY = "averageLoadPenalty";
    public static final String STATISTICS_TOTAL_LOAD_TIME = "totalLoadTime";

    public static final ImmutableList<String> cacheableAuthBys = ImmutableList.of(
            AuthenticatedByMethodEnum.PASSWORD.getValue()
            , AuthenticatedByMethodEnum.APIKEY.getValue()
            , AuthenticatedByMethodEnum.PASSCODE.getValue()
            , AuthenticatedByMethodEnum.OTPPASSCODE.getValue()
    );

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private ConfigExpiry configExpiry;

    @Autowired(required = false)
    private Ticker ticker;

    @Autowired
    private AETokenService aeTokenService;

    private volatile CacheBlock cacheBlock;

    @PostConstruct
    public void init() {
        recreateCache();
    }

    @Override
    public void recreateCache() {
        CacheBlock oldCacheBlock = cacheBlock;
        try {
            CacheBlock newCacheBlock = new CacheBlock(identityConfig);
            cacheBlock = newCacheBlock;
        } catch (Exception e) {
            LOG.error("Error initializing AE token cache", e);
            throw new IdmException("Error creating or recreating AE cache.", e);
        }

        /* Cleanup old cache. Shouldn't be necessary as the old reference would drop and make available for gc, but perf
         * hit should be small and doesn't hurt to be explicit. Also protects in case the guava cache does some things
         * that requires explicit invalidation.
         */
        if (oldCacheBlock != null) {
            oldCacheBlock.cache.invalidateAll();
        }
    }

    @Override
    public void invalidateCache() {
        if (getCache() != null) {
            getCache().invalidateAll();
        }
    }

    @Override
    public void performMaintenance() {
        if (getCache() != null) {
            getCache().cleanUp();
        }
    }

    @Override
    public Cache getCache() {
        Cache cache = null;
        if (cacheBlock != null) {
            cache = cacheBlock.getCache();
        }
        return cache;
    }

    @Override
    public Map<String, Object> getCacheInfo() {
        CacheBlock cacheBlockInner = cacheBlock;

        HashMap<String, Object> map = new LinkedHashMap<>();
        HashMap<String, Object> configuredMap = new LinkedHashMap<>();
        map.put(CACHE_INFO_CONFIGURATION_MAP_KEY, configuredMap);
        HashMap<String, Number> statisticsMap = new LinkedHashMap<>();
        map.put(CACHE_INFO_STATISTICS_MAP_KEY, statisticsMap);

        if (cacheBlockInner != null) {
            configuredMap.put(CONFIGURATION_CACHE_ENABLED, identityConfig.getRepositoryConfig().getTokenCacheConfiguration().isEnabled());
        } else {
            configuredMap.put(CONFIGURATION_CACHE_ENABLED, false);
        }

        try {

            if (cacheBlockInner != null) {
                configuredMap.put(CONFIGURATION_CACHE_MAX_SIZE, cacheBlock.configuredMaxSize);
                configuredMap.put(CONFIGURATION_CACHE_INITIAL_CAPACITY, cacheBlock.configuredInitialCapacity);
                configuredMap.put(CONFIGURATION_CACHE_RECORDING_STATS, cacheBlock.configuredRecordStats);

                Cache cache = cacheBlockInner.getCache();
                if (cache != null) {
                    statisticsMap.put(STATISTICS_ESTIMATED_SIZE, cache.estimatedSize());
                    statisticsMap.put(STATISTICS_HIT_RATE, cache.stats().hitRate());
                    statisticsMap.put(STATISTICS_MISS_RATE, cache.stats().missRate());
                    statisticsMap.put(STATISTICS_REQUEST_COUNT, cache.stats().requestCount());
                    statisticsMap.put(STATISTICS_HIT_COUNT, cache.stats().hitCount());
                    statisticsMap.put(STATISTICS_MISS_COUNT, cache.stats().missCount());
                    statisticsMap.put(STATISTICS_LOAD_COUNT, cache.stats().loadCount());
                    statisticsMap.put(STATISTICS_EVICTION_COUNT, cache.stats().evictionCount());
                    statisticsMap.put(STATISTICS_LOAD_SUCCESS_COUNT, cache.stats().loadSuccessCount());
                    statisticsMap.put(STATISTICS_LOAD_FAILURE_COUNT, cache.stats().loadFailureCount());
                    statisticsMap.put(STATISTICS_LOAD_FAILURE_RATE, cache.stats().loadFailureRate());
                    statisticsMap.put(STATISTICS_AVERAGE_LOAD_PENALTY, cache.stats().averageLoadPenalty());
                    statisticsMap.put(STATISTICS_TOTAL_LOAD_TIME, cache.stats().totalLoadTime());
                }
            }
        } catch (Exception e) {
            LOG.error("Encountered error collecting AE cache info", e);
            configuredMap.put("CollectionError", "Error collecting data.");
        }

        return map;
    }

    @Override
    public String getOrCreateTokenForUser(final BaseUser user, final ScopeAccess token, final TokenProvider provider) {
        String webSafeToken = null;

        // Only cache provisioned user regular non-scoped tokens for now
        if (isTokenCacheableForUser(user, token)) {
            try {
                AETokenCacheRequestProcessor entryProcessor = new AETokenCacheRequestProcessor(provider, user, token);
                webSafeToken = entryProcessor.getOrCreateWebTokenFromCache();
            } catch (Exception e) {
                // If encounter any exception with cache, fallback to original
                LOG.error("Encountered error using token cache. Falling back to non-cached token.", e);
                webSafeToken = provider.marshallTokenForUser(user, token);
            }
        } else {
            webSafeToken = provider.marshallTokenForUser(user, token);
        }
        return webSafeToken;
    }

    /**
     * We can only cache tokens of a particular types
     *
     * @param user
     * @param token
     * @return
     */
    @Override
    public boolean isTokenCacheableForUser(BaseUser user, ScopeAccess token) {
        try {
            if (cacheBlock == null) {
                LOG.debug("Cache is not configured. No tokens are cacheable.");
                return false;
            }

            if (user == null || token == null) {
                LOG.debug("Not caching token due to user or token being null.");
                return false;
            }

            // The user must have an id
            if (StringUtils.isBlank(user.getId())) {
                LOG.debug("Not caching token due to user not having an id.");
                return false;
            }

            // The token must have auth by values and all must be supported to be cached
            if (CollectionUtils.isEmpty(token.getAuthenticatedBy()) || !CollectionUtils.containsAll(cacheableAuthBys, token.getAuthenticatedBy())) {
                LOG.debug("Not caching token due to auth methods used not all supported for caching");
                return false;
            }

            // Must not be a scoped token
            if (StringUtils.isNotBlank(token.getScope())) {
                LOG.debug("Not caching token due to token contains a scoped value.");
                return false;
            }

            // Must be issued to a user
            if (!((token instanceof UserScopeAccess) || (token instanceof RackerScopeAccess))) {
                LOG.debug(String.format("Not caching token due to token being an unsupported type. Not a BaseUserToken but a '%s'.", token.getClass().getSimpleName()));
                return false;
            }

            BaseUserToken buToken = (BaseUserToken) token;

            // The token must be issued to the specified user
            if (!user.getId().equalsIgnoreCase(buToken.getIssuedToUserId()) && !buToken.isDelegationToken()) {
                LOG.debug("Not caching token because the supplied user does not correspond to the user to whom the token is being issued");
                return false;
            }

            /*
            The user token is of a format that is cacheable. However, not all cacheable tokens are actually cached as it's a per
            user configuration. Determine if this user's tokens for the specified type are cacheable.
             */
            CacheKey key = new CacheKey(user, token.getAuthenticatedBy());
            if (!configExpiry.areTokensCacheable(key)) {
                LOG.debug("Not caching token because the authentication methods in token for the user are not cached.");
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.warn("Exception occurred determining whether token can be cached.", e);
            return false;
        }
    }

    private class AETokenCacheRequestProcessor {
        final TokenProvider tokenProvider;
        final BaseUser user;
        final ScopeAccess token;
        boolean newTokenGenerated = false;

        private Function generateTokenFunction = new Function<CacheKey, TokenCacheEntry>() {
            @Override
            public TokenCacheEntry apply(CacheKey key) {
                LOG.trace("Creating new token for user {}", user.getUsername());

                // Update the processor to indicate a new token was generated
                newTokenGenerated = true;
                tokenProvider.marshallTokenForUser(user, token);
                return new TokenCacheEntry(token);
            }
        };

        public AETokenCacheRequestProcessor(TokenProvider tokenProvider, BaseUser user, ScopeAccess token) {
            Validate.notNull(user);
            Validate.notNull(token);
            Validate.notNull(tokenProvider);

            this.tokenProvider = tokenProvider;
            this.user = user;
            this.token = token;

            Validate.isTrue(StringUtils.isNotBlank(user.getId()), "User must have a user id");
            Validate.isTrue(CollectionUtils.isNotEmpty(token.getAuthenticatedBy()), "Token must have at least one authenticated by.");
        }

        public CacheKey getKey() {
            return new CacheKey(user, token.getAuthenticatedBy());
        }

        public String getOrCreateWebTokenFromCache() {
            return getOrCreateWebTokenFromCacheWithInvalidationCheck(0);
        }

        private String getOrCreateWebTokenFromCacheWithInvalidationCheck(int recursionDepth) {
            Validate.isTrue(recursionDepth >= 0);

        /*
         Failsafe recursion check. This should never happen, but just to be on absolute safe side since consequences
         would be an infinite loop...
         */
            if (recursionDepth > 1) {
                throw new IllegalStateException(String.format("Hit recursion level '%s' trying to generate or " +
                        "retrieve AE token and placing in cache.", recursionDepth));
            }
            CacheKey key = getKey();
            TokenCacheEntry tokenCacheEntry = cacheBlock.cache.get(key, generateTokenFunction);

            if (!newTokenGenerated) {
                /*
                 The cached token must be checked to ensure it is still valid. While cache entries will expire prior
                  to the token expiring naturally, tokens can be revoked at any time.

                  Also double check to ensure it is for the user. While it should
                 always be due to the key including the userId, want to take extra precaution when reissuing tokens.
                  */
                ScopeAccess cachedTokenSa = aeTokenService.unmarshallTokenAndValidate(tokenCacheEntry.getAccessTokenStr());

                boolean validEntry = true;
                if (!(cachedTokenSa instanceof BaseUserToken)) {
                    LOG.debug("Token returned from cache was expired or revoked!");
                    validEntry = false;
                } else {
                    BaseUserToken buToken = (BaseUserToken) cachedTokenSa;
                    if (!user.getId().equalsIgnoreCase(buToken.getIssuedToUserId())) {
                        LOG.error("Token returned cache was for different user!");
                        validEntry = false;
                    }
                }

                if (!validEntry) {
                    LOG.debug("Token in cache was revoked, expired, or otherwise invalid. Retrieving new token and populating in cache");
                    cacheBlock.cache.invalidate(getKey());

                    // Generate a new token
                    return getOrCreateWebTokenFromCacheWithInvalidationCheck(++recursionDepth);
                } else {
                    processCacheHit(tokenCacheEntry, user, token);
                }
            }

            return token.getAccessTokenString();
        }

        /**
         * This simulates the changes that the TokenProvider would make to the passed in ScopeAccess object when generating
         * a new token.
         *
         * @param tokenCacheEntry
         * @param user
         * @param token
         * @return
         */
        private void processCacheHit(TokenCacheEntry tokenCacheEntry, BaseUser user, ScopeAccess token) {
            //cache hit. Token is still good. Need to update the passed in token with the new info similar to
            //the changes the TokenProvider would do when generating a new token
            token.setAccessTokenString(tokenCacheEntry.getAccessTokenStr());
            token.setAccessTokenExp(Date.from(tokenCacheEntry.getExpirationDate()));
            token.setCreateTimestamp(Date.from(tokenCacheEntry.getCreationDate()));
            token.setUniqueId(tokenCacheEntry.getUniqueId());
        }
    }

    @Getter
    private class CacheBlock {
        Integer configuredMaxSize;
        Integer configuredInitialCapacity;
        Boolean configuredRecordStats;

        Cache<CacheKey, TokenCacheEntry> cache;

        public CacheBlock(IdentityConfig config) {
            LOG.debug("Creating AE Token Cache");
            configuredMaxSize = config.getReloadableConfig().cachedAETokenCacheMaxSize();
            configuredInitialCapacity = config.getReloadableConfig().cachedAETokenCacheInitialCapacity();
            configuredRecordStats = config.getReloadableConfig().cachedAETokenCacheRecordStats();

            Ticker theTicker = ticker != null ? ticker : Ticker.systemTicker();
            Caffeine builder = Caffeine.newBuilder()
                    .maximumSize(configuredMaxSize)
                    .initialCapacity(configuredInitialCapacity)
                    .ticker(theTicker)
                    .expireAfter(configExpiry);

            if (configuredRecordStats) {
                builder.recordStats();
            }
            cache = builder.build();
        }
    }
}