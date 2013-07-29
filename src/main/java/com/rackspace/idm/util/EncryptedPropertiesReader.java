package com.rackspace.idm.util;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;

/**
 * Custom reader to decrypt values if they are determined to be encrypted. A value is deemed
 * encrypted if it is surrounded by 'ENC(value)' (e.g. ENC(an_encrypted_value).
 * <p/>
 * <p>
 * The only change required is
 * that AFTER the propery value is parsed/unescaped the value must be decrypted, if necessary. Unfortunately
 * {@link org.apache.commons.configuration.PropertiesConfiguration.PropertiesReader#doParseProperty(String)} is
 * private,
 * {@link PropertiesConfiguration#unescapeJava(String, char)} is unavailable in this class,
 * and the only way to set the propertyValue variable and unescape it is to call
 * {@link org.apache.commons.configuration.PropertiesConfiguration.PropertiesReader#initPropertyValue(String)}. The
 * decrypted value should NOT be unescaped, so the only way to decrypt the value
 * without unescaping the decrypted value is to override the
 * {@link org.apache.commons.configuration.PropertiesConfiguration.PropertiesReader#getPropertyValue()} method.
 * </p>
 */
public class EncryptedPropertiesReader extends PropertiesConfiguration.PropertiesReader {
    private final Logger logger = LoggerFactory.getLogger(EncryptedPropertiesReader.class);

    /**
     * The encryptor used to decrypt the encrypted property values
     */
    private StringEncryptor encryptor;

    /**
     * Creates a new instance of <code>EncryptedPropertiesReader</code> and sets
     * the underlying reader and the list delimiter.
     *
     * @param reader        the reader
     * @param listDelimiter the list delimiter character
     * @param encryptor     the encryptor used to decrypt encrypted values
     * @since 1.3
     */
    public EncryptedPropertiesReader(Reader reader, char listDelimiter, StringEncryptor encryptor) {
        super(reader, listDelimiter);
        this.encryptor = encryptor;
    }

    /**
     * Decrypts the value using the provided encryptor if the value is determined to be encrypted.
     */
    @Override
    public String getPropertyValue() {
        try {
            return convertValue(super.getPropertyValue());
        } catch (EncryptionOperationNotPossibleException e) {
            logger.error(String.format("Could not decrypt property '%s':\n %s", getPropertyName(), e));
            throw new ConversionException(String.format("Could not decrypt property '%s':\n", getPropertyName()));
        }
    }

    /**
     * Decrypt properties if necessary
     */
    private String convertValue(String originalValue) {
        if (!PropertyValueEncryptionUtils.isEncryptedValue(originalValue)) {
            return originalValue;
        }
        return PropertyValueEncryptionUtils.decrypt(originalValue, encryptor);
    }

}