package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.exception.IdmException;
import org.apache.commons.configuration.Configuration;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class KeyCzarAuthenticatedMessageProvider implements AuthenticatedMessageProvider {
    public static final String ERROR_CODE_AM_DECRYPTION_EXCEPTION = "AMD-0000";
    public static final String ERROR_CODE_AM_ENCRYPTING_EXCEPTION = "AME-0000";

    @Autowired
    private KeyCzarCrypterLocator keyCzarCrypterLocator;

    @Autowired
    private IdentityConfig identityConfig;

    @PostConstruct
    public void init() {
        //eagerly load crypter at app context load time if ae tokens enabled in any fashion. By doing this the app
        //will fail at startup if the keys can't be read.
        if (areAeTokensEnabled()) {
            try {
                keyCzarCrypterLocator.getCrypter();
            } catch (Exception e) {
                throw new IdmException("Error retrieving cryptor for AE Token Support. Please verify keys are available.", e);
            }
        }
    }

    public byte[] encrypt(byte[] bytes) {
        verifyAeEnabled();
        try {
            return keyCzarCrypterLocator.getCrypter().encrypt(bytes);
        } catch (KeyczarException e) {
            throw new MarshallTokenException(ERROR_CODE_AM_ENCRYPTING_EXCEPTION, "Error encountered encrypting bytes", e);
        }
    }

    public byte[] decrypt(byte[] bytes) {
        verifyAeEnabled();
        try {
            return keyCzarCrypterLocator.getCrypter().decrypt(bytes);
        } catch (KeyczarException e) {
            throw new UnmarshallTokenException(ERROR_CODE_AM_DECRYPTION_EXCEPTION, "Error encountered decrypting bytes", e);
        }
    }

    private void verifyAeEnabled() {
        if (!areAeTokensEnabled()) {
            throw new IllegalStateException("AE Tokens are not enabled. Encryption/Decryption not supported");
        }
    }

    /**
     * Are AE tokens supported by this node? Only need to test if decrypt is enabled since decrypt is considered enabled
     * if encrypt is enabled.
     */
    private boolean areAeTokensEnabled() {
        return identityConfig.getFeatureAETokensDecrypt();
    }

}
