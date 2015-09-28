package com.rackspace.idm.domain.security.jmx;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * A JMX wrapper for google's Guava Cache
 */
public class GuavaCacheStats implements GuavaCacheStatsMXBean {

    private final com.google.common.cache.Cache cache;

    public GuavaCacheStats(String cname, com.google.common.cache.Cache cache) {
        this.cache = cache;
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            String name = String.format("%s:type=Cache,name=%s",
                    this.cache.getClass().getPackage().getName(), cname);
            ObjectName mxBeanName = new ObjectName(name);
            if (!server.isRegistered(mxBeanName)) {
                server.registerMBean(this, new ObjectName(name));
            }
        } catch (MalformedObjectNameException ex) {
            // NOP
        } catch (InstanceAlreadyExistsException ex) {
            // NOP
        } catch (MBeanRegistrationException ex) {
            // NOP
        } catch (NotCompliantMBeanException ex) {
            // NOP
        }
    }

    @Override
    public long getRequestCount() {
        return cache.stats().requestCount();
    }

    @Override
    public long getHitCount() {
        return cache.stats().hitCount();
    }

    @Override
    public double getHitRate() {
        return cache.stats().hitRate();
    }

    @Override
    public long getMissCount() {
        return cache.stats().missCount();
    }

    @Override
    public double getMissRate() {
        return cache.stats().missRate();
    }

    @Override
    public long getLoadCount() {
        return cache.stats().loadCount();
    }

    @Override
    public long getLoadSuccessCount() {
        return cache.stats().loadSuccessCount();
    }

    @Override
    public long getLoadExceptionCount() {
        return cache.stats().loadExceptionCount();
    }

    @Override
    public double getLoadExceptionRate() {
        return cache.stats().loadExceptionRate();
    }

    @Override
    public long getTotalLoadTime() {
        return cache.stats().totalLoadTime();
    }

    @Override
    public double getAverageLoadPenalty() {
        return cache.stats().averageLoadPenalty();
    }

    @Override
    public long getEvictionCount() {
        return cache.stats().evictionCount();
    }

    @Override
    public long getSize() {
        return cache.size();
    }

    @Override
    public void cleanUp() {
        cache.cleanUp();
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
