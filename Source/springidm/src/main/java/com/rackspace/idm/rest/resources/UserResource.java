package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserResource {

    private ApiKeyResource apiKeyResource;
    private UserLockResource userLockResource;
    private UserPasswordResource userPasswordResource;
    private UserRolesResource userRolesResource;
    private UserSecretResource userSecretResource;
    private UserSoftDeleteResource userSoftDeleteResource;
    private UserStatusResource userStatusResource;
    private UserService userService;
    private UserConverter userConverter;
    private InputValidator inputValidator;
    private IDMAuthorizationHelper authorizationHelper;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public UserResource(ApiKeyResource apiKeyResource,
        UserLockResource userLockResource,
        UserPasswordResource userPasswordResource,
        UserRolesResource userRolesResource,
        UserSecretResource userSecretResource,
        UserSoftDeleteResource userSoftDeleteResource,
        UserStatusResource userStatusResource, UserService userService,
        UserConverter userConverter, InputValidator inputValidator,
        IDMAuthorizationHelper authorizationHelper, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.apiKeyResource = apiKeyResource;
        this.userLockResource = userLockResource;
        this.userPasswordResource = userPasswordResource;
        this.userRolesResource = userRolesResource;
        this.userSecretResource = userSecretResource;
        this.userSoftDeleteResource = userSoftDeleteResource;
        this.userStatusResource = userStatusResource;
        this.userService = userService;
        this.userConverter = userConverter;
        this.inputValidator = inputValidator;
        this.authorizationHelper = authorizationHelper;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getUser(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting User: {}", username);
        User user = checkAndGetUser(customerId, username);

        if (!checkUserOrAdminAuthorization(authHeader, user, "getUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();
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
    public Response updateUser(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        User updatedUser = userConverter.toUserDO(inputUser);

        logger.info("Updating User: {}", username);

        User user = checkAndGetUser(customerId, username);

        if (!checkUserOrAdminAuthorization(authHeader, user, "udpateUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        user.copyChanges(updatedUser);
        validateParam(user);

        try {
            this.userService.updateUser(user);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Updated User: {}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();
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
    public Response deleteUser(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.info("Deleting User :{}", username);
        User user = checkAndGetUser(customerId, username);

        if (!authorizeDeleteUser(authHeader, user, "deleteUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        this.userService.deleteUser(username);

        logger.info("Deleted User: {}", user);
        
        return Response.noContent().build();
    }

    @Path("key")
    public ApiKeyResource getApiKeyResource() {
        return apiKeyResource;
    }

    @Path("lock")
    public UserLockResource getUserLockResource() {
        return userLockResource;
    }

    @Path("password")
    public UserPasswordResource getUserPasswordResource() {
        return userPasswordResource;
    }

    @Path("roles")
    public UserRolesResource getUserRolesResource() {
        return userRolesResource;
    }

    @Path("secret")
    public UserSecretResource getUserSecretResource() {
        return userSecretResource;
    }

    @Path("softdelete")
    public UserSoftDeleteResource getUserSoftDeleteResource() {
        return userSoftDeleteResource;
    }

    @Path("status")
    public UserStatusResource getUserStatusResource() {
        return userStatusResource;
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

    private void validateParam(Object inputParam) {
        ApiError err = inputValidator.validate(inputParam);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }
    }

    private boolean authorizeDeleteUser(String authHeader, User user,
        String methodName) {
        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        String companyId = user.getCustomerId();

        if (subjectUsername == null) {
            return false;
        }

        // Condition 1: Admin can delete user.
        if (authorizationHelper.checkAdminAuthorizationForUser(subjectUsername,
            companyId, methodName)) {

            // Condition 2: Admin cannot delete himself/herself.
            if (!subjectUsername.equalsIgnoreCase(user.getUsername())) {
                return true;
            }
        }
        return false;
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
