package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.UserService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserStatusResource {

    private UserService userService;
    private UserConverter userConverter;
    private IDMAuthorizationHelper authorizationHelper;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public UserStatusResource(UserService userService,
        UserConverter userConverter,
        IDMAuthorizationHelper authorizationHelper, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationHelper = authorizationHelper;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @request.representation.qname 
     *                               {http://docs.rackspacecloud.com/idm/api/v1.0
     *                               }user
     * @response.representation.200.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}user
     * @response.representation.400.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}badRequest
     * @response.representation.401.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}unauthorized
     * @response.representation.403.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}forbidden
     * @response.representation.404.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}itemNotFound
     * @response.representation.500.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}serverError
     * @response.representation.503.qname 
     *                                    {http://docs.rackspacecloud.com/idm/api
     *                                    /v1.0}serviceUnavailable
     */
    @PUT
    @Path("{username}/status")
    public Response setUserStatus(@HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Updating Status for User: {}", username);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!authorizeSetUserStatus(authHeader, user.getCustomerId(),
            "setUserStatus")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        String statusStr;
        try {
            statusStr = inputUser.getStatus().value();
        } catch (Exception ex) {
            String errMsg = "Invalid value for Status sent in.";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        try {
            this.userService.updateUserStatus(user, statusStr);
        } catch (IllegalArgumentException ex) {
            String errorMsg = String.format("Invalid status value: %s",
                statusStr);
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        logger.info("Updated status for user: {}", user);

        User outputUser = this.userService.getUser(customerId, username);
        return Response.ok(userConverter.toUserWithOnlyStatusJaxb(outputUser))
            .build();
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

    private boolean authorizeSetUserStatus(String authHeader,
        String userCompanyId, String methodName) {

        // Admin can change user status, Or if there is no admin then a
        // company's client can change user status.
        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);
        if (subjectUsername == null) {
            // Condition 1: User's company can set user's status.
            return authorizationHelper.checkCompanyAuthorization(authHeader,
                userCompanyId, methodName);
        } else {
            // Condition 2: Admin can set user's status.
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, userCompanyId, methodName);
        }
    }
}
