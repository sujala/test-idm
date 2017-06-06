package com.rackspace.idm.domain.config;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

final class OpenSAMLConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpenSAMLConfiguration.class);

    @PostConstruct
    public void init() {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            String errMsg = "Exception initializing OpenSAML";
            logger.error(errMsg);
            throw new RuntimeException(errMsg, e);
        }
    }
}
