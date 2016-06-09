package com.rackspace.idm.api.resource.cloud.atomHopper;

public enum HttpClientConnectionEvictionStrategyType {
    DAEMON, ON_USE;

    public static HttpClientConnectionEvictionStrategyType byName(String name) {
        for (HttpClientConnectionEvictionStrategyType httpClientConnectionEvictionStrategyType : HttpClientConnectionEvictionStrategyType.values()) {
            if (httpClientConnectionEvictionStrategyType.name().equalsIgnoreCase(name)) {
                return httpClientConnectionEvictionStrategyType;
            }
        }
        return null;
    }
}
