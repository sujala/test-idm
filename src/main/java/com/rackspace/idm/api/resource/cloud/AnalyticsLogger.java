package com.rackspace.idm.api.resource.cloud;

public interface AnalyticsLogger {
    void log(Long startTime, String authToken, String basicAuth, String host, String remoteHost, String userAgent, String method, String path, int status, String requestBody, String contentType);
}
