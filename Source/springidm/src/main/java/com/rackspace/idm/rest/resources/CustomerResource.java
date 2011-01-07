package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.CustomerService;

/**
 * A Rackspace Customer.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerResource {

    private ClientsResource clientsResource;
    private CustomerLockResource customerLockResource;
    private RolesResource rolesResource;
    private CustomerUsersResource customerUsersResource;
    private CustomerService customerService;
    private CustomerConverter customerConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public CustomerResource(ClientsResource clientsResource,
        CustomerLockResource customerLockResource, RolesResource rolesResource,
        CustomerUsersResource customerUsersResource, CustomerService customerService,
        CustomerConverter customerConverter,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.clientsResource = clientsResource;
        this.customerLockResource = customerLockResource;
        this.rolesResource = rolesResource;
        this.customerUsersResource = customerUsersResource;
        this.customerService = customerService;
        this.customerConverter = customerConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets a customer.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
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
    public Response getCustomer(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer: {}", customerId);

        // Racker's, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeRackspaceClient(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath());

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

        com.rackspace.idm.jaxb.Customer outputCustomer = customerConverter
            .toJaxbCustomer(customer);

        logger.debug("Got Customer :{}", customer);
        return Response.ok(outputCustomer).build();
    }

    /**
     * Deletes a customer.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
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
    @DELETE
    public Response deleteCustomer(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId) {

        logger.info("Deleting Customer :{}", customerId);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(authHeader,
            request.getMethod(), uriInfo.getPath());

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

        this.customerService.softDeleteCustomer(customerId);

        logger.info("Deleted Customer: {}", customerId);

        return Response.noContent().build();
    }

    @Path("actions/lock")
    public CustomerLockResource getCustomerLockResource() {
        return customerLockResource;
    }

    @Path("clients")
    public ClientsResource getClientsResource() {
        return clientsResource;
    }

    @Path("roles")
    public RolesResource getRolesResource() {
        return rolesResource;
    }

    @Path("users")
    public CustomerUsersResource getCustomerUsersResource() {
        return customerUsersResource;
    }
}
