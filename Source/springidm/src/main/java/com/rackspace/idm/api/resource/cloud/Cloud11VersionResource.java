package com.rackspace.idm.api.resource.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
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

    @Autowired
    private CloudClient cloudClient;

    @Value("#{properties.cloudAuth11url}")
    private String url;

    @GET
    public Response getCloud11VersionInfo() throws IOException {
        return cloudClient.get(url,null,null);
    }

    @POST
    @Path("auth")
    public Response authenticate(
            @HeaderParam("Content-type") String  contentType, String body) throws IOException {

        return cloudClient.post(url.concat("auth"),contentType , body);
    }
}
