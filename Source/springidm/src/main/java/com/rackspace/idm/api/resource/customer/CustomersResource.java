package com.rackspace.idm.api.resource.customer;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Rackspace Customers.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomersResource {
    
    private final CustomerResource customerResource;
    private final CustomerService customerService;
    private final ScopeAccessService scopeAccessService;
    private final InputValidator inputValidator;
    private final CustomerConverter customerConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomersResource(CustomerResource customerResource, CustomerService customerService, 
        ScopeAccessService scopeAccessService, InputValidator inputValidator, CustomerConverter customerConverter,
        AuthorizationService authorizationService) {
       
        this.customerResource = customerResource;
        this.customerService = customerService;
        this.scopeAccessService = scopeAccessService;
        this.inputValidator = inputValidator;
        this.customerConverter = customerConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Adds a Customer.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param inputCustomer New Customer
     */
    @POST
    public Response addCustomer(@Context UriInfo uriInfo, @Context Request request,
        @HeaderParam("Authorization") String authHeader, EntityHolder<com.rackspace.api.idm.v1.Customer> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.api.idm.v1.Customer inputCustomer = holder.getEntity();
        Customer customer = customerConverter.toCustomerDO(inputCustomer);
        customer.setDefaults();

        ApiError err = inputValidator.validate(customer);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        logger.debug("Adding Customer: {}", customer.getRCN());

        try {
            this.customerService.addCustomer(customer);
        } catch (DuplicateException ex) {
            String errorMsg = String.format("A customer with that customerId already exists: %s",
                customer.getRCN());
            logger.warn(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        logger.debug("Added Customer: {}", customer);

        String location = uriInfo.getPath() + customer.getRCN();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Customer Location URI error");
        }

        return Response.ok(customerConverter.toJaxbCustomer(customer)).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{customerId}")
    public CustomerResource getCustomerResource() {
        // No customerId lookup from here to prevent probing attacks.
        return customerResource;
    }
}
