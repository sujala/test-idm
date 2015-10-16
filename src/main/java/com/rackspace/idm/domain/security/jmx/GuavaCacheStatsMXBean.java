package com.rackspace.idm.domain.security.jmx;

public interface GuavaCacheStatsMXBean {

    public long getRequestCount();

    public long getHitCount();

    public double getHitRate();

    public long getMissCount();

    public double getMissRate();

    public long getLoadCount();

    public long getLoadSuccessCount();

    public long getLoadExceptionCount();

    public double getLoadExceptionRate();

    public long getTotalLoadTime();

    public double getAverageLoadPenalty();

    public long getEvictionCount();

    public long getSize();

    public void cleanUp();

    public void invalidateAll();

    public void recreateCache();
}
