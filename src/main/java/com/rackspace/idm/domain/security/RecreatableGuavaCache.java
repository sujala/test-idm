package com.rackspace.idm.domain.security;

import com.google.common.cache.Cache;

public interface RecreatableGuavaCache<K, V> {
    /**
     * Recreates the cache from scratch. Any previous entries are discarded.
     */
    void recreateCache();

    /**
     * Retrieves the cache
     * @return
     */
    Cache<K,V> getCache();
}
