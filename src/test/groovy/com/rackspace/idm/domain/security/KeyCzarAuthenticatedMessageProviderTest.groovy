package com.rackspace.idm.domain.security

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.security.encrypters.KeyCzarAuthenticatedMessageProvider
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator
import org.keyczar.Crypter
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

/**
 * Created by robe4218 on 2/3/15.
 */
class KeyCzarAuthenticatedMessageProviderTest extends Specification {
    private KeyCzarAuthenticatedMessageProvider authenticatedMessageProvider;

    private KeyCzarCrypterLocator keyCzarCrypterLocator;
    private IdentityConfig identityConfig;

    def setup() {
        keyCzarCrypterLocator = Mock()
        identityConfig = Mock()
        authenticatedMessageProvider = new KeyCzarAuthenticatedMessageProvider()
        authenticatedMessageProvider.keyCzarCrypterLocator = keyCzarCrypterLocator
        authenticatedMessageProvider.identityConfig = identityConfig
    }

    def "crypter not initialized on init when ae tokens disabled"() {
        when:
        authenticatedMessageProvider.init()

        then:
        identityConfig.getFeatureAETokensDecrypt() >> false
        0 * keyCzarCrypterLocator.getCrypter()
    }

    def "crypter initialized on init when ae tokens enabled"() {
        given:
        Crypter crypter = Mock(Crypter)
        identityConfig.getFeatureAETokensDecrypt() >> true

        when:
        authenticatedMessageProvider.init()

        then:
        1 * keyCzarCrypterLocator.getCrypter() >> crypter
    }

    def "encryption and decryption throw error when ae tokens disabled"() {
        given:
        Crypter crypter = Mock(Crypter)
        identityConfig.getFeatureAETokensDecrypt() >> false

        when:
        authenticatedMessageProvider.decrypt()

        then:
        thrown(IllegalStateException)
        0 * keyCzarCrypterLocator.getCrypter()

        when:
        authenticatedMessageProvider.encrypt([0] as byte[])

        then:
        thrown(IllegalStateException)
        0 * keyCzarCrypterLocator.getCrypter()

    }

    def "encryption and decryption always lookup crypter on encryption and decryption"() {
        given:
        Crypter crypter = Mock(Crypter)
        identityConfig.getFeatureAETokensDecrypt() >> true

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
