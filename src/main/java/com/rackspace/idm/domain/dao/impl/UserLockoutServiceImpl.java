package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.CacheConfiguration;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.LockoutCheckResult;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.dao.UserLockoutService;
import com.rackspace.idm.domain.entity.User;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UserLockoutServiceImpl implements UserLockoutService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private UserDao userDao;

    @PostConstruct
    public void init() {
        Cache userLockoutCache = getUserLockoutCache();

        boolean useCache = identityConfig.getReloadableConfig().isLdapAuthPasswordLockoutCacheEnabled();
        if (useCache && userLockoutCache == null) {
            logger.error("** User lockout cache failed to load. Lockout cache will not be used.");
        } else if (useCache) {
            logger.info("User lockout cache initialized.");
        } else if (userLockoutCache != null) {
            logger.info("User lockout cache initialized, but not enabled.");
        }
    }

    @Override
    public LockoutCheckResult performLockoutCheck(String username) {
        LockoutCheckResult results = null;

        boolean useCache = useLockoutCache();

        if (useCache) {
            UserLockoutCacheEntry entry = checkAndGetValidUserLockoutCacheEntry(username);
            if (entry != null) {
                results = new LockoutCheckResult(false, entry.getLockoutExpiration(), null);
            }
        }

        // Is cache is disabled or no cache entry found attempt to load user from backend
        if (results == null) {
            User user = userDao.getUserByUsername(username);
            DateTime lockoutExpiration = null;
            if (user != null) {
                if (isUserLocked(user)) {
                    // Locked users must always be indicated for audit logging purposes
                    lockoutExpiration = calculateExpirationFromDate(new DateTime(user.getPasswordFailureDate()));
                    if (useCache) {
                        addLockoutCacheEntryForUser(user, new DateTime(user.getPasswordFailureDate()));
                    }
                }
            }
            results = new LockoutCheckResult(true, lockoutExpiration, user);
        }
        return results;
    }

    /**
     * Returns the time, in the future, that an existing lockout will expire. If the user is not currently locked out,
     * returns null.
     *
     * @param user
     * @return
     */
    private boolean isUserLocked(User user) {
        boolean locked = false;

        int failureAttemptLockoutThreshold = identityConfig.getReloadableConfig().getLdapAuthPasswordLockoutRetries();
        if (user != null && user.getPasswordFailureAttempts() >= failureAttemptLockoutThreshold
                && user.getPasswordFailureDate() != null) {
            DateTime lockoutExpirationDate = calculateExpirationFromDate(new DateTime(user.getPasswordFailureDate()));
            locked = lockoutExpirationDate.isAfterNow();
        }
        return locked;
    }

    /**
     * If a cache entry exists where user is still considered locked out, return it. Otherwise, return null. Removes
     * found entries if the lockout period has expired.
     *
     * @param username
     * @return
     */
    private UserLockoutCacheEntry checkAndGetValidUserLockoutCacheEntry(String username) {
        UserLockoutCacheEntry userLockoutCacheEntry = null;
        try {
            Cache userLockoutCache = getUserLockoutCache();
            Cache.ValueWrapper entryWrapper = userLockoutCache.get(username);
            if (entryWrapper != null) {
                UserLockoutCacheEntry entry = (UserLockoutCacheEntry) entryWrapper.get();
                if (entry.inLockoutPeriod()) {
                    userLockoutCacheEntry = entry;
                } else {
                    // Remove cache entry if not in lockout period anymore.
                    logger.debug(String.format("Found expired lockout for user '%s' in user lockout cache. Clearing.", username));
                    userLockoutCache.evict(username);
                }
            }
        } catch (Exception ex) {
            // Eat. Cache is performance improvement, but not a requirement.
            logger.debug("Error trying to lookup user in lockout cache", ex);
        }
        return userLockoutCacheEntry;
    }

    private void addLockoutCacheEntryForUser(User user, DateTime lockoutExpiration) {
        try {
            UserLockoutCacheEntry entry = new UserLockoutCacheEntry(user, lockoutExpiration);
            getUserLockoutCache().putIfAbsent(user.getUsername(), entry);
        } catch (Exception ex) {
            // Eat. Cache is performance improvement, but not a requirement.
            logger.debug("Error trying to add user lockout entry to lockout cache", ex);
        }
    }

    private Cache getUserLockoutCache() {
        return cacheManager.getCache(CacheConfiguration.USER_LOCKOUT_CACHE_BY_NAME);
    }

    private boolean useLockoutCache() {
        return identityConfig.getReloadableConfig().isLdapAuthPasswordLockoutCacheEnabled();
    }

    private DateTime calculateExpirationFromDate(DateTime start) {
        DateTime expiration = null;
        if (start != null) {
            expiration = start.plus(identityConfig.getReloadableConfig().getLdapAuthPasswordLockoutDuration().toMillis());
        }
        return expiration;
    }

    @Getter
    private class UserLockoutCacheEntry {
        private DateTime created = new DateTime();
        private DateTime lastPasswordFailureDate;
        private String userName;

        /**
         * Creates a cache entry based on the supplied user.
         * @param user
         */
        public UserLockoutCacheEntry(User user, DateTime lastPasswordFailureDate) {
            if (user == null || StringUtils.isBlank(user.getUsername())) {
                throw new IllegalArgumentException("User must be non null with a username");
            }
            this.userName = user.getUsername();
            this.lastPasswordFailureDate = lastPasswordFailureDate;
        }

        public boolean inLockoutPeriod() {
            return getLockoutExpiration() != null;
        }

        public DateTime getLockoutExpiration() {
            DateTime expiration = calculateExpirationFromDate(lastPasswordFailureDate);
            return expiration != null && expiration.isAfterNow() ? expiration : null;
        }
    }

}
