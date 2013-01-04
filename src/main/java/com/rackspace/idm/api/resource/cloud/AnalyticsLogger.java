package com.rackspace.idm.api.resource.cloud;

public interface AnalyticsLogger {
    void log(String authToken, String host, String userAgent, String method, String path);
}
