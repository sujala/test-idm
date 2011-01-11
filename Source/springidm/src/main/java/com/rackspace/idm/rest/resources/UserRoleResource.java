package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;

/**
 * A user role.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserRoleResource {

    private AccessTokenService accessTokenService;
    private UserService userService;
    private RoleService roleService;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public UserRoleResource(AccessTokenService accessTokenService,
        UserService userService, AuthorizationService authorizationService,
        RoleService roleService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.roleService = roleService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Set a user's Role.
     * 
     * @response.representation.204.doc Successful request
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
     * @param roleName Role to add to user
     */
    @PUT
    public Response setRole(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("roleName") String roleName) {

        if (StringUtils.isBlank(roleName)) {
            String errorMsg = "RoleName cannot be blank";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Setting role {} for User {}", roleName, username);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        // get user to update
        User user = checkAndGetUser(customerId, username);

        // get role to add user to
        Role role = this.roleService.getRole(roleName, user.getCustomerId());

        if (role == null) {
            String errorMsg = String.format(
                "Set Role Failed - Role not found: {}", roleName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.roleService.addUserToRole(user, role);

        logger.info("Set the role {} for user {}", role, user);

        return Response.noContent().build();
    }

    /**
     * Delete a user's role
     * @response.representation.204.doc Successful request
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
     * @param roleName Role to delete from a user
     */
    @DELETE
    public Response deleteRole(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("roleName") String roleName) {

        logger.info("Deleting role for User: {}", username);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        // get user to update
        User user = this.userService.getUser(customerId, username);

        if (user == null) {
            String errorMsg = String.format(
                "Set Role Failed - User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        // get role to add user to
        Role role = this.roleService.getRole(roleName, user.getCustomerId());

        if (role == null) {
            String errorMsg = String.format(
                "Set Role Failed - Role not found: {}", roleName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.roleService.deleteUserFromRole(user, role);

        logger.info("User {} deleted from role {}", user, role);

        return Response.noContent().build();
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
