package com.rackspace.idm.domain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Making the LoggerFactory injectable through IoC.
 */
public class LoggerFactoryWrapper {

    public Logger getLogger(Class<? extends Object> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
