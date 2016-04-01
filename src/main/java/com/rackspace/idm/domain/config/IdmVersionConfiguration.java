package com.rackspace.idm.domain.config;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class IdmVersionConfiguration implements ApplicationContextAware {

    private static final String BUILD_INFO_VERSION = "version";
    private static final String BUILD_INFO_BUILD = "buildVersion";
    private static final String BUILD_INFO_PROPERTIES_FILE = "classpath:build_info.properties";

    private ApplicationContext ctx;

    @Bean
    public IdmVersion getIdmVersionBean() throws IOException, ConfigurationException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        Resource versionInfoResource = ctx.getResource(BUILD_INFO_PROPERTIES_FILE);
        config.load(versionInfoResource.getInputStream());
        return new IdmVersion(new IdmVersion.Version(config.getString(BUILD_INFO_VERSION)),
                new IdmVersion.Build(config.getString(BUILD_INFO_BUILD)));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

}
