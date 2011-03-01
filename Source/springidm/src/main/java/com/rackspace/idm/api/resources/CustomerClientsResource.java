package com.rackspace.idm.api.resources;

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
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
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
public class CustomerClientsResource {

    private AccessTokenService accessTokenService;
    private CustomerService customerService;
    private InputValidator inputValidator;
    private ClientConverter clientConverter;
    private ClientService clientService;
    private CustomerClientResource customerClientResource;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientsResource(AccessTokenService accessTokenService,
        CustomerService customerService, InputValidator inputValidator,
        ClientConverter clientConverter, ClientService clientService,
        CustomerClientResource customerClientResource,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.customerService = customerService;
        this.clientService = clientService;
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
    public Response getClients(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

        logger.debug("Getting Customer Clients: {}", customerId);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

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
    public Response addClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.Client client) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

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

        client = clientConverter
            .toClientJaxbWithPermissionsAndCredentials(clientDO);

        String location = uriInfo.getPath() + clientDO.getClientId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Client Location URI error");
        }

        return Response.ok(client).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{clientId}")
    public CustomerClientResource getCustomerClientResource() {
        return customerClientResource;
    }
}
