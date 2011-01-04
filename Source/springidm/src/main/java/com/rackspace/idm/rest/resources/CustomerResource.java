package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.CustomerService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerResource {

    private ClientsResource clientsResource;
    private CustomerLockResource customerLockResource;
    private RolesResource rolesResource;
    private UsersResource usersResource;
    private CustomerService customerService;
    private CustomerConverter customerConverter;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    @Autowired
    public CustomerResource(ClientsResource clientsResource,
        CustomerLockResource customerLockResource, RolesResource rolesResource,
        UsersResource usersResource, CustomerService customerService,
        CustomerConverter customerConverter,
        IDMAuthorizationHelper authorizationHelper, LoggerFactoryWrapper logger) {
        this.clientsResource = clientsResource;
        this.customerLockResource = customerLockResource;
        this.rolesResource = rolesResource;
        this.usersResource = usersResource;
        this.customerService = customerService;
        this.customerConverter = customerConverter;
        this.authorizationHelper = authorizationHelper;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getCustomer(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer: {}", customerId);

        if (!authorizeCustomerAddDeleteViewLock(authHeader, "getCustomer")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
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
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    public Response deleteCustomer(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId) {

        logger.info("Deleting Customer :{}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);

        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (!authorizeCustomerAddDeleteViewLock(authHeader, "deleteCustomer")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
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
    public UsersResource getUsersResource() {
        return usersResource;
    }

    private boolean authorizeCustomerAddDeleteViewLock(String authHeader,
        String methodName) {

        // Condition: Rackspace client can add, view, delete customer.
        return authorizationHelper.checkRackspaceClientAuthorization(
            authHeader, methodName);
    }
}
