package com.rackspace.idm.domain.security;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.Map;

public interface RecreatableCaffeineCache<K, V> {
    /**
     * Recreates the cache from scratch. Any previous entries are discarded.
     */
    void recreateCache();

    /**
     * Remove all entries from the cache
     */
    void invalidateCache();

    /**
     * Perform any outstanding maintenance activities related to cache
     */
    void performMaintenance();

    /**
     * Retrieves the cache
     * @return
     */
    Cache<K,V> getCache();

    Map<String, Object> getCacheInfo();

}
