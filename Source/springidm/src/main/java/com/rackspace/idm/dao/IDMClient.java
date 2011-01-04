package com.rackspace.idm.dao;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.rackspace.idm.oauth.AuthCredentials;

public interface IDMClient {

    @POST
    @Consumes({"application/xml", "application/json"})
    @Path("/token")
    com.rackspace.idm.jaxb.Auth getAccessToken(
        @HeaderParam("Content-Type") String contentType,
        @HeaderParam("Accept") String accept,
        @HeaderParam("Authorization") String authHeader, AuthCredentials trParam);

    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/token/{tokenString}")
    com.rackspace.idm.jaxb.Auth validateAccessToken(
        @HeaderParam("Authorization") String authHeader);

}
