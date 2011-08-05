package com.rackspace.idm.api.resource.cloud;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

/**
 * Cloud Auth 2.0 API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    @GET
    public Response getCloud20VersionInfo() {
        //TODO: Implement Cloud Version Info Call
        return Response.ok().build();
    }
}
