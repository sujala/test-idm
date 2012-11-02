package com.rackspace.idm.domain.config;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class ExternalBeansConfiguration {

    @Bean
    Mapper getMapper() {
        return new DozerBeanMapper();
    }
}
