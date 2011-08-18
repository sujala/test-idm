package com.rackspace.idm.api.resource.cloud;

import javax.ws.rs.PathParam;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

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
        return cloudClient.get(getCloudAuthV11Url(),null,null);
    }

    @POST
    @Path("auth{contentType:(\\.(xml|json))?}")
    public Response authenticate(
        @PathParam ("contentType") String contentType, @Context HttpHeaders httpHeaders, String body
    ) throws IOException {
        return cloudClient.post(getCloudAuthV11Url().concat(getPath("auth", contentType)), httpHeaders , body);
    }

    private String getPath(String path, String contentType) {
        if(contentType != null) {
            return path + contentType;
        }
        return path;
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
