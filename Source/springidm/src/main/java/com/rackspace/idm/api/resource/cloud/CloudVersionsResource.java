package com.rackspace.idm.api.resource.cloud;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cloud Auth API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudVersionsResource {
    
    private final Cloud10VersionResource cloud10VersionResource;
    private final Cloud11VersionResource cloud11VersionResource;
    private final Cloud20VersionResource cloud20VersionResource;
    private final Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CloudVersionsResource(Cloud10VersionResource cloud10VersionResource,
    Cloud11VersionResource cloud11VersionResource,
    Cloud20VersionResource cloud20VersionResource, Configuration config) {
        this.cloud10VersionResource = cloud10VersionResource;
        this.cloud11VersionResource = cloud11VersionResource;
        this.cloud20VersionResource = cloud20VersionResource;
        this.config = config;
    }
    
    @GET
    public Response getCloudVersionsInfo() {
        //TODO: Implement Cloud Version Info Call
        return Response.ok().build();
    }

    @Path("v1.0")
    public Cloud10VersionResource getCloud10VersionResource() {
            return cloud10VersionResource;
    }
    
    @Path("v1.1")
    public Cloud11VersionResource getCloud11VersionResource() {
            return cloud11VersionResource;
    }
    
    @Path("v2.0")
    public Cloud20VersionResource getCloud20VersionResource() {
            return cloud20VersionResource;
    }
}
