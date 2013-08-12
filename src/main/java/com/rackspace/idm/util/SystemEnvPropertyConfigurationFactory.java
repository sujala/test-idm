package com.rackspace.idm.util;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.encryption.pbe.config.StringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

public class SystemEnvPropertyConfigurationFactory implements PropertyFileEncryptionConfigurationFactory {
    private final Logger logger = LoggerFactory
            .getLogger(SystemEnvPropertyConfigurationFactory.class);

    public static final String PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME = "property_encryption_password";
    public static final String PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME = "property_encryption_algorithm";
    public static final String PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME = "property_encryption_provider_name";

    public static final String DEFAULT_PROVIDER_NAME = "BC";
    /**
     * The default algorithm for encryption is PBEWITHMD5ANDDES since it does not require unlimited strength policy jars
     * to be installed on the host system. However, it is strongly recommended to override this default with a more
     * secure algorithm such as PBEWITHSHA256AND128BITAES-CBC-BC
     */
    public static final String DEFAULT_ALGORITHM = "PBEWITHMD5ANDDES";

    @Override
    public StringPBEConfig loadPropertyFileEncryptionConfig() {
        Security.addProvider(new BouncyCastleProvider());

        EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
        config.setProviderName(DEFAULT_PROVIDER_NAME);

        //set the algorithm via first non-null/empty value found: Environment Variable -> System Property -> Default value
        config.setAlgorithmSysPropertyName(PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME);
        if (StringUtils.isBlank(config.getAlgorithm())) {
            config.setAlgorithmEnvName(PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME);
        }
        if (StringUtils.isBlank(config.getAlgorithm())) {
            config.setAlgorithm(DEFAULT_ALGORITHM);
        }

        //set the provider via first non-null/empty value found: Environment Variable -> System Property -> Default value
        config.setProviderNameEnvName(PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME);
        if (StringUtils.isBlank(config.getProviderName())) {
            config.setProviderNameSysPropertyName(PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME);
        }
        if (StringUtils.isBlank(config.getProviderName())) {
            config.setProviderName(DEFAULT_PROVIDER_NAME);
        }

        //load the password from the environment variable, and if not found, then from System prop
        config.setPasswordSysPropertyName(PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME);
        if (StringUtils.isBlank(config.getAlgorithm())) {
            config.setPasswordEnvName(PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME);
        }

        return config;
    }
}
