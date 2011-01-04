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

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.oauth.OAuthService;
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
    private IDMAuthorizationHelper authorizationHelper;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public ApiKeyResource(UserService userService,
        IDMAuthorizationHelper authorizationHelper, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.authorizationHelper = authorizationHelper;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an user API key.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userApiKey
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param customerId RCN
     * @param username
     * @return The response with an userApiKey representation.
     */
    @GET
    public Response getApiKey(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.debug("Reseting Cloud Auth service API key for User: {}",
            username);

        // FIXME: We need to discuss who should be authorized to get an apiKey
        // get user to update
        User user = checkAndGetUser(customerId, username);
        if (!checkUserOrAdminAuthorization(authHeader, user, "resetApiKey")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        logger.debug("Retrieved Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(user.getApiKey());

        return Response.ok(userApiKey).build();
    }

    /**
     * Resets the user API key.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userApiKey
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param customerId RCN
     * @param username
     * @return The response with a new userApiKey representation.
     */
    @POST
    public Response resetApiKey(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.info("Reseting Cloud Auth service API key for User: {}",
            username);

        // get user to update
        User user = checkAndGetUser(customerId, username);
        if (!checkUserOrAdminAuthorization(authHeader, user, "resetApiKey")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

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

    private boolean checkUserOrAdminAuthorization(String authHeader, User user,
        String methodName) {

        String userName = user.getUsername();
        String companyId = user.getCustomerId();

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            return false;
        }

        // Condition 1: User can do stuff.
        if (!authorizationHelper.checkUserAuthorization(subjectUsername,
            userName, methodName)) {

            // Condition 2: An Admin can do stuff.
            if (!authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, companyId, methodName)) {
                return false;
            }
        }
        return true;
    }
}
