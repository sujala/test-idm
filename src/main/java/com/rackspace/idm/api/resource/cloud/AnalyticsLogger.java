package com.rackspace.idm.api.resource.cloud;

public interface AnalyticsLogger {
    void log(Long startTime, String authToken, String basicAuth, String host, String userAgent, String method, String path, int status);
}
