package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
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

    @PostConstruct
    public void init() {
        //eagerly load crypter at app context load time
        keyCzarCrypterLocator.getCrypter();
    }

    public byte[] encrypt(byte[] bytes) {
        try {
            return keyCzarCrypterLocator.getCrypter().encrypt(bytes);
        } catch (KeyczarException e) {
            throw new MarshallTokenException(ERROR_CODE_AM_ENCRYPTING_EXCEPTION, "Error encountered encrypting bytes", e);
        }
    }

    public byte[] decrypt(byte[] bytes) {
        try {
            return keyCzarCrypterLocator.getCrypter().decrypt(bytes);
        } catch (KeyczarException e) {
            throw new UnmarshallTokenException(ERROR_CODE_AM_DECRYPTION_EXCEPTION, "Error encountered decrypting bytes", e);
        }
    }
}
