package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.service.AuthenticationServiceSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Cloud Auth 2.0 API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    @Autowired
    AuthenticationServiceSelector authenticationServiceSelector;

    @GET
    public Response getCloud20VersionInfo() {
        //TODO: Implement Cloud Version Info Call
        return Response.ok().build();
    }
}
