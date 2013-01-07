package com.rackspace.idm.api.resource.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultAnalyticsLogHandler implements AnalyticsLogHandler {

    private static final String AUDIT_LOGGER_ID = "analytics";

    Logger logger = LoggerFactory.getLogger(AUDIT_LOGGER_ID);

    @Override
    public void log(String message) {
        logger.info(message);
    }
}
