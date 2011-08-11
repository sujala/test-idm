package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.service.AuthenticationService;
import com.rackspace.idm.api.service.AuthenticationServiceSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Cloud Auth 1.1 API Versions
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    @Autowired
    AuthenticationServiceSelector authenticationServiceSelector;


    @GET
    public Response getCloud11VersionInfo() {
        //TODO: Implement Cloud Version Info Call
        AuthenticationService authenticationService = authenticationServiceSelector.getAuthenticationService();
        System.out.println(authenticationService);
        return Response.ok().build();
    }
}
