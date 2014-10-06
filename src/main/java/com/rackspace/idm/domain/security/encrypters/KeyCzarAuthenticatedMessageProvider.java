package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import com.rackspace.idm.domain.security.InitializationException;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import org.apache.commons.configuration.Configuration;
import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class KeyCzarAuthenticatedMessageProvider implements AuthenticatedMessageProvider {
    public static final String ERROR_CODE_AM_DECRYPTION_EXCEPTION = "AMD-0000";
    public static final String ERROR_CODE_AM_ENCRYPTING_EXCEPTION = "AME-0000";

    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME;
    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT;

    private volatile Crypter crypter = null;

    @Autowired
    private Configuration config;

    public byte[] encrypt(byte[] bytes) {
        try {
            return getCrypter().encrypt(bytes);
        } catch (KeyczarException e) {
            throw new MarshallTokenException(ERROR_CODE_AM_ENCRYPTING_EXCEPTION, "Error encountered encrypting bytes", e);
        }
    }

    public byte[] decrypt(byte[] bytes) {
        try {
            return getCrypter().decrypt(bytes);
        } catch (KeyczarException e) {
            throw new UnmarshallTokenException(ERROR_CODE_AM_DECRYPTION_EXCEPTION, "Error encountered decrypting bytes", e);
        }
    }

    /**
     * This performs lazy initialization of the crypter.
     *
     * @return
     */
    private Crypter getCrypter() {
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
