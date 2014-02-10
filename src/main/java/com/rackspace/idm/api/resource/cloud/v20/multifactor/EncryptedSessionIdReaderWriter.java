package com.rackspace.idm.api.resource.cloud.v20.multifactor;

import com.rackspace.idm.exception.MultiFactorInitializationException;
import com.rackspace.idm.exception.MultiFactorSessionIdEncryptionException;
import com.rackspace.idm.exception.MultiFactorSessionIdFormatException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class EncryptedSessionIdReaderWriter implements SessionIdReaderWriter {
    public static final DateTimeFormatter RFC822DATEFORMAT = DateTimeFormat.forPattern("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z").withLocale(Locale.US).withZone(DateTimeZone.UTC);
    private static final Logger LOG = LoggerFactory.getLogger(EncryptedSessionIdReaderWriter.class);

    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME = "multifactor.key.location";
    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT = "/etc/idm/config/keys";

    private volatile Crypter crypter = null;

    @Autowired
    private List<SessionIdVersion> versions;

    @Autowired
    private Configuration config;

    @Override
    public SessionId readEncoded(String encodedFormat) {
        String plaintext = decryptCipherText(encodedFormat);

        SessionIdVersion version = getSessionIdVersionForPlaintext(plaintext);
        SessionId sessionId = version.getSessionId(plaintext);
        return sessionId;
    }

    @Override
    public String writeEncoded(SessionId toEncode) {
        SessionIdVersion version = getSessionIdVersionForPrefix(toEncode.getVersion());

        String plaintext = version.getPlaintext(toEncode);
        String encrypted = encryptPlainText(plaintext);
        return encrypted;
    }

    private SessionIdVersion getSessionIdVersionForPlaintext(String plainText) {
        String versionPrefix = plainText.substring(0,SessionIdVersion.PREFIX_LENGTH);

        SessionIdVersion version = getSessionIdVersionForPrefix(versionPrefix);
        if (version == null) {
            throw new MultiFactorSessionIdFormatException(String.format("Unknown session id plaintext format version '%s'", versionPrefix));
        }
        return version;
    }

    private SessionIdVersion getSessionIdVersionForPrefix(String versionPrefix) {
        SessionIdVersion result = null;
        for (SessionIdVersion version : versions) {
            if (version.getPrefix().equals(versionPrefix)) {
                result = version;
            }
        }
        return result;
    }

    private String encryptPlainText(String sessionIdPlaintext) {
        try {
            String encryptedSessionId = getCrypter().encrypt(sessionIdPlaintext);
            return encryptedSessionId;
        } catch (KeyczarException e) {
            throw new MultiFactorSessionIdEncryptionException("Error encrypting sessionId", e);
        }
    }

    private String decryptCipherText(String ciphertext) {
        String plaintext = null;
        try {
            plaintext = getCrypter().decrypt(ciphertext);
        } catch (KeyczarException e) {
            throw new MultiFactorSessionIdEncryptionException("Error decrypting sessionId", e);
        }
        return plaintext;
    }


    /**
     * This performs lazy initialization of the crypter.
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
            String keyLocation = config.getString(MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT);
            return new Crypter(keyLocation);
        } catch (KeyczarException e) {
            LOG.error("An error occurred creating crypter to encrypt/decrypt session ids for multifactor authentication", e);
            throw new MultiFactorInitializationException("Error initializing multifactor crypter", e);
        }
    }
}
