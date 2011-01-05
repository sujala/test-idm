package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserSecret;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;

/**
 * A users secret question and answer
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserSecretResource {

    private UserService userService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public UserSecretResource(UserService userService,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets a users secret question and answer
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     */
    @GET
    public Response getUserSecret(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.info("Getting Secret Q&A for User: {}", username);

        // Only Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService
            .authorizeRackspaceClient(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        // get user to update
        User user = checkAndGetUser(customerId, username);

        com.rackspace.idm.jaxb.UserSecret secret = new com.rackspace.idm.jaxb.UserSecret();
        secret.setSecretAnswer(user.getSecretAnswer());
        secret.setSecretQuestion(user.getSecretQuestion());

        logger.info("Got Secret Q&A for user: {}", user);

        return Response.ok(secret).build();
    }

    /**
     * Sets a users secret question and answer
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     */
    @PUT
    public Response setUserSecret(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.UserSecret userSecret) {

        validateUserSecretParam(userSecret);

        logger.info("Updating Secret Q&A for User: {}", username);

        // Racker's and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
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

        user.setSecretQuestion(userSecret.getSecretQuestion());
        user.setSecretAnswer(userSecret.getSecretAnswer());
        this.userService.updateUser(user);

        logger.info("Updated Secret Q&A for user: {}", user);

        return Response.ok(userSecret).build();
    }

    private void validateUserSecretParam(UserSecret userSecret) {

        if (StringUtils.isBlank(userSecret.getSecretQuestion())
            || StringUtils.isBlank(userSecret.getSecretAnswer())) {
            String errMsg = "";
            if (StringUtils.isBlank(userSecret.getSecretQuestion())) {
                errMsg = "Secret Question cannot be blank. ";
            }
            if (StringUtils.isBlank(userSecret.getSecretAnswer())) {
                errMsg += "Secret Answer cannot be blank.";
            }
            throw new BadRequestException(errMsg);
        }
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
