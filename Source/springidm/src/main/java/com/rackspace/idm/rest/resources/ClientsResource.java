package com.rackspace.idm.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientConflictException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.validation.InputValidator;

/**
 * Client applications that belong to a customer.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientsResource {

    private CustomerService customerService;
    private InputValidator inputValidator;
    private ClientConverter clientConverter;
    private ClientService clientService;
    private ClientResource clientResource;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public ClientsResource(CustomerService customerService,
        InputValidator inputValidator, ClientConverter clientConverter,
        ClientService clientService, ClientResource clientResource,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.customerService = customerService;
        this.clientService = clientService;
        this.clientConverter = clientConverter;
        this.inputValidator = inputValidator;
        this.clientResource = clientResource;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
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
    public Response getClients(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer Clients: {}", customerId);

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

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        List<Client> clients = clientService.getByCustomerId(customerId);

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
    public Response addClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.Client client) {

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

        if (!client.getCustomerId().toLowerCase()
            .equals(customerId.toLowerCase())) {
            String errorMsg = String.format(
                "Client's customerId (%s) does not match customerId (%s)",
                client.getCustomerId(), customerId);
            logger.error(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

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
            logger.error(errorMsg);
            throw new ClientConflictException(errorMsg);
        }

        logger.info("Added Client: {}", clientDO);

        client = clientConverter
            .toClientJaxbWithPermissionsAndCredentials(clientDO);

        String location = uriInfo.getPath() + clientDO.getClientId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.error("Client Location URI error");
        }

        return Response.ok(client).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{clientId}")
    public ClientResource getClientResource() {
        return clientResource;
    }
}
