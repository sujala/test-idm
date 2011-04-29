package com.rackspace.idm.api.resource.customer.client.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotFoundException;

/**
 * A clients services
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerClientServicesResource {
    private final CustomerClientServiceResource customerClientServiceResource;
    private final ScopeAccessService scopeAccessService;
    private final ClientConverter clientConverter;
    private final ClientService clientService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientServicesResource(
        CustomerClientServiceResource customerClientServiceResource,
        CustomerService customerService, ScopeAccessService scopeAccessService,
        ClientConverter clientConverter, ClientService clientService,
        AuthorizationService authorizationService) {

        this.customerClientServiceResource = customerClientServiceResource;
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.clientConverter = clientConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets the services a client has.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}clients
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client Id
     */
    @POST
    public Response getServicesForClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        logger.debug("Getting services for client {}", clientId);

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo) || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Client client = this.clientService.getClient(customerId, clientId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", client);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        Clients services = this.clientService.getClientServices(client);

        logger.debug("Got services for client {} - {}", clientId, services);

        return Response.ok(clientConverter.toClientListJaxb(services)).build();
    }

    @Path("services")
    public CustomerClientServiceResource getCustomerClientServiceResource() {
        return customerClientServiceResource;
    }
}
