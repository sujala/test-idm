package com.rackspace.idm.rest.resources;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.ApiException;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomersResource {

    private CustomerResource customerResource;
    private CustomerService customerService;
    private InputValidator inputValidator;
    private CustomerConverter customerConverter;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    @Autowired
    public CustomersResource(CustomerResource customerResource,
        CustomerService customerService, InputValidator inputValidator,
        CustomerConverter customerConverter,
        IDMAuthorizationHelper authorizationHelper, LoggerFactoryWrapper logger) {
        this.customerResource = customerResource;
        this.customerService = customerService;
        this.inputValidator = inputValidator;
        this.customerConverter = customerConverter;
        this.authorizationHelper = authorizationHelper;
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
    @POST
    public Response addCustomer(@Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.Customer inputCustomer) {

        Customer customer = customerConverter.toCustomerDO(inputCustomer);
        customer.setDefaults();

        ApiError err = inputValidator.validate(customer);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        logger.info("Adding Customer: {}", customer.getCustomerId());

        if (!authorizeCustomerAddDeleteViewLock(authHeader, "addCustomer")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        try {
            this.customerService.addCustomer(customer);
        } catch (DuplicateException ex) {
            String errorMsg = String.format(
                "A customer with that customerId already exists: %s",
                customer.getCustomerId());
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                ErrorMsg.BAD_REQUEST, errorMsg);
        }

        logger.info("Added Customer: {}", customer);

        String location = uriInfo.getPath() + customer.getCustomerId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.error("Customer Location URI error");
        }

        return Response.ok(customerConverter.toJaxbCustomer(customer))
            .location(uri).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{customerId}")
    public CustomerResource getCustomerResource() {
        return customerResource;
    }

    private boolean authorizeCustomerAddDeleteViewLock(String authHeader,
        String methodName) {

        // Condition: Rackspace client can add, view, delete customer.
        return authorizationHelper.checkRackspaceClientAuthorization(
            authHeader, methodName);
    }
}
