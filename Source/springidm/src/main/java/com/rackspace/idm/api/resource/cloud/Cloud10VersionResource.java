package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cloud Auth 1.0 API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud10VersionResource {

    public static final String HEADER_AUTH_TOKEN = "X-Auth-Token";
    @Deprecated
    public static final String HEADER_STORAGE_TOKEN = "X-Storage-Token";
    @Deprecated
    public static final String HEADER_STORAGE_URL = "X-Storage-Url";
    public static final String HEADER_CDN_URL = "X-CDN-Management-Url";
    public static final String HEADER_SERVER_MANAGEMENT_URL = "X-Server-Management-Url";
    public static final String HEADER_STORAGE_INTERNAL_URL = "X-Storage-Internal-Url";

    public static final String HEADER_AUTH_USER = "X-Auth-User";
    public static final String HEADER_AUTH_KEY = "X-Auth-Key";
    public static final String HEADER_STORAGE_USER = "X-Storage-User";
    public static final String HEADER_STORAGE_PASS = "X-Storage-Pass";

    public static final String AUTH_V1_0_FAILED_MSG = "Bad username or password";
    public static final String AUTH_V1_0_USR_DISABLED_MSG = "Account disabled";

    public static final String SERVICENAME_CLOUD_FILES = "cloudFiles";
    public static final String SERVICENAME_CLOUD_FILES_CDN = "cloudFilesCDN";
    public static final String SERVICENAME_CLOUD_SERVERS = "cloudServers";

    private final Configuration config;
    private final CloudClient cloudClient;

    @Autowired
    public Cloud10VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getCloud10VersionInfo(@Context HttpHeaders httpHeaders,
        @HeaderParam(HEADER_AUTH_USER) String username,
        @HeaderParam(HEADER_AUTH_KEY) String key) throws IOException {
        return cloudClient.get(getCloudAuthV10Url(), httpHeaders).build();
    }

    private String getCloudAuthV10Url() {
        return config.getString("cloudAuth10url");
    }
}
