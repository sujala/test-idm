package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Cloud Auth 1.0 API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud10VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    
    @Autowired
    public Cloud10VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getCloud10VersionInfo() throws IOException {
        return cloudClient.get(getCloudAuthV10Url(),null,null);
    }

//    @GET
//    public Response authenticate(
//            @HeaderParam("X-Auth-User") String  username,
//            @HeaderParam("X-Auth-Key") String  key){
//
//        //TODO forward call to Auth 1.0
//        return Response.ok().build();
//    }
    
    private String getCloudAuthV10Url() {
        return config.getString("cloudAuth10url");
    }
}
