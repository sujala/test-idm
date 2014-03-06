package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.devops.DevOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DevOpsResource {
    @Autowired
    DevOpsService devOpsService;

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @PUT
    @Path("cloud/users/encrypt")
    public Response encryptUsers(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        devOpsService.encryptUsers(authToken);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * Retrieves a log of ldap calls made while processing a previous request where the X-LOG-LDAP header (with a value of true) to the request
     *
     * Only callable by service admins and when the configuration property "allow.ldap.logging" is set to true in the configuration files (will
     * return 404 otherwise)
     *
     * @param uriInfo
     * @param authToken
     * @param logName
     * @return
     */
    @GET
    @Path("/ldap/log/{logName}")
    @Produces({MediaType.APPLICATION_XML})
    public Response getLog(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("logName") String logName) {
        return devOpsService.getLdapLog(uriInfo, authToken, logName).build();
    }
}
