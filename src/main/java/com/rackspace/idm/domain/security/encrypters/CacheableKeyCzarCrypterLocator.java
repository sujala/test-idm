package com.rackspace.idm.domain.security.encrypters;

import java.util.Map;

public interface CacheableKeyCzarCrypterLocator extends KeyCzarCrypterLocator {
    /**
     * Forces the cache to reset
     */
    void resetCache();

    /**
     * Retrieves cache info
     *
     * @return
     */
    Map<String, Object> getCacheInfo();
}
