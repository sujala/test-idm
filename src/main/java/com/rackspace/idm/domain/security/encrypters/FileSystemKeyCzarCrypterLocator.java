package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.security.InitializationException;
import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileSystemKeyCzarCrypterLocator implements KeyCzarCrypterLocator {
    private volatile Crypter crypter = null;

    private IdentityConfig identityConfig;

    @Autowired
    public FileSystemKeyCzarCrypterLocator(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }

    @Override
    public Crypter getCrypter() {
        return getCrypterInternal();
    }

    /**
     * This performs lazy initialization of the crypter.
     *
     * @return
     */
    private Crypter getCrypterInternal() {
        Crypter result = crypter;
        if (result == null) {
            synchronized (this) {
                result = crypter;
                if (result == null) {
                    crypter = result = initCrypter();
                }
            }
        }
        return result;
    }

    private Crypter initCrypter() {
        try {
            String keyLocation = identityConfig.getStaticConfig().getAEFileStorageKeyLocation();
            return new Crypter(keyLocation);
        } catch (KeyczarException e) {
            throw new InitializationException("Error initializing AE Token Service crypter", e);
        }
    }

}
