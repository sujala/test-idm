package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Cloud Auth 1.1 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Autowired
    private DelegateCloud11Service delegateCloud11Service;


    @Autowired
    public Cloud11VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getCloud11VersionInfo(@Context HttpHeaders httpHeaders)
            throws IOException {
        return cloudClient.get(getCloudAuthV11Url(), httpHeaders).build();
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("auth")
    public Response authenticate(@Context HttpServletResponse response, @Context HttpHeaders httpHeaders, String body)
            throws IOException {
        return getCloud11Service().authenticate(response,httpHeaders, body).build();
    }

    @GET
    @Path("token")
    public Response validateToken(
            @QueryParam("belongsTo") String belongsTo,
            @QueryParam("type") String type,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().validateToken(belongsTo, type, httpHeaders).build();
    }

    @DELETE
    @Path("token")
    public Response revokeToken(
            @PathParam("contentType") String contentType, @Context HttpHeaders httpHeaders, String body
    ) throws IOException {
        //Todo: Jorge implement this method.
        return null;
    }

    private Cloud11Service getCloud11Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud11Service;
        } else {
            return defaultCloud11Service;
        }
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
