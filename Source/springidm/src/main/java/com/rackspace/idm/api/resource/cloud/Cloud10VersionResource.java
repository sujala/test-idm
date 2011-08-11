package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.service.AuthenticationServiceSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Cloud Auth 1.0 API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud10VersionResource {

    @Autowired
    AuthenticationServiceSelector authenticationServiceSelector;

    @GET
    public Response getCloud10VersionInfo() {
        //TODO: Implement Cloud Version Info Call
        return Response.ok().build();
    }

    @GET
    public Response authenticate(
            @HeaderParam("X-Auth-User") String  username,
            @HeaderParam("X-Auth-Key") String  key){

        //TODO forward call to Auth 1.0
        return Response.ok().build();
    }
}
