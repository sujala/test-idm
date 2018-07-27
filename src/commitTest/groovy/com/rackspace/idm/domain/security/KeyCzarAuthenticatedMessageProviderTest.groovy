package com.rackspace.idm.domain.security

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.security.encrypters.KeyCzarAuthenticatedMessageProvider
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator
import org.apache.commons.configuration.Configuration
import org.keyczar.Crypter
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

/**
 * Created by robe4218 on 2/3/15.
 */
class KeyCzarAuthenticatedMessageProviderTest extends Specification {
    private KeyCzarAuthenticatedMessageProvider authenticatedMessageProvider;

    private KeyCzarCrypterLocator keyCzarCrypterLocator;
    private IdentityConfig.ReloadableConfig reloadableConfig;

    def setup() {
        keyCzarCrypterLocator = Mock()

        authenticatedMessageProvider = new KeyCzarAuthenticatedMessageProvider()
        authenticatedMessageProvider.keyCzarCrypterLocator = keyCzarCrypterLocator
    }

    def "crypter initialized on init"() {
        given:
        Crypter crypter = Mock(Crypter)

        when:
        authenticatedMessageProvider.init()

        then:
        1 * keyCzarCrypterLocator.getCrypter() >> crypter
    }

    def "encryption and decryption always lookup crypter on encryption and decryption"() {
        given:
        Crypter crypter = Mock(Crypter)

        when:
        authenticatedMessageProvider.decrypt()

        then:
        1 * keyCzarCrypterLocator.getCrypter() >> crypter
        1 * crypter.decrypt(_)

        when:
        authenticatedMessageProvider.encrypt()

        then:
        1 * keyCzarCrypterLocator.getCrypter() >> crypter
        1 * crypter.encrypt(_)
    }

}
