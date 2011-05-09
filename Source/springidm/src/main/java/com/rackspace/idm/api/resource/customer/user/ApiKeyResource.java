package com.rackspace.idm.api.resource.customer.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.UserApiKey;
import com.sun.jersey.core.provider.EntityHolder;

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

    private final ScopeAccessService scopeAccessService;
  
    private final UserService userService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ApiKeyResource(ScopeAccessService scopeAccessService, UserService userService,
        AuthorizationService authorizationService) {
      
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.authorizationService = authorizationService;
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
    public Response getApiKey(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.debug("Reseting Cloud Auth service API key for User: {}", username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Specific Clients and Users are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo)
            || authorizationService.authorizeUser(token, customerId, username);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
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
    public Response resetApiKey(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Reseting Cloud Auth service API key for User: {}", username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients, Admins and Users are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        // generate random api key
        String apiKey = userService.generateApiKey();
        user.setApiKey(apiKey);
        this.userService.updateUser(user, false);

        logger.debug("Reset Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(apiKey);

        return Response.ok(userApiKey).build();
    }

    /**
     * Set a user's API key.
     *
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userApiKey
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
     * @param userApiKey
     */
    @PUT
    public Response setApiKey(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, EntityHolder<com.rackspace.idm.jaxb.UserApiKey> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        logger.debug("Reseting Cloud Auth service API key for User: {}", username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Rackers and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        // get user to update
        User user = checkAndGetUser(customerId, username);
        UserApiKey userApiKey = holder.getEntity();
        String apiKey = userApiKey.getApiKey();
        user.setApiKey(apiKey);
        this.userService.updateUser(user, false);

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
        String errorMsg = String.format("User not found: %s - %s", customerId, username);
        logger.warn(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
