package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cloud Auth 1.1 API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    
    @Autowired
    public Cloud11VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getCloud11VersionInfo() throws IOException {
        return cloudClient.get(getCloudAuthV11Url());
    }
    
    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
