package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.migration.CloudMigrationService;
import com.rackspace.idm.api.resource.cloud.migration.MigrateUserResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;

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

    @Autowired
    private CloudMigrationService cloudMigrationService;

    public MigrationResource(){};

    @GET
    @Path("cloud/users")
    public Response getMigratedUsers() {
        return cloudMigrationService.getMigratedUserList().build();
    }

    @GET
    @Path("cloud/users/pending")
    public Response getInMigrationUsers() {
        return cloudMigrationService.getInMigrationUserList().build();
    }

    @POST
    @Path("cloud/users/{username}")
    public Response migrateCloudUserByUsername(@PathParam("username") String username,
                                               @DefaultValue("false") @QueryParam("subusers") boolean processSubUsers) {
    	MigrateUserResponseType migrateUserResponseType = cloudMigrationService.migrateUserByUsername(username, processSubUsers);
    	return Response.status(Response.Status.OK).entity(migrateUserResponseType).build();
    }

    @PUT
    @Path("cloud/users/{username}/enable")
    public Response enableMigratedUserByUsername(@PathParam("username") String username) throws IOException, JAXBException {
        cloudMigrationService.setMigratedUserEnabledStatus(username, false);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @PUT
    @Path("cloud/users/{username}/disable")
    public Response disableMigratedUserByUsername(@PathParam("username") String username) throws IOException, JAXBException {
        cloudMigrationService.setMigratedUserEnabledStatus(username, true);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("cloud/users/{username}/unmigrate")
    public Response unmigrateCloudUserByUsername(@PathParam("username") String username) {
        cloudMigrationService.unmigrateUserByUsername(username);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("cloud/users/{username}")
    public Response getMigratedCloudUserByUsername(@PathParam("username") String username) {
        return cloudMigrationService.getMigratedUser(username).build();
    }

    @GET
    @Path("cloud/users/{username}/roles")
    public Response getMigratedCloudUserRolesByUsername(@PathParam("username") String username) {
        return cloudMigrationService.getMigratedUserRoles(username).build();
    }

    @GET
    @Path("cloud/users/{username}/endpoints")
    public Response getMigratedCloudUserEndpointsByUsername(@PathParam("username") String username) {
        return cloudMigrationService.getMigratedUserEndpoints(username).build();
    }


    @POST
    @Path("cloud/baseurls")
    public Response migrateBaseURLs() {
        cloudMigrationService.migrateBaseURLs();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("cloud/roles")
    public Response migrateRoles() {
        cloudMigrationService.migrateRoles();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("cloud/groups")
    public Response migrateGroups() {
        cloudMigrationService.migrateGroups();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("cloud/groups")
    public Response getGroups() {
        return cloudMigrationService.getGroups().build();
    }

    public void setCloudMigrationService(CloudMigrationService cloudMigrationService) {
        this.cloudMigrationService = cloudMigrationService;
    }

}
