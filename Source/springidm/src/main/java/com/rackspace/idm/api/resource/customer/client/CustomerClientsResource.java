package com.rackspace.idm.api.resource.customer.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.api.resource.customer.AbstractCustomerConsumer;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Client applications that belong to a customer.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerClientsResource extends AbstractCustomerConsumer {
    
    private final ScopeAccessService scopeAccessService;
    private final InputValidator inputValidator;
    private final ClientConverter clientConverter;
    private final ClientService clientService;
    private final CustomerClientResource customerClientResource;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientsResource(CustomerService customerService, ScopeAccessService scopeAccessService,
        InputValidator inputValidator, ClientConverter clientConverter, ClientService clientService,
        CustomerClientResource customerClientResource, AuthorizationService authorizationService) {
        super(customerService);
   
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.clientConverter = clientConverter;
        this.inputValidator = inputValidator;
        this.customerClientResource = customerClientResource;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a list of client applications for a customer.
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
     */
    @GET
    public Response getClients(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

        logger.debug("Getting Customer Clients: {}", customerId);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetCustomer(customerId);

        Clients clients = clientService.getByCustomerId(customerId, offset, limit);

        logger.debug("Got Customer Clients:{}", clients);

        return Response.ok(clientConverter.toClientListJaxb(clients)).build();
    }

    /**
     * Adds a client to the customer.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customerIdConflict
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param client New Client.
     */
    @POST
    public Response addClient(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        EntityHolder<com.rackspace.idm.jaxb.Client> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.Client client = holder.getEntity();
        client.setCustomerId(customerId);

        Client clientDO = clientConverter.toClientDO(client);
        clientDO.setDefaults();

        ApiError err = inputValidator.validate(clientDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        try {
            this.clientService.add(clientDO);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.warn(errorMsg);
            throw new ClientConflictException(errorMsg);
        }

        logger.info("Added Client: {}", clientDO);

        client = clientConverter.toClientJaxbWithPermissionsAndCredentials(clientDO);

        String location = uriInfo.getPath() + clientDO.getClientId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Client Location URI error");
        }

        return Response.ok(client).location(uri).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{clientId}")
    public CustomerClientResource getCustomerClientResource() {
        return customerClientResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
