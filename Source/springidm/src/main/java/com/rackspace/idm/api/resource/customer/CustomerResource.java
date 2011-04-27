package com.rackspace.idm.api.resource.customer;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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

import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.resource.customer.client.CustomerClientsResource;
import com.rackspace.idm.api.resource.customer.user.CustomerUsersResource;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.jaxb.PasswordRotationPolicy;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A Rackspace Customer.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerResource extends AbstractCustomerConsumer {

    private final CustomerClientsResource customerClientsResource;
    private final CustomerLockResource customerLockResource;
    private final CustomerUsersResource customerUsersResource;
    private final CustomerService customerService;
    private final ScopeAccessService scopeAccessService;
    private final CustomerConverter customerConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerResource(CustomerClientsResource customerClientsResource, CustomerLockResource customerLockResource,
        CustomerUsersResource customerUsersResource,
        CustomerService customerService, ScopeAccessService scopeAccessService, CustomerConverter customerConverter,
        AuthorizationService authorizationService) {
        super(customerService);
        
        this.customerClientsResource = customerClientsResource;
        this.customerLockResource = customerLockResource;
        this.customerUsersResource = customerUsersResource;
        this.customerService = customerService;
        this.scopeAccessService = scopeAccessService;
        this.customerConverter = customerConverter;
        this.authorizationService = authorizationService;
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
    public Response getCustomer(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer: {}", customerId);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Customer customer = checkAndGetCustomer(customerId);

        com.rackspace.idm.jaxb.Customer outputCustomer = customerConverter.toJaxbCustomer(customer);

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
    public Response deleteCustomer(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId) {

        logger.info("Deleting Customer :{}", customerId);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetCustomer(customerId);

        this.customerService.deleteCustomer(customerId);
        logger.debug("Deleted Customer: {}", customerId);

        return Response.noContent().build();
    }

    /**
     * Updates customer's password rotation policy resource
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
    @PUT
    @Path("passwordRotationPolicy")
    public Response updatePasswordRotationCustomer(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        EntityHolder<com.rackspace.idm.jaxb.PasswordRotationPolicy> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        logger.debug("Updating Customer's Password Rotation Policy: {}", customerId);
        
        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's and Rackspace Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        PasswordRotationPolicy passwordRotationPolicy = holder.getEntity();
        int duration = passwordRotationPolicy.getDuration();
        boolean enabled = passwordRotationPolicy.isEnabled();

        if (enabled) {
            if (duration < 0) {
                String errorMsg = String.format("Password rotation duration cannot be negative.");
                logger.warn(errorMsg);
                throw new BadRequestException(errorMsg);
            }
        }

        Customer customer = checkAndGetCustomer(customerId);
        customer.setPasswordRotationEnabled(enabled);
        customer.setPasswordRotationDuration(duration);

        this.customerService.updateCustomer(customer);

        logger.debug("Updated password rotation policy for customer {}", customerId);
        return Response.ok(passwordRotationPolicy).build();
    }

    @Path("actions/lock")
    public CustomerLockResource getCustomerLockResource() {
        return customerLockResource;
    }

    @Path("clients")
    public CustomerClientsResource getCustomerClientsResource() {
        return customerClientsResource;
    }

    @Path("users")
    public CustomerUsersResource getCustomerUsersResource() {
        return customerUsersResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
