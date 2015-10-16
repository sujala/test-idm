package com.rackspace.idm.domain.security.jmx;

import com.rackspace.idm.domain.security.AETokenCache;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * A JMX wrapper for google's Guava Cache
 */
public class GuavaCacheStats implements GuavaCacheStatsMXBean {

    private final AETokenCache cache;

    public GuavaCacheStats(String cname, AETokenCache cache) {
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
        return cache.getCache().stats().requestCount();
    }

    @Override
    public long getHitCount() {
        return cache.getCache().stats().hitCount();
    }

    @Override
    public double getHitRate() {
        return cache.getCache().stats().hitRate();
    }

    @Override
    public long getMissCount() {
        return cache.getCache().stats().missCount();
    }

    @Override
    public double getMissRate() {
        return cache.getCache().stats().missRate();
    }

    @Override
    public long getLoadCount() {
        return cache.getCache().stats().loadCount();
    }

    @Override
    public long getLoadSuccessCount() {
        return cache.getCache().stats().loadSuccessCount();
    }

    @Override
    public long getLoadExceptionCount() {
        return cache.getCache().stats().loadExceptionCount();
    }

    @Override
    public double getLoadExceptionRate() {
        return cache.getCache().stats().loadExceptionRate();
    }

    @Override
    public long getTotalLoadTime() {
        return cache.getCache().stats().totalLoadTime();
    }

    @Override
    public double getAverageLoadPenalty() {
        return cache.getCache().stats().averageLoadPenalty();
    }

    @Override
    public long getEvictionCount() {
        return cache.getCache().stats().evictionCount();
    }

    @Override
    public long getSize() {
        return cache.getCache().size();
    }

    @Override
    public void cleanUp() {
        cache.getCache().cleanUp();
    }

    @Override
    public void invalidateAll() {
        cache.getCache().invalidateAll();
    }

    @Override
    public void recreateCache() {
        cache.recreateCache();
    }
}
