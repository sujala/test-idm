package com.rackspace.idm.api.resource.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private CloudClient cloudClient;

    @Value("#{properties.cloudAuth10url}")
    private String url;

    @GET
    public Response getCloud10VersionInfo() throws IOException {
        return cloudClient.get(url);
    }

//    @GET
//    public Response authenticate(
//            @HeaderParam("X-Auth-User") String  username,
//            @HeaderParam("X-Auth-Key") String  key){
//
//        //TODO forward call to Auth 1.0
//        return Response.ok().build();
//    }
}
