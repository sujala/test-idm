package com.rackspace.idm.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

/**
 * First user for a customer
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UsersResource {

    private AccessTokenService accessTokenService;
    private CustomerService customerService;
    private UserService userService;
    private RoleService roleService;
    private InputValidator inputValidator;
    private UserConverter userConverter;
    private PasswordComplexityService passwordComplexityService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public UsersResource(AccessTokenService accessTokenService,
        CustomerService customerService, UserService userService,
        RoleService roleService, InputValidator inputValidator,
        UserConverter userConverter,
        PasswordComplexityService passwordComplexityService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.customerService = customerService;
        this.userService = userService;
        this.roleService = roleService;
        this.inputValidator = inputValidator;
        this.userConverter = userConverter;
        this.passwordComplexityService = passwordComplexityService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Creates customer and adds first user.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customerConflict
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameConflict
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}idmFault
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param user New User
     */
    @POST
    public Response addFirstUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.User user) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User userDO = userConverter.toUserDO(user);
        userDO.setDefaults();

        ApiError err = inputValidator.validate(userDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        if (!this.userService.isUsernameUnique(userDO.getUsername())) {
            String errorMsg = String.format(
                "A user with username '%s' already exists.",
                userDO.getUsername());
            logger.error(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        // If a blank or null password is passed into the method we
        // generate a random password for the user else we check the password
        // against our password complexity rules.
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

        Customer customer = new Customer();
        customer.setCustomerId(userDO.getCustomerId());
        customer.setDefaults();

        try {
            this.customerService.addCustomer(customer);
        } catch (DuplicateException ex) {
            String errorMsg = String.format(
                "A customer with customerId '%s' already exists.",
                customer.getCustomerId());
            logger.error(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        try {
            this.userService.addUser(userDO);
        } catch (DuplicateException ex) {
            // Roll Back the Add Customer call
            this.customerService.deleteCustomer(customer.getCustomerId());
            // Then throw the error
            String errorMsg = String.format(
                "A user with username '%s' already exists.",
                userDO.getUsername());
            logger.error(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        Role role = this.roleService.getRole(
            GlobalConstants.IDM_ADMIN_ROLE_NAME, userDO.getCustomerId());
        this.roleService.addUserToRole(userDO, role);

        // Add the new Admin role to the User Object
        List<Role> roles = new ArrayList<Role>();
        roles.add(role);
        userDO.setRoles(roles);

        logger.info("Added User: {}", userDO);

        String locationUri = String.format("/customers/%s/users/%s",
            customer.getCustomerId(), user.getUsername());

        user = userConverter.toUserJaxb(userDO);

        URI uri = null;
        try {
            uri = new URI(locationUri);
        } catch (URISyntaxException e) {
            logger.error("Customer Location URI error");
        }

        return Response.ok(user).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    /**
     * Gets a user.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     */
    @GET
    @Path("{username}")
    public Response getUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("username") String username) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        logger.debug("Getting User: {}", username);
        User user = this.userService.getUser(username);

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();

    }
}
