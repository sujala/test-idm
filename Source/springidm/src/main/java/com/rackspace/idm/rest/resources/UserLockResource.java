package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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
import com.rackspace.idm.services.UserService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserLockResource {

    private UserService userService;
    private UserConverter userConverter;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    @Autowired
    public UserLockResource(UserService userService,
        UserConverter userConverter,
        IDMAuthorizationHelper authorizationHelper, LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationHelper = authorizationHelper;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response setUserLock(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Locking User: {} - {}", username);

        if (inputUser.isLocked() == null) {
            String errMsg = "Invalid value for locked sent in.";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }
        
        boolean lockStatus = inputUser.isLocked();

        User user = checkAndGetUser(customerId, username);

        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }

        String methodName = "setUserLock";
        String httpMethodName = "PUT";
        String requestURI = "/customers/" + user.getCustomerId() + "/users/"
            + user.getUsername() + "/lock";
        if (!authorizeRackerOrRackspaceApplication(authHeader, user,
            methodName, httpMethodName, requestURI)) {
            authorizationHelper.handleAuthorizationFailure();
        }

        user.setIsLocked(lockStatus);

        this.userService.updateUser(user);

        return Response.ok(userConverter.toUserWithOnlyLockJaxb(user)).build();
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

    private boolean authorizeRackerOrRackspaceApplication(String authHeader,
        User user, String methodName, String httpMethodName, String requestURI) {

        if (!authorizationHelper.checkPermission(authHeader, httpMethodName,
            requestURI)) {

            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                return authorizationHelper.checkRackspaceClientAuthorization(
                    authHeader, methodName);
            }
        }
        return true;
    }
}
