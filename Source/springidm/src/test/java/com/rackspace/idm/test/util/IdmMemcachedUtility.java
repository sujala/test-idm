package com.rackspace.idm.test.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.config.MemcachedConfiguration;

public class IdmMemcachedUtility {
    private static final String LOCAL_SERVERLIST = "127.0.0.1:11211";
    private static final String DEV_SERVERLIST = "10.127.7.167:11211";
    private static final String QA_SERVERLIST = "10.127.7.165:11211";

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Logger logger = LoggerFactory.getLogger(IdmMemcachedUtility.class);

        String serverList = LOCAL_SERVERLIST;
        if (args.length > 0) {
            if ("dev".equalsIgnoreCase(args[0])) {
                serverList = DEV_SERVERLIST;
            } else if ("qa".equals(args[0])) {
                serverList = QA_SERVERLIST;
            }
        }
        Configuration config = new PropertiesConfiguration();
        config.addProperty("memcached.serverList", serverList);
        MemcachedClient mclient = new MemcachedConfiguration(config, logger).memcacheClient();
        
        Future<Boolean> result = mclient.flush(5);
        if (result.get()) {
            System.out.println("flushed");
        } else {
            System.out.println("flush call failed.");
        }
    }
}
