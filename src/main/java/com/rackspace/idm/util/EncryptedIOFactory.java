package com.rackspace.idm.util;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.StringEncryptor;

import java.io.Reader;

/**
 * Used to construct new EncryptedPropertiesReader instances.
 */
public class EncryptedIOFactory extends PropertiesConfiguration.DefaultIOFactory {

    private StringEncryptor encryptor;

    public EncryptedIOFactory(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public PropertiesConfiguration.PropertiesReader createPropertiesReader(Reader in, char delimiter) {
        return new EncryptedPropertiesReader(in, delimiter, encryptor);
    }

    public StringEncryptor getEncryptor() {
        return encryptor;
    }
}
