package com.rackspace.idm.domain.security.encrypters;

import org.keyczar.Crypter;

public interface KeyCzarCrypterLocator {
    /**
     * Retrieve the crypter to use for encrypting/decrypting the ae tokens.
     * @return
     */
    Crypter getCrypter();
}
