package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.KeyMetadata;

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
    KeyMetadata getCacheInfo();
}
