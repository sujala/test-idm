package com.rackspace.idm.api.resource.user;

import java.net.URI;
import java.net.URISyntaxException;
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

import com.rackspace.idm.api.converter.EndPointConverter;
import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PasswordValidationException;
import com.rackspace.idm.jaxb.BaseURLRef;
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
    private InputValidator inputValidator;
    private UserConverter userConverter;
    private PasswordComplexityService passwordComplexityService;
    private AuthorizationService authorizationService;
    private EndpointService endpointService;
    private EndPointConverter endpointConverter;
    private ClientService clientService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Configuration config;

    @Autowired
    public UsersResource(AccessTokenService accessTokenService, CustomerService customerService,
        UserService userService, InputValidator inputValidator, UserConverter userConverter,
        PasswordComplexityService passwordComplexityService, AuthorizationService authorizationService,
        EndpointService endpointService, EndPointConverter endpointConverter, ClientService clientService,
        Configuration config) {
        this.accessTokenService = accessTokenService;
        this.customerService = customerService;
        this.userService = userService;
        this.inputValidator = inputValidator;
        this.userConverter = userConverter;
        this.passwordComplexityService = passwordComplexityService;
        this.authorizationService = authorizationService;
        this.endpointService = endpointService;
        this.endpointConverter = endpointConverter;
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
    public Response addFirstUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, com.rackspace.idm.jaxb.User user) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User userDO = userConverter.toUserDO(user);
        userDO.setDefaults();

        ApiError err = inputValidator.validate(userDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        if (!this.userService.isUsernameUnique(userDO.getUsername())) {
            String errorMsg = String
                .format("A user with username '%s' already exists.", userDO.getUsername());
            logger.warn(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        if (userDO.getPasswordObj() != null && !StringUtils.isBlank(userDO.getPasswordObj().getValue())) {
            String password = userDO.getPasswordObj().getValue();
            if (!passwordComplexityService.checkPassword(password).isValidPassword()) {
                String errorMsg = String.format("Invalid password %s", password);
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
            String errorMsg = String.format("A customer with customerId '%s' already exists.",
                customer.getCustomerId());
            logger.warn(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        try {
            this.userService.addUser(userDO);
        } catch (DuplicateException ex) {
            // Roll Back the Add Customer call
            this.customerService.deleteCustomer(customer.getCustomerId());
            // Then throw the error
            String errorMsg = String
                .format("A user with username '%s' already exists.", userDO.getUsername());
            logger.warn(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        ClientGroup idmAdmin = this.clientService.getClientGroup(getRackspaceCustomerId(), getIdmClientId(),
            getIdmAdminGroupName());

        this.clientService.addUserToClientGroup(userDO.getUsername(), idmAdmin);

        // Add the new Admin role to the User Object
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(idmAdmin);
        userDO.setGroups(groups);

        logger.debug("Added User: {}", userDO);

        String locationUri = String.format("/customers/%s/users/%s", customer.getCustomerId(),
            user.getUsername());

        user = userConverter.toUserJaxb(userDO);

        URI uri = null;
        try {
            uri = new URI(locationUri);
        } catch (URISyntaxException e) {
            logger.warn("Customer Location URI error");
        }

        return Response.ok(user).location(uri).status(HttpServletResponse.SC_CREATED).build();
    }

    /**
     * Gets a list of serviceCatalog for a user.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceCatalog
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     */
    @GET
    @Path("{username}/servicecatalog")
    public Response getServiceCatalog(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(username);

        return Response.ok(this.endpointConverter.toServiceCatalog(endpoints)).build();
    }

    /**
     * Gets a list of baseUrlRefs for a user.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}baseURLRefs
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     */
    @GET
    @Path("{username}/baseurlrefs")
    public Response getBaseUrlRefs(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(username);

        return Response.ok(this.endpointConverter.toBaseUrlRefs(endpoints)).build();
    }

    /**
     * Adds a baseUrl to a user.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}baseUrlRef
     * @response.representation.201.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     * @param baseUrlRef baseUrlRef
     */
    @PUT
    @Path("{username}/baseurlrefs")
    public Response addBaseUrlRef(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username,
        BaseURLRef baseUrlRef) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        this.endpointService.addBaseUrlToUser(baseUrlRef.getId(), baseUrlRef.isV1Default(), username);

        return Response.ok().status(HttpServletResponse.SC_CREATED).build();
    }

    /**
     * Gets a baseUrlRef for a user.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}baseURLRef
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     * @param baseUrlId baseUrlId
     */
    @GET
    @Path("{username}/baseurlrefs/{baseUrlId}")
    public Response getBaseUrlRef(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username,
        @PathParam("baseUrlId") int baseUrlId) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        CloudEndpoint endpoint = this.endpointService.getEndpointForUser(username, baseUrlId);

        if (endpoint == null) {
            String errMsg = String.format("BaseUrlId %s not found for user %s", baseUrlId, username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return Response.ok(this.endpointConverter.toBaseUrlRef(endpoint)).build();
    }

    /**
     * Removes a baseUrl from a user.
     * 
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username username
     * @param baseUrlId baseUrlId
     */
    @DELETE
    @Path("{username}/baseurlrefs/{baseUrlId}")
    public Response deleteBaseUrlRef(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username,
        @PathParam("baseUrlId") int baseUrlId) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        this.endpointService.removeBaseUrlFromUser(baseUrlId, username);

        return Response.noContent().build();
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
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Rackers, Rackspace Clients, Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        logger.debug("Getting User: {}", username);

        User user = checkAndGetUser(username);

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();

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
    @Path("{username}/mossoId")
    public Response updateUserMossoId(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username,
        com.rackspace.idm.jaxb.User jaxbUser) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Specific clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(username);

        user.setMossoId(jaxbUser.getMossoId());

        this.userService.updateUser(user);

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
    @Path("{username}/nastId")
    public Response updateUserNastId(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("username") String username,
        com.rackspace.idm.jaxb.User jaxbUser) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Specific clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(username);

        user.setNastId(jaxbUser.getNastId());

        this.userService.updateUser(user);

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
        @PathParam("username") String username,@PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.User jaxbUser) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Specific clients are authorized
        boolean authorized = authorizationService.authorizeRackspaceClient(token);
   
        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
        
        User user = checkAndGetUser(username);

        user.setPersonId(jaxbUser.getPersonId());

        this.userService.updateUser(user);

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

    private String getIdmAdminGroupName() {
        return config.getString("idm.AdminGroupName");
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }
}
