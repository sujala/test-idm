package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.exception.IdmException;
import org.keyczar.exceptions.KeyczarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.BufferUnderflowException;

@Component
public class KeyCzarAuthenticatedMessageProvider implements AuthenticatedMessageProvider {
    public static final String ERROR_CODE_AM_DECRYPTION_EXCEPTION = "AMD-0000";
    public static final String ERROR_CODE_AM_DECRYPTION_TRUNCATED_TOKEN_EXCEPTION = "AMD-0001";
    public static final String ERROR_CODE_AM_DECRYPTION_UNKNOWN_EXCEPTION = "AMD-0002";
    public static final String ERROR_CODE_AM_ENCRYPTING_EXCEPTION = "AME-0000";

    @Autowired
    private KeyCzarCrypterLocator keyCzarCrypterLocator;

    @PostConstruct
    public void init() {
        //eagerly load crypter at app context load time so the app will fail at startup if the keys can't be read.
        try {
            keyCzarCrypterLocator.getCrypter();
        } catch (Exception e) {
            throw new IdmException("Error retrieving cryptor for AE Token Support. Please verify keys are available.", e);
        }
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
        } catch (BufferUnderflowException e) {
            throw new UnmarshallTokenException(ERROR_CODE_AM_DECRYPTION_TRUNCATED_TOKEN_EXCEPTION, "Error encountered decrypting bytes", e);
        } catch (Exception e) {
            //catch general exception, and wrap in IDM specific exception hierarchy
            throw new UnmarshallTokenException(ERROR_CODE_AM_DECRYPTION_UNKNOWN_EXCEPTION, "Error encountered decrypting bytes", e);
        }
    }
}
