package com.rackspace.idm.api.resources;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.UserService;

/**
 * Customer lock.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerLockResource {
    private CustomerService customerService;
    private AuthorizationService authorizationService;
    private UserService userService;
    private OAuthService oauthService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerLockResource(CustomerService customerService, AuthorizationService authorizationService,
        UserService userService, OAuthService oauthService) {
        this.customerService = customerService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.oauthService = oauthService;
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
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @PUT
    public Response setCustomerLockStatus(@Context Request request, @Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.Customer inputCustomer) {

        logger.debug("Getting Customer: {}", customerId);

        AccessToken token = oauthService.getAccessTokenByAuthHeader(authHeader);

        // Racker's and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (inputCustomer.isLocked() == null) {
            String errMsg = "Blank value for locked passed in.";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s", customerId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        boolean isLocked = inputCustomer.isLocked();
        this.customerService.setCustomerLocked(customer, isLocked);
        logger.debug("Successfully locked customer: {}", customer);

        logger.debug("Revoking all user tokens for customer {}", customer.getCustomerId());

        if (isLocked) {
            oauthService.revokeTokensGloballyForCustomer(customerId);
        }

        return Response.ok(inputCustomer).build();
    }
}
