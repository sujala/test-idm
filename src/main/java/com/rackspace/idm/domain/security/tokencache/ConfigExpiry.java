package com.rackspace.idm.domain.security.tokencache;

import com.github.benmanes.caffeine.cache.Expiry;
import com.rackspace.idm.domain.config.IdentityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Calulates the TTL for a cache entry based on the key data.
 */
@Component()
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConfigExpiry implements Expiry<CacheKey, TokenCacheEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigExpiry.class);
    public static final BigDecimal MAX_CACHE_ENTRY_TTL_PERCENT_OF_TOKEN_LIFETIME = BigDecimal.valueOf(.9);

    @Autowired
    private IdentityConfig identityConfig;

    private Clock clock;

    public ConfigExpiry() {
        this.clock = Clock.systemUTC();
    }

    public ConfigExpiry(Clock clock) {
        this.clock = clock;
    }

    /**
     * Expiration is based on the configuration. Assumes the token creation time is relatively close of the
     * time this method is called as the cacheTTL is calculated based on time when cache entry is inserted.
     * @param key
     * @param tokenCacheEntry
     * @param currentTime
     * @return
     */
    public long expireAfterCreate(CacheKey key, TokenCacheEntry tokenCacheEntry, long currentTime) {
        CacheableUserJson cacheableUserJson = getUserCacheConfigFromCacheKey(key);
        return calculateCacheEntryExpiration(cacheableUserJson, tokenCacheEntry);
    }

    /**
     * No change to expiration
     *
     * @param key
     * @param value
     * @param currentTime
     * @param currentDuration
     * @return
     */
    public long expireAfterUpdate(CacheKey key, TokenCacheEntry value,
                                  long currentTime, long currentDuration) {
        return currentDuration;
    }

    /**
     * No change to expiration
     *
     * @param key
     * @param value
     * @param currentTime
     * @param currentDuration
     * @return
     */
    public long expireAfterRead(CacheKey key, TokenCacheEntry value,
                                long currentTime, long currentDuration) {
        return currentDuration;
    }

    /**
     * Determines whether a user's auth could potentially result in an entry in the cache. Not definitive that a given
     * user's token will be cached as that's based on state of existing cache entries, user's cache config, etc, but
     * this provides ability to eliminate obvious cases where regardless of the requested expiration, it's guaranteed
     * that a new token will need to be generated for the user.
     *
     * @param key
     * @return
     */
    public boolean areTokensCacheable(CacheKey key) {
        CacheableUserJson userConfig = getUserCacheConfigForUser(key.userManagementSystem(), key.userId, key.authByMethods);
        boolean hasZeroCacheDuration = userConfig != null && userConfig.getMaximumCacheDuration() != null && (userConfig.getMaximumCacheDuration().isZero() || userConfig.getMaximumCacheDuration().isNegative());

        // If user doesn't have a config, or the configuration for the cache duration sets a zero (or negative) max ttl, then can guarantee no token will be cached for this cache key.
        return !(userConfig == null || hasZeroCacheDuration);
    }

    private CacheableUserJson getUserCacheConfigFromCacheKey(CacheKey key) {
        return getUserCacheConfigForUser(key.userManagementSystem(), key.userId, key.authByMethods);
    }

    private CacheableUserJson getUserCacheConfigForUser(UserManagementSystem managementSystem, String userId, List<String> authenticatedByMethods) {
        CacheableUserJson userConfig = null;

        try {
            TokenCacheConfigJson tokenCacheConfig = identityConfig.getRepositoryConfig().getTokenCacheConfiguration();
            userConfig = tokenCacheConfig.findConfigForUserWithAuthMethods(managementSystem, userId, authenticatedByMethods);
        } catch (Exception e) {
            LOG.error("Error searching token cache config for user cache duration. Falling back to no caching.", e);
        }

        return userConfig;
    }

    /**
     * The <b>maximumCacheDuration</b> and <b>minimalValidityDuration</b> of the user's cache configuration and the token's
     *  natural expiration date are used to determine the time to live (TTL) of the
     *  token in the cache. The formula for calculating the TTL is calculating the
     *  following dates:
     *
     *  Date 1: (token expiration - minimum validity duration)
     *  Date 2: (current time + maximum cache duration) or null if no cacheDuration set
     *
     *   The token's TTL in the cache is set to the earliest of the two dates. However,
     *  if Date 1 is earlier than the current time, the token is not cached at all.
     *  This is shown in the following examples where the current time is
     *  ``2019-01-01T00:00:00.000-05:00`` and the token's expiration is
     *  ``2019-01-02T00:00:00.000-05:00`` (a 24 hour token).
     *
     * @param cacheableUserJson
     * @param tokenCacheEntry
     * @return TTL in nanos
     */
    private long calculateCacheEntryExpiration(CacheableUserJson cacheableUserJson, TokenCacheEntry tokenCacheEntry) {
        long cacheLifetimeNanos = 0;
        Instant now = clock.instant();

        if (cacheableUserJson != null && cacheableUserJson.getMinimumValidityDuration() != null) {
            Instant tokenExpiration = tokenCacheEntry.getExpirationDate();

            // The cache expiration
            Instant minValidityExpiration = tokenExpiration.minus(cacheableUserJson.getMinimumValidityDuration());
            Instant durationExpiration = null;
            if (cacheableUserJson.getMaximumCacheDuration() != null) {
                durationExpiration = now.plus(cacheableUserJson.getMaximumCacheDuration());
            }

            if (minValidityExpiration.isBefore(now) || (durationExpiration != null && durationExpiration.isBefore(now))) {
                // The token can't be cached. So let lifetime to 0.
                cacheLifetimeNanos = 0;
            } else {
                // Need to determine expiration based on formula
                Instant expiration;
                if (durationExpiration == null || minValidityExpiration.isBefore(durationExpiration)) {
                    expiration = minValidityExpiration;
                } else {
                    expiration = durationExpiration;
                }

                // Convert expiration to nano TTL
                Duration cacheLifeTimeDuration = Duration.between(now, expiration);
                cacheLifetimeNanos = cacheLifeTimeDuration.toNanos();
            }
        }
        return cacheLifetimeNanos;
    }

}