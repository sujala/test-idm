package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "aeControl")
public class AEControl implements AEControlMethods {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired(required = false)
    private CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator;

    @Autowired
    private IdentityConfig identityConfig;

    /**
     * This is pretty much a hack to have a method callable by a scheduler that can be configured to be off via a
     * reloadable property. While could look into programmatically creating a task and attaching, using the spring
     * built in scheduler via xml configuration was trivial, and did the job.
     */
    public void reloadKeys() {
        if (cacheableKeyCzarCrypterLocator != null
                && identityConfig.getReloadableConfig().getAutoReloadOfAEKeys()) {
            cacheableKeyCzarCrypterLocator.resetCache();
        }
    }
}
