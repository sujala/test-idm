package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import com.rackspace.idm.domain.security.InitializationException;
import org.apache.commons.configuration.Configuration;
import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileSystemKeyCzarCrypterLocator implements KeyCzarCrypterLocator {
    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME;
    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT;

    private volatile Crypter crypter = null;

    private Configuration config;

    @Autowired
    public FileSystemKeyCzarCrypterLocator(Configuration config) {
        this.config = config;
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
            String keyLocation = config.getString(SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME, SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT);
            return new Crypter(keyLocation);
        } catch (KeyczarException e) {
            throw new InitializationException("Error initializing AE Token Service crypter", e);
        }
    }
}
