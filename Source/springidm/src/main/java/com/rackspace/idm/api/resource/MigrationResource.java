package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.migration.CloudMigrationService;
import com.rackspace.idm.api.resource.tenant.TenantsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/9/12
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class MigrationResource {

    private final TenantsResource tenantsResource;
    
    @Context
    private UriInfo uriInfo;

    @Autowired
    CloudMigrationService cloudMigrationService;

    @Autowired
    public MigrationResource(TenantsResource tenantsResource) {
        this.tenantsResource = tenantsResource;
    }

    @POST
    @Path("cloud/users/{username}")
    public Response migrateCloudUserByUsername(@PathParam("username") String username) throws Exception {
        String id = cloudMigrationService.migrateUserByUsername(username, true);
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).build();
    }

    @PUT
    @Path("cloud/users/{username}/enable")
    public Response enableMigratedUserByUsername(@PathParam("username") String username) throws Exception {
        cloudMigrationService.setMigratedUserEnabledStatus(username, false);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @PUT
    @Path("cloud/users/{username}/disable")
    public Response disableMigratedUserByUsername(@PathParam("username") String username) throws Exception {
        cloudMigrationService.setMigratedUserEnabledStatus(username, true);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("cloud/users/{username}/unmigrate")
    public Response unmigrateCloudUserByUsername(@PathParam("username") String username) throws Exception {
        cloudMigrationService.unmigrateUserByUsername(username);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("cloud/users/{username}")
    public Response getMigratedCloudUserByUsername(@PathParam("username") String username) throws Exception {
        return cloudMigrationService.getMigratedUser(username).build();
    }

    @GET
    @Path("cloud/users/{username}/roles")
    public Response getMigratedCloudUserRolesByUsername(@PathParam("username") String username) throws Exception {
        return cloudMigrationService.getMigratedUserRoles(username).build();
    }

    @GET
    @Path("cloud/users/{username}/endpoints")
    public Response getMigratedCloudUserEndpointsByUsername(@PathParam("username") String username) throws Exception {
        return cloudMigrationService.getMigratedUserEndpoints(username).build();
    }


    @POST
    @Path("cloud/baseurls")
    public Response migrateBaseURLs() throws Exception {
        cloudMigrationService.migrateBaseURLs();
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
