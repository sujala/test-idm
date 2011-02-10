package com.rackspace.idm.rest.resources;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

/**
 * A customers users.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerUsersResource {

    private AccessTokenService accessTokenService;
    private UserResource userResource;
    private UserService userService;
    private CustomerService customerService;
    private UserConverter userConverter;
    private InputValidator inputValidator;
    private PasswordComplexityService passwordComplexityService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public CustomerUsersResource(AccessTokenService accessTokenService,
        UserResource userResource, UserService userService,
        CustomerService customerService, UserConverter userConverter,
        InputValidator inputValidator,
        PasswordComplexityService passwordComplexityService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.userResource = userResource;
        this.userService = userService;
        this.userConverter = userConverter;
        this.customerService = customerService;
        this.inputValidator = inputValidator;
        this.passwordComplexityService = passwordComplexityService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets a list of users for a customer
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}users
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameConflict
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    public Response getUsers(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

        logger.debug("Getting Customer Users: {}", customerId);

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

        Users users = userService.getByCustomerId(customerId, offset, limit);
        logger.debug("Got Customer Users:{}", users);

        return Response.ok(userConverter.toUserListJaxb(users)).build();
    }

    /**
     * Adds a user to a customer
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param user New User
     */
    @POST
    public Response addUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.User user) {

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
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        user.setCustomerId(customerId);

        User userDO = userConverter.toUserDO(user);
        userDO.setDefaults();

        ApiError err = inputValidator.validate(userDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        logger.info("Adding User: {}", user.getUsername());

        if (userDO.getPasswordObj() == null
            || StringUtils.isBlank(userDO.getPasswordObj().getValue())) {
            Password newpassword = Password.generateRandom();
            userDO.setPasswordObj(newpassword);
        } else {
            String password = userDO.getPasswordObj().getValue();
            if (!passwordComplexityService.checkPassword(password)
                .isValidPassword()) {
                String errorMsg = String
                    .format("Invalid password %s", password);
                logger.warn(errorMsg);
                throw new PasswordValidationException(errorMsg);
            }
        }

        try {
            this.userService.addUser(userDO);
        } catch (IllegalStateException ex) {
            String errorMsg = "User not added because customer doesn't exist.";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        logger.info("Added User: {}", user);

        String location = uriInfo.getPath() + userDO.getUsername();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.error("Customer Location URI error");
        }

        return Response.ok(userConverter.toUserJaxb(userDO)).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{username}")
    public UserResource getUserResource() {
        return userResource;
    }
}
