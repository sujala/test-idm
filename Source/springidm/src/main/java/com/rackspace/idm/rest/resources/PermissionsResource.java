package com.rackspace.idm.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.PermissionSet;
import com.rackspace.idm.services.ClientService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class PermissionsResource {

    private DefinedPermissionsResource definedPermissionsResource;
    private GrantedPermissionsResource grantedPermissionsResource;
    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private Logger logger;

    @Autowired
    public PermissionsResource(
        DefinedPermissionsResource definedPermissionsResource,
        GrantedPermissionsResource grantedPermissionsResource,
        ClientService clientService, PermissionConverter permissionConverter,
        LoggerFactoryWrapper logger) {
        this.definedPermissionsResource = definedPermissionsResource;
        this.grantedPermissionsResource = grantedPermissionsResource;
        this.permissionConverter = permissionConverter;
        this.clientService = clientService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permissions
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getClientPermissions(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        logger.debug(String.format("Getting Permissions for client %s",
            clientId));
        List<Permission> defineds = this.clientService
            .getDefinedPermissionsByClientId(clientId);

        PermissionSet permset = new PermissionSet();

        permset.setDefineds(defineds);
        logger.debug(String.format("Got Permissions for client %s", clientId));
        return Response.ok(permissionConverter.toPermissionsJaxb(permset)).build();
    }

    @Path("defined")
    public DefinedPermissionsResource getDefinedPermissionsResource() {
        return definedPermissionsResource;
    }

    @Path("granted")
    public GrantedPermissionsResource getGrantedPermissionsResource() {
        return grantedPermissionsResource;
    }
}
