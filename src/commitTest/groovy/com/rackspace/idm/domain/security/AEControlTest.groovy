package com.rackspace.idm.domain.security

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator
import spock.lang.Specification

class AEControlTest extends Specification {
    AEControl aeControl = new AEControl()
    CacheableKeyCzarCrypterLocator locator = Mock(CacheableKeyCzarCrypterLocator)
    IdentityConfig identityConfig = Mock(IdentityConfig)
    IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)

    def setup() {
        identityConfig.getReloadableConfig() >> reloadableConfig
        identityConfig.getStaticConfig() >> staticConfig

        aeControl = new AEControl()
        aeControl.cacheableKeyCzarCrypterLocator = locator
        aeControl.identityConfig = identityConfig
    }

    def "No-Op when no cachable locator"() {
        aeControl.cacheableKeyCzarCrypterLocator = null

        when:
        aeControl.reloadKeys()

        then:
        notThrown(NullPointerException)
    }

    def "No-Op when ae key reload disabled"() {
        when:
        aeControl.reloadKeys()

        then:
        1 * reloadableConfig.getAutoReloadOfAEKeys() >> false
        0 * locator.resetCache()
    }

    def "Keys reloaded if AE Reload enabled and cachable locator even if AE tokens disabled"() {
        staticConfig.getFeatureAETokensDecrypt() >> false

        when:
        aeControl.reloadKeys()

        then:
        1 * reloadableConfig.getAutoReloadOfAEKeys() >> true
        1 * locator.resetCache()
    }

}
