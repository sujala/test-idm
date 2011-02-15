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
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
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

    private AccessTokenService accessTokenService;
    private CustomerService customerService;
    private AuthorizationService authorizationService;
    private UserService userService;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public CustomerLockResource(CustomerService customerService,
        AuthorizationService authorizationService, UserService userService, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.customerService = customerService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    @Deprecated
    public CustomerLockResource(AccessTokenService accessTokenService, CustomerService customerService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
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
            String errorMsg = String.format("Customer not found: %s", customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        boolean locked = inputCustomer.isLocked();
        this.customerService.setCustomerLocked(customer, locked);
        logger.debug("Successfully locked customer: {}", customer);

        logger.debug("Revoking all user tokens for customer {}", customer.getCustomerId());

        // TODO What is the right limit for this?
        for (User user : userService.getByCustomerId(customerId, 0, -1).getUsers()) {
            oauthService.revokeTokensGloballyForOwner(token.getTokenString(), user.getUsername());
        }

        return Response.ok(inputCustomer).build();
    }
}
