package com.rackspace.idm.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;
import com.rackspace.idm.api.resource.cloud.devops.DevOpsService;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private DevOpsService devOpsService;

    @Autowired
    private IdentityConfig identityConfig;

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

    /**
     * Retrieves the current node state for KeyCzar cached keys.
     *
     * @return The metadata format is JSON only since it is just for internal use.
     */
    @GET
    @Path("/keystore/meta")
    public Response getKeyMetadata(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return devOpsService.getKeyMetadata(authToken).build();
    }

    /**
     * Reset the KeyCzar cache.
     *
     * @return The metadata format is JSON only since it is just for internal use.
     */
    @PUT
    @Path("/keystore/meta")
    public Response resetKeyMetadata(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return devOpsService.resetKeyMetadata(authToken).build();
    }

    /**
     * Retrieves the IDM properties.
     *
     * @return IDM properties as JSON; JSON only since it is just for internal use.
     */
    @GET
    @Path("/props")
    public Response getIdmProps(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return devOpsService.getIdmProps(authToken).build();
    }

    @POST
    @Path("/federation/deletion")
    public Response expiredFederatedUsersDeletion(@HeaderParam(X_AUTH_TOKEN) String authToken, FederatedUsersDeletionRequest request) {
        return devOpsService.expiredFederatedUsersDeletion(authToken, request).build();
    }

    @POST
    @Path("/users/{userId}/multi-factor/setupsms")
    public Response setupSmsMfa(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("userId") String userId, MobilePhone mobilePhone) {
        if (identityConfig.getReloadableConfig().areMfaMigrationServicesEnabled()) {
            return devOpsService.setupSmsMfaOnUser(authToken, userId, mobilePhone).build();
        }
        throw new NotFoundException("Service Not Found");
    }

    @DELETE
    @Path("/users/{userId}/multi-factor")
    public Response removeMfa(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("userId") String userId) {
        if (identityConfig.getReloadableConfig().areMfaMigrationServicesEnabled()) {
            return devOpsService.removeMfaFromUser(authToken, userId).build();
        }
        throw new NotFoundException("Service Not Found");
    }
}
