package com.rackspace.idm.api.resource.user;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PasswordValidationException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * First user for a customer
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UsersResource {

    private final ScopeAccessService scopeAccessService;
    private final CustomerService customerService;
    private final UserService userService;
    private final InputValidator inputValidator;
    private final UserConverter userConverter;
    private final PasswordComplexityService passwordComplexityService;
    private final AuthorizationService authorizationService;
    private final ClientService clientService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Configuration config;

    @Autowired
    public UsersResource(ScopeAccessService scopeAccessService,
        CustomerService customerService, UserService userService,
        InputValidator inputValidator, UserConverter userConverter,
        PasswordComplexityService passwordComplexityService,
        AuthorizationService authorizationService, ClientService clientService,
        Configuration config) {

        this.scopeAccessService = scopeAccessService;
        this.customerService = customerService;
        this.userService = userService;
        this.inputValidator = inputValidator;
        this.userConverter = userConverter;
        this.passwordComplexityService = passwordComplexityService;
        this.authorizationService = authorizationService;
        this.clientService = clientService;
        this.config = config;
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
        EntityHolder<com.rackspace.api.idm.v1.User> holder) {

        authorizeRequest(request, uriInfo, authHeader, holder);

        com.rackspace.api.idm.v1.User user = holder.getEntity();
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
            logger.warn(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        if (userDO.getPasswordObj() != null
            && !StringUtils.isBlank(userDO.getPasswordObj().getValue())) {
            String password = userDO.getPasswordObj().getValue();
            if (isPasswordRulesEnforced()
                && !passwordComplexityService.checkPassword(password)
                    .isValidPassword()) {
                String errorMsg = String
                    .format("Invalid password %s", password);
                logger.warn(errorMsg);
                throw new PasswordValidationException(errorMsg);
            }
        }

        Customer customer = new Customer();
        customer.setRCN(userDO.getCustomerId());
        customer.setDefaults();

        try {
            this.customerService.addCustomer(customer);
        } catch (DuplicateException ex) {
            String errorMsg = String.format(
                "A customer with customerId '%s' already exists.",
                customer.getRCN());
            logger.warn(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        try {
            this.userService.addUser(userDO);
        } catch (DuplicateException ex) {
            // Roll Back the Add Customer call
            this.customerService.deleteCustomer(customer.getRCN());
            // Then throw the error
            String errorMsg = String.format(
                "A user with username '%s' already exists.",
                userDO.getUsername());
            logger.warn(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        this.clientService.addUserToClientGroup(userDO.getUsername(),
            getRackspaceCustomerId(), getIdmClientId(), getIdmAdminGroupName());

        logger.debug("Added User: {}", userDO);

        String locationUri = String.format("/customers/%s/users/%s",
            customer.getRCN(), user.getUsername());

        user = userConverter.toUserJaxb(userDO);

        URI uri = null;
        try {
            uri = new URI(locationUri);
        } catch (URISyntaxException e) {
            logger.warn("Customer Location URI error");
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
    public Response getUserByUsername(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("username") String username) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers, Rackspace Clients, Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        logger.debug("Getting User: {}", username);

        User user = checkAndGetUser(username);

        logger.debug("Got User :{}", user);
        return Response.ok(
            userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();

    }

    /**
     * Updates mossoId of an user.
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
     * @param username username
     */
    @PUT
    @Path("{username}/mossoid")
    public Response updateUserMossoId(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("username") String username,
        EntityHolder<com.rackspace.api.idm.v1.User> holder) {

        authorizeRequest(request, uriInfo, authHeader, holder);

        User user = checkAndGetUser(username);
        com.rackspace.api.idm.v1.User jaxbUser = holder.getEntity();
        user.setMossoId(jaxbUser.getMossoId());

        this.userService.updateUser(user, false);

        logger.debug("Updated MossoId for User: {}", user);

        return Response.ok(userConverter.toUserWithOnlyMossoId(user)).build();
    }

    /**
     * Updates nastId of an user.
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
     * @param username username
     */
    @PUT
    @Path("{username}/nastid")
    public Response updateUserNastId(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("username") String username,
        EntityHolder<com.rackspace.api.idm.v1.User> holder) {

        authorizeRequest(request, uriInfo, authHeader, holder);

        User user = checkAndGetUser(username);
        com.rackspace.api.idm.v1.User jaxbUser = holder.getEntity();
        user.setNastId(jaxbUser.getNastId());

        this.userService.updateUser(user, false);

        logger.debug("Updated NastID for User: {}", user);

        return Response.ok(userConverter.toUserWithOnlyNastId(user)).build();
    }

    /**
     * Updates RPN of an user.
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
     * @param username username
     */
    @PUT
    @Path("{username}/rpn")
    public Response updateUserRPN(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("username") String username,
        @PathParam("customerId") String customerId,
        EntityHolder<com.rackspace.api.idm.v1.User> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Specific clients are authorized
        boolean authorized = authorizationService.authorizeCustomerIdm(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        User user = checkAndGetUser(username);
        com.rackspace.api.idm.v1.User jaxbUser = holder.getEntity();
        user.setPersonId(jaxbUser.getPersonId());

        this.userService.updateUser(user, false);

        logger.debug("Updated RPN for User: {}", user);

        return Response.ok(userConverter.toUserJaxb(user)).build();
    }

    private User checkAndGetUser(String username) {
        User user = this.userService.getUser(username);

        if (user == null) {
            String errorMsg = String.format("User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return user;
    }

    private void authorizeRequest(Request request, UriInfo uriInfo,
        String authHeader, EntityHolder<com.rackspace.api.idm.v1.User> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Specific clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
    }

    private String getIdmAdminGroupName() {
        return config.getString("idm.AdminGroupName");
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private boolean isPasswordRulesEnforced() {
        return config.getBoolean("password.rules.enforced", true);
    }
}
