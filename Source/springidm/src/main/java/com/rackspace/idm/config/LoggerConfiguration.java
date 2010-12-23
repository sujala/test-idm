package com.rackspace.idm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggerConfiguration {

    @Bean
    public LoggerFactoryWrapper getLoggerFactoryWrapper() {
        return new LoggerFactoryWrapper();
    }
}
