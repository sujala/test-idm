package com.rackspace.idm.domain.config;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * @author john.eo Tries to load the config file from system defined location, and if that
 *         fails, loads from the classpath.
 */
@org.springframework.context.annotation.Configuration
public class PropertyFileConfiguration {
    private static final String CONFIG_FILE_NAME = "idm.properties";
    
    private Logger logger = LoggerFactory
        .getLogger(PropertyFileConfiguration.class);

    /**
     * @return Configuration instance that is loaded from system defined location it exists.
     *         If the file isn't there, loads from the classpath. 
     */
    @Bean(name="properties")
    @Scope(value = "singleton")
    public Configuration getConfig() {
    	final String externalConfigFile = System.getProperty("idm.properties.location") + "/" + CONFIG_FILE_NAME;
        File configFile = new File(externalConfigFile);
        if (configFile.exists()) {
            return getConfigFromClasspath(externalConfigFile);
        }

        logger.debug(String.format("No config file found at %s. Loding from the classpath", externalConfigFile));
        
        return getConfigFromClasspath(CONFIG_FILE_NAME);
    }


    @Bean
    ResourceBundle faultMessageBundle() {
        return ResourceBundle.getBundle("fault_messages", Locale.ENGLISH);
    }

    private Configuration getConfigFromClasspath(String filePath) {
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
