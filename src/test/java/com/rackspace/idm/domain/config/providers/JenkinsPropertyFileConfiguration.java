package com.rackspace.idm.domain.config.providers;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.Locale;
import java.util.ResourceBundle;

@org.springframework.context.annotation.Configuration
public class JenkinsPropertyFileConfiguration {
    private static final String CONFIG_FILE_NAME = "idm.properties";

    @Bean
    @Scope(value = "singleton")
    public Configuration getConfig() throws ConfigurationException {
    	final String externalConfigFile = "target/config/OPENLDAP/" + CONFIG_FILE_NAME;
        return new PropertiesConfiguration(externalConfigFile);
    }

    @Bean
    ResourceBundle faultMessageBundle() {
        return ResourceBundle.getBundle("fault_messages", Locale.ENGLISH);
    }
}
