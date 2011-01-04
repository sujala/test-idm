package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.PermissionSet;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;

/**
 * Client granted permissions
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class GrantedPermissionsResource {

    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public GrantedPermissionsResource(ClientService clientService,
        PermissionConverter permissionConverter,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.permissionConverter = permissionConverter;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets a list of Client granted permissions.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permissions
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client application ID
     */
    @GET
    public Response getClientGrantedPermissions(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {
        
        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }
        
        Client client = this.clientService.getById(clientId);
        
        if (client == null || !client.getCustomerId().equals(customerId)) {
            String errMsg = String.format("Client With Client Id %SNot Found", clientId);
            logger.error(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        if (client.getPermissions() == null || client.getPermissions().size() <1) {
            Response.noContent().build();
        }
        
        PermissionSet perms = new PermissionSet();
        perms.setGranteds(client.getPermissions());

        return Response.ok(permissionConverter.toPermissionsJaxb(perms)).build();
    }
}
