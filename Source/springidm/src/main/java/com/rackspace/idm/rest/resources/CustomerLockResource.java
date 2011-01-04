package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.CustomerService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerLockResource {

    private CustomerService customerService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public CustomerLockResource(CustomerService customerService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.customerService = customerService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response setCustomerLockStatus(@Context Request request,
        @Context UriInfo uriInfo, @PathParam("customerId") String customerId,
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.Customer inputCustomer) {

        logger.debug("Getting Customer: {}", customerId);

        // Racker's and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (inputCustomer.isLocked() == null) {
            String errMsg = "Blank value for locked passed in.";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        boolean locked = inputCustomer.isLocked();
        this.customerService.setCustomerLocked(customer, locked);
        logger.debug("Successfully locked customer: {}", customer);

        return Response.ok(inputCustomer).build();
    }
}
