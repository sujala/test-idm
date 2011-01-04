package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.authorizationService.AuthorizationConstants;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserRoleResource {

    private UserService userService;
    private RoleService roleService;
    private IDMAuthorizationHelper authorizationHelper;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public UserRoleResource(UserService userService,
        IDMAuthorizationHelper authorizationHelper, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.authorizationHelper = authorizationHelper;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response setRole(
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

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!authorizeSetRole(authHeader, user, "setRole", roleName)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

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
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    public Response deleteRole(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("roleName") String roleName) {

        logger.info("Deleting role for User: {}", username);

        // get user to update
        User user = this.userService.getUser(customerId, username);

        if (user == null) {
            String errorMsg = String.format(
                "Set Role Failed - User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (!authorizeDeleteRole(authHeader, user, "deleteRole", roleName)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {

                authorizationHelper.handleAuthorizationFailure();
            }
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

    private boolean authorizeSetRole(String authHeader, User user,
        String methodName, String roleName) {

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            String httpMethodName = "PUT";
            String requestURI = "/customers/" + user.getCustomerId()
                + "/users/" + user.getUsername() + "/roles/" + roleName;

            return authorizationHelper.checkPermission(authHeader,
                httpMethodName, requestURI);
        } else {
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, user.getCustomerId(), methodName);
        }
    }

    private boolean authorizeDeleteRole(String authHeader, User user,
        String methodName, String rolename) {

        String requestor = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        String targetUser = user.getUsername();

        // Condition 1: An admin can delete other user's any role (including
        // admin role).
        if (!authorizationHelper.checkAdminAuthorizationForUser(requestor, user
            .getCustomerId(), methodName)) {
            return false;
        }

        // Condition 2: An admin cannot delete his/her own admin role.
        if (requestor.equalsIgnoreCase(targetUser)
            && rolename.equalsIgnoreCase(AuthorizationConstants.ADMIN_ROLE)) {
            return false;
        }
        return true;
    }
}
