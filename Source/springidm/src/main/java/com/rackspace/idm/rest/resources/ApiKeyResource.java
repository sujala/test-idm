package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;

/**
 * Support for user-level API that the Cloud Auth service uses.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ApiKeyResource {
    @Context
    HttpHeaders httpHeaders;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    private UserService userService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public ApiKeyResource(UserService userService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an user's API key.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userApiKey
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username
     */
    @GET
    public Response getApiKey(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.debug("Reseting Cloud Auth service API key for User: {}",
            username);

        // Racker's, Rackspace Clients, Specific Clients, Admins and Users are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeRackspaceClient(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId)
            || authorizationService.authorizeUser(authHeader, customerId,
                username);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        // get user to update
        User user = checkAndGetUser(customerId, username);

        logger.debug("Retrieved Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(user.getApiKey());

        return Response.ok(userApiKey).build();
    }

    /**
     * Resets a user's API key.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userApiKey
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username
     */
    @POST
    public Response resetApiKey(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.info("Reseting Cloud Auth service API key for User: {}",
            username);

        // Racker's, Specific Clients, Admins and Users are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId)
            || authorizationService.authorizeUser(authHeader, customerId,
                username);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        // get user to update
        User user = checkAndGetUser(customerId, username);

        // generate random api key
        String apiKey = userService.generateApiKey();
        user.setApiKey(apiKey);
        this.userService.updateUser(user);

        logger.info("Reset Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(apiKey);

        return Response.ok(userApiKey).build();
    }

    private User checkAndGetUser(String customerId, String username) {
        User user = this.userService.getUser(customerId, username);
        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }
        return user;
    }

    private void handleUserNotFoundError(String customerId, String username) {
        String errorMsg = String.format("User not found: %s - %s", customerId,
            username);
        logger.error(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
