package com.rackspace.idm.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.ApiException;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

/**
 * Customers resource
 */
@Path("")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@NoCache
@Component
public class CustomerController {

    private OAuthService oauthService;
    private CustomerService customerService;
    private UserService userService;
    private RoleService roleService;
    private ClientService clientService;
    private IDMAuthorizationHelper authorizationHelper;
    private InputValidator inputValidator;
    private ClientConverter clientConverter;
    private UserConverter userConverter;
    private CustomerConverter customerConverter;
    private RoleConverter roleConverter;
    private PasswordComplexityService passwordComplexityService;
    private Logger logger;

    @Autowired
    public CustomerController(OAuthService oauthService,
        CustomerService customerService, UserService userService,
        RoleService roleService, ClientService clientService,
        IDMAuthorizationHelper idmAuthHelper, InputValidator inputValidator,
        ClientConverter clientConverter, UserConverter userConverter,
        CustomerConverter customerConverter, RoleConverter roleConverter,
        PasswordComplexityService passwordComplexityService,
        LoggerFactoryWrapper logger) {

        this.oauthService = oauthService;
        this.customerService = customerService;
        this.userService = userService;
        this.roleService = roleService;
        this.clientService = clientService;
        this.authorizationHelper = idmAuthHelper;
        this.inputValidator = inputValidator;
        this.clientConverter = clientConverter;
        this.userConverter = userConverter;
        this.customerConverter = customerConverter;
        this.roleConverter = roleConverter;
        this.passwordComplexityService = passwordComplexityService;
        this.logger = logger.getLogger(CustomerController.class);
    }

    @POST
    @Path("/users")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User addFirstUser(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.User user) {

        User userDO = userConverter.toUserDO(user);
        userDO.setDefaults();

        ApiError err = inputValidator.validate(userDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        if (!this.userService.isUsernameUnique(userDO.getUsername())) {
            String errorMsg = String.format(
                "A user with username '%s' already exists.", userDO
                    .getUsername());
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
                "A customer with customerId '%s' already exists.", customer
                    .getCustomerId());
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
                "A user with username '%s' already exists.", userDO
                    .getUsername());
            logger.error(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        Role role = this.roleService.getRole("Admin", userDO.getCustomerId());
        this.roleService.addUserToRole(userDO, role);

        // Add the new Admin role to the User Object
        List<Role> roles = new ArrayList<Role>();
        roles.add(role);
        userDO.setRoles(roles);

        logger.info("Added User: {}", userDO);

        String locationUri = String.format("/customers/%s/users/%s", customer
            .getCustomerId(), user.getUsername());
        response.setHeader("Location", locationUri);
        response.setStatus(HttpServletResponse.SC_CREATED);

        user = userConverter.toUserJaxb(userDO);
        return user;
    }

    /**
     * Add a customer.
     * 
     * @RequestHeader Authorization Authorization header, For Example - Token
     *                token="XXXX"
     * 
     * @param customer
     *            Customer representation
     * @return Newly created Customer representation
     * 
     * @HTTP 200 If customer is added
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @ResponseHeader Location URI of the newly added customer.
     */
    @POST
    @Path("/customers")
    public com.rackspace.idm.jaxb.Customer addCustomer(
        @Context HttpServletResponse response,
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
                "A customer with that customerId already exists: %s", customer
                    .getCustomerId());
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                ErrorMsg.BAD_REQUEST, errorMsg);
        }

        String locationUri = String.format("/customers/%s", customer
            .getCustomerId());
        response.setHeader("Location", locationUri);

        logger.info("Added Customer: {}", customer);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return inputCustomer;
    }

    /**
     * Customer resource.
     * 
     * Single customer and its attributes.
     * 
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @param customerId
     *            The customerId of the customer to retrieve
     * @return A customer resource, if it exists
     * 
     * @HTTP 200 If an existing customer is found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @GET
    @Path("/customers/{customerId}")
    public com.rackspace.idm.jaxb.Customer getCustomer(
        @Context HttpServletResponse response,
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
        return outputCustomer;
    }

    /**
     * Delete a customer.
     * 
     * @RequestHeader Authorization Authorization header, For Example - Token
     *                token="XXXX"
     * 
     * @param customerId
     *            The customerId of the customer to delete
     * 
     * @HTTP 204 If the customer is deleted
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @DELETE
    @Path("/customers/{customerId}")
    public void deleteCustomer(@Context HttpServletResponse response,
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
    }

    /**
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @param customerId
     *            The customerId of the customer to retrieve
     * @return A List of Users
     * 
     * @HTTP 200 If an existing customer users are found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @GET
    @Path("/customers/{customerId}/users")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Users getUsers(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

        logger.debug("Getting Customer Users: {}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (!checkAdminAuthorization(authHeader, customerId, "getUsers")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        Users users = userService.getByCustomerId(customerId, offset, limit);
        logger.debug("Got Customer Users:{}", users);

        return userConverter.toUserListJaxb(users);
    }

    /**
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @param customerId
     *            The customerId of the customer to retrieve
     * @return A List of Users
     * 
     * @HTTP 200 If an existing customer users are found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @GET
    @Path("/customers/{customerId}/roles")
    @Wrapped(element = "roles")
    public com.rackspace.idm.jaxb.Roles getRoles(
        @Context HttpServletResponse response,
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer Roles: {}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        List<Role> roles = roleService.getByCustomerId(customerId);

        com.rackspace.idm.jaxb.Roles outputRoles = roleConverter
            .toRolesJaxb(roles);

        logger.debug("Got Customer Roles:{}", roles);
        return outputRoles;
    }

    /**
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @param customerId
     *            The customerId of the customer to retrieve
     * @return A List of Users
     * 
     * @HTTP 200 If an existing customer users are found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @GET
    @Path("/customers/{customerId}/clients")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Clients getClients(
        @Context HttpServletResponse response,
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer Clients: {}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        List<Client> clients = clientService.getByCustomerId(customerId);

        logger.debug("Got Customer Clients:{}", clients);

        return clientConverter.toClientListJaxb(clients);
    }

    /**
     * Lock or unlock all users in a customer
     * 
     * @RequestHeader Authorization Authorization header, For Example - Token
     *                token="XXXX"
     * 
     * @param customerId
     *            The customerId of the customer to lock
     * 
     * @HTTP 204 If the customer is locked or unlocked successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If customer is not found
     */
    @PUT
    @Path("/customers/{customerId}/actions/lock")
    public com.rackspace.idm.jaxb.Customer setCustomerLockStatus(
        @Context HttpServletResponse response,
        @PathParam("customerId") String customerId,
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.Customer inputCustomer) {

        logger.debug("Getting Customer: {}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (!authorizeCustomerAddDeleteViewLock(authHeader,
            "setCustomerLockStatus")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        boolean locked = inputCustomer.isLocked();
        this.customerService.setCustomerLocked(customer, locked);
        logger.debug("Successfully locked customer: {}", customer);

        return inputCustomer;
    }

    // private functions
    private boolean authorizeCustomerAddDeleteViewLock(String authHeader,
        String methodName) {

        // Condition: Rackspace client can add, view, delete customer.
        return authorizationHelper.checkRackspaceClientAuthorization(
            authHeader, methodName);
    }

    private boolean checkAdminAuthorization(String authHeader,
        String companyId, String methodName) {

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            // Condition 1: RACKSPACE Company can add user.

            String httpMethodName = "GET";
            String requestURI = "/customers/" + companyId + "/users";

            if (!authorizationHelper.checkPermission(authHeader,
                httpMethodName, requestURI)) {
                return authorizationHelper.checkRackspaceClientAuthorization(
                    authHeader, methodName);
            } else {
                return true;
            }

        } else {
            // Condition 2: Admin can add a user.
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, companyId, methodName);
        }
    }
}
