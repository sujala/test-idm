package com.rackspace.idm.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;

/**
 * A users roles.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserRolesResource {

    private UserRoleResource userRoleResource;
    private UserService userService;
    private RoleService roleService;
    private RoleConverter roleConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public UserRolesResource(UserRoleResource userRoleResource,
        UserService userService, RoleService roleService,
        RoleConverter roleConverter, AuthorizationService authorizationService,
        LoggerFactoryWrapper logger) {
        this.userRoleResource = userRoleResource;
        this.userService = userService;
        this.roleService = roleService;
        this.roleConverter = roleConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets a list of the roles a user has.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}roles
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
    public Response getRoles(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting roles for User: {}", username);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
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

        User user = checkAndGetUser(customerId, username);

        // get roles for user
        List<Role> roles = this.roleService.getRolesForUser(username);
        logger.debug("Got roles for User: {} - {}", user, roles);

        com.rackspace.idm.jaxb.Roles outputRoles = roleConverter
            .toRolesJaxb(roles);

        return Response.ok(outputRoles).build();
    }

    @Path("{roleName}")
    public UserRoleResource getUserRoleResource() {
        return userRoleResource;
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
