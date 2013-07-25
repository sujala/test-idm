package com.rackspace.idm.util;

import org.jasypt.encryption.pbe.config.StringPBEConfig;

/**
 */
public interface PropertyFileEncryptionConfigurationFactory {
    /**
     * Generates a StringPBEConfig that can be used to initialize a StringEncryptor. If defaults
     *
     * @return
     */
    StringPBEConfig loadPropertyFileEncryptionConfig();
}
