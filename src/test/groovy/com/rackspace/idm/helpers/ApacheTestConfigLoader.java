package com.rackspace.idm.helpers;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;

/**
 * Test helper class to load an Apache configuration file that contains the properties required for tests.
 */
public class ApacheTestConfigLoader {

    public static final String SYSTEM_PROP_CONFIG_LOCATION = "test.config.location";
    public static final String CONFIG_FILE_NAME = "test.properties";

    /**
     * Return a Configuration from the location specified in the System property 'test.config.location'. If not found,
     * load from the file 'test.properties' from the root classpath.
     * @return
     */
    public static Configuration getConfig() {
        final String externalConfigFile = System.getProperty(SYSTEM_PROP_CONFIG_LOCATION) + "/" + CONFIG_FILE_NAME;
        File configFile = new File(externalConfigFile);
        if (configFile.exists()) {
            return readConfigFile(externalConfigFile);
        }

        return readConfigFile(CONFIG_FILE_NAME);
    }

    /**
     * Return a Configuration from the location specified in the System property 'test.config.location'. If not found,
     * load from the file 'test.properties' from the root classpath.
     * @return
     */
    public static ApacheTestConfigurationWrapper getWrappedConfig() {
        return new ApacheTestConfigurationWrapper(getConfig());
    }

    public static Configuration readConfigFile(String filePath) {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setFileName(filePath);
            config.load();
            return config;
        } catch (ConfigurationException e) {
            throw new IllegalStateException(String.format(
                    "Could not load configuration file %s", filePath));
        }
    }
}
