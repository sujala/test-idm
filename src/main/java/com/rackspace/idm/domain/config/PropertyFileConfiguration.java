package com.rackspace.idm.domain.config;

import com.rackspace.idm.util.EncryptedIOFactory;
import com.rackspace.idm.util.PropertyFileEncryptionConfigurationFactory;
import com.rackspace.idm.util.SystemEnvPropertyConfigurationFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.reloading.InvariantReloadingStrategy;
import org.apache.commons.configuration.reloading.ReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.StringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.io.File;

/**
 * @author john.eo Tries to load the config file from system defined location, and if that
 *         fails, loads from the classpath.
 */
@org.springframework.context.annotation.Configuration
public class PropertyFileConfiguration {
    public static final String CONFIG_FILE_NAME = "idm.properties";

    public static final String RELOADABLE_CONFIG_FILE_NAME = "idm.reloadable.properties";

    private static final int NUM_MILLISECONDS_IN_SECOND = 1000;

    /**
     * The name of the system property that will be checked to determine whether or not property encryption/decryption is
     * disabled. By default it is enabled. Set this property to "true" to disable the decryption of property values.
     */
    public static final String PROPERTY_ENCRYPTION_DISABLED_PROP_NAME = "property_encryption_disabled";

    private final Logger logger = LoggerFactory
        .getLogger(PropertyFileConfiguration.class);

    /**
     * @return Configuration instance that is loaded from system defined location it exists.
     *         If the file isn't there, loads from the classpath. 
     */
    @Primary
    @Bean(name = "staticConfiguration")
    @Scope(value = "singleton")
    public Configuration getConfig() {
    	final String externalConfigFile = System.getProperty(IdentityConfig.CONFIG_FOLDER_SYS_PROP_NAME) + "/" + CONFIG_FILE_NAME;
        File configFile = new File(externalConfigFile);
        if (configFile.exists()) {
            return readConfigFile(externalConfigFile);
        }

        logger.debug(String.format("No config file found at %s. Loading from the classpath", externalConfigFile));
        
        return readConfigFile(CONFIG_FILE_NAME);
    }

    /**
     * @return Configuration instance that is loaded from system defined location it exists.
     *         If the file isn't there, loads from the classpath.
     */
    @Bean(name = "reloadableConfiguration")
    @Scope(value = "singleton")
    public Configuration getReloadableConfig(@Qualifier("staticConfiguration") Configuration staticConfiguration) {
        final String externalConfigFile = System.getProperty(IdentityConfig.CONFIG_FOLDER_SYS_PROP_NAME) + "/" + RELOADABLE_CONFIG_FILE_NAME;
        File configFile = new File(externalConfigFile);
        String filePath = configFile.exists() ? externalConfigFile : RELOADABLE_CONFIG_FILE_NAME;
        logger.debug(String.format("No config file found at %s. Loading from the classpath", externalConfigFile));

        FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
        strategy.setRefreshDelay(NUM_MILLISECONDS_IN_SECOND * staticConfiguration.getInt(IdentityConfig.PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME, IdentityConfig.PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE));
        return readConfigFile(filePath, strategy);
    }

    Configuration readConfigFile(String filePath) {
        return readConfigFile(filePath, new InvariantReloadingStrategy());
    }

    Configuration readConfigFile(String filePath, ReloadingStrategy reloadingStrategy) {
        try {
            logger.debug(String.format("Attempting to open file %s", filePath));
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setReloadingStrategy(reloadingStrategy);
            if (isPropertyEncryptionEnabled()) {
                //use custom IOFactory if encryption enabled. Otherwise use default.
                logger.debug("Config file property encryption is enabled");
                config.setIOFactory(encryptedIOFactory());
            }
            else {
                logger.debug("Config file property encryption is disabled");
            }
            config.setFileName(filePath);
            config.load();
            return config;
        } catch (ConfigurationException e) {
            logger.error(String.format("Could not load config file:\n %s", e));
            throw new IllegalStateException(String.format(
                "Could not load configuration file %s", filePath), e);
        }
    }

    @Bean
    public StringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();

        if (isPropertyEncryptionEnabled()) {
            StringPBEConfig config = encryptionConfigurationFactory().loadPropertyFileEncryptionConfig();
            if (config == null) {
                logger.warn("No configuration loaded for property file encryption. If encrypted properties are found, the " +
                        "application will fail to load.");
            }
            else {

                if (StringUtils.isNotBlank(config.getProviderName())) {
                    logger.info(String.format("Using encryption provider '%s' to decrypt configuration values."
                            , config.getProviderName()));
                }

                if (StringUtils.isNotBlank(config.getAlgorithm())) {
                    logger.info(String.format("Using encryption algorithm '%s' to decrypt configuration values."
                            , config.getAlgorithm()));
                }

                /*
                For some reason config.getPassword() decides that it should throw a NPE if the password is null. Null
                passwords are fine from IDM perspective as long as nothing needs to be decrypted.
                 */
                try {
                    if (StringUtils.isBlank(config.getPassword())) {
                        logger.warn("A password has not been provided to decrypt encrypted configuration values. If any " +
                                "configuration values are encrypted an EncryptionInitializationException will be thrown");
                    }
                } catch (NullPointerException ex) {
                    logger.warn("A password has not been provided to decrypt encrypted configuration values. If any " +
                            "configuration values are encrypted an EncryptionInitializationException will be thrown");
                }

                stringEncryptor.setConfig(config);
            }
        }

        return stringEncryptor;
    }

    /**
     * Creates the EncryptedIOFactory bean. The factory creates a reader that will decrypt encrypted values. If for some
     * reason the decryption needs to be disabled, the decryption can be bypassed by
     * setting the bypassDecryption flag on the encryptedIOFactory.
     *
     * @return
     */
    @Bean
    public EncryptedIOFactory encryptedIOFactory() {
        return new EncryptedIOFactory(stringEncryptor());
    }

    @Bean
    public PropertyFileEncryptionConfigurationFactory encryptionConfigurationFactory() {
        return new SystemEnvPropertyConfigurationFactory();
    }

    /**
     * Returns whether property encryption will be enabled in the bean returned by getConfig().
     */
    public boolean isPropertyEncryptionEnabled() {
        String disableEncryption = System.getProperty(PROPERTY_ENCRYPTION_DISABLED_PROP_NAME);
        return !(Boolean.valueOf(disableEncryption));
    }

}
