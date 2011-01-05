package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;

/**
 * Client application resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientResource {

    private ClientConverter clientConverter;
    private ClientService clientService;
    private PermissionsResource permissionsResource;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public ClientResource(ClientService clientService,
        ClientConverter clientConverter,
        PermissionsResource permissionsResource,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.clientService = clientService;
        this.clientConverter = clientConverter;
        this.permissionsResource = permissionsResource;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets the client data.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
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
    public Response getClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {
        logger.debug("Getting Client: {}", clientId);

        // Racker's, Rackspace Clients, Specific Clients, Admins and Users are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeRackspaceClient(authHeader)
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

        if (client == null
            || !client.getCustomerId().toLowerCase()
                .equals(customerId.toLowerCase())) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Got Client: {}", client);

        com.rackspace.idm.jaxb.Client returnedClient = clientConverter
            .toClientJaxbWithPermissions(client);

        return Response.ok(returnedClient).build();
    }

    @Path("permissions")
    public PermissionsResource getPermissionsResource() {
        return permissionsResource;
    }
}
