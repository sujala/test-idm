package com.rackspace.idm.config;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * @author john.eo Tries to load the config file from /etc/idm/, and if that
 *         fails, loads from from the classpath.
 */

@org.springframework.context.annotation.Configuration
public class PropertyFileConfiguration {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String EXTERNAL_CONFIG_FILE_PATH = "/etc/idm/"
        + CONFIG_FILE_NAME;
    private Logger logger = LoggerFactory
        .getLogger(PropertyFileConfiguration.class);

    /**
     * @return Configuration instance that is loaded from /etc/idm if it exists.
     *         If the file isn't there, loads from the classpath. Note that the
     *         classpath for the config file during test will be
     *         src/test/resources, rather than src/main/resources.
     */
    @Bean
    @Scope(value = "singleton")
    public Configuration getConfig() {
        File configFile = new File(EXTERNAL_CONFIG_FILE_PATH);
        if (configFile.exists()) {
            return readConfigFile(EXTERNAL_CONFIG_FILE_PATH);
        }

        logger.debug(String.format(
            "No config file found at %s. Loding from the classpath",
            CONFIG_FILE_NAME));
        return readConfigFile(CONFIG_FILE_NAME);
    }

    /**
     * Configuration instance from the classpath. Use this for unit tests, so
     * that the config file from /etc/idm won't be read instead.
     * 
     * @return
     */
    public Configuration getConfigFromClasspath() {
        return readConfigFile(CONFIG_FILE_NAME);
    }

    private Configuration readConfigFile(String filePath) {
        try {
            logger.debug(String.format("Attempting to open file %s", filePath));
            return new PropertiesConfiguration(filePath);
        } catch (ConfigurationException e) {
            logger.error(String.format("Could not load config file:\n %s", e));
            throw new IllegalStateException(String.format(
                "Could not load configuration file %s", filePath));
        }
    }
}
