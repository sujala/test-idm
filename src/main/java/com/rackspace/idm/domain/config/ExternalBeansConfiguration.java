package com.rackspace.idm.domain.config;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

@org.springframework.context.annotation.Configuration
public class ExternalBeansConfiguration {

    @Bean
    public Mapper getMapper() {
        DozerBeanMapper mapper = new DozerBeanMapper();

        List<String> mappingFiles = Arrays.asList("xmlgregoriancalender-mapping.xml");
        mapper.setMappingFiles(mappingFiles);

        return mapper;
    }
}
