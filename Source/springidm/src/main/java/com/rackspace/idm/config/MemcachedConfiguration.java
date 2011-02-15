package com.rackspace.idm.config;

import java.io.IOException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class MemcachedConfiguration {
    private static final String MEMCACHED_CLIENT_ERROR_MSG = "Could not intantiate a memcached client.";
    private Logger logger;

    @Autowired
    private Configuration config;

    public MemcachedConfiguration() {
        logger = LoggerFactory.getLogger(MemcachedConfiguration.class);
    }

    /**
     * Used for testing
     * 
     * @param logger
     *            Can use a stub logger here.
     */
    public MemcachedConfiguration(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Bean(destroyMethod="shutdown") 
    public MemcachedClient memcacheClient() {
        try {
            String serverList = config.getString("memcached.serverList");
            logger
                .debug("Creating memcached client for servers {}", serverList);
            return new MemcachedClient(new BinaryConnectionFactory(), AddrUtil
                .getAddresses(serverList));
        } catch (IOException ex) {
            System.out.println(ex);
            logger.error(MEMCACHED_CLIENT_ERROR_MSG);
            throw new IllegalStateException(MEMCACHED_CLIENT_ERROR_MSG, ex);
        }
    }
}
