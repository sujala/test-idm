package com.rackspace.idm.domain.dao;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.rackspace.idm.domain.entity.AuthCredentials;

public interface IDMClient {

    @POST
    @Consumes({"application/xml", "application/json"})
    @Path("/tokens")
    com.rackspace.api.idm.v1.Auth getAccessToken(
        @HeaderParam("Content-Type") String contentType,
        @HeaderParam("Accept") String accept,
        @HeaderParam("X-Auth-Token") String authHeader, AuthCredentials trParam);

    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/tokens/{tokenString}")
    com.rackspace.api.idm.v1.Auth validateAccessToken(
        @HeaderParam("X-Auth-Token") String authHeader);

}
