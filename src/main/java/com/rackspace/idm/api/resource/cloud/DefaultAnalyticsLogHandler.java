package com.rackspace.idm.api.resource.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultAnalyticsLogHandler implements AnalyticsLogHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void log(String message) {
        logger.info(message);
    }
}
