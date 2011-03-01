package com.rackspace.idm.api.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

/**
 * A User.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserResource {
    private AccessTokenService accessTokenService;
    private ApiKeyResource apiKeyResource;
    private UserLockResource userLockResource;
    private UserPasswordResource userPasswordResource;
    private UserGroupsResource userGroupsResource;
    private UserSecretResource userSecretResource;
    private UserSoftDeleteResource userSoftDeleteResource;
    private UserStatusResource userStatusResource;
    private UserService userService;
    private UserConverter userConverter;
    private InputValidator inputValidator;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserResource(AccessTokenService accessTokenService, ApiKeyResource apiKeyResource,
        UserLockResource userLockResource, UserPasswordResource userPasswordResource,
        UserGroupsResource userGroupsResource, UserSecretResource userSecretResource,
        UserSoftDeleteResource userSoftDeleteResource, UserStatusResource userStatusResource,
        UserService userService, UserConverter userConverter, InputValidator inputValidator,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.apiKeyResource = apiKeyResource;
        this.userLockResource = userLockResource;
        this.userPasswordResource = userPasswordResource;
        this.userGroupsResource = userGroupsResource;
        this.userSecretResource = userSecretResource;
        this.userSoftDeleteResource = userSoftDeleteResource;
        this.userStatusResource = userStatusResource;
        this.userService = userService;
        this.userConverter = userConverter;
        this.inputValidator = inputValidator;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a user.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
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
    public Response getUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        AccessToken token = accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        logger.debug("Getting User: {}", username);
        User user = checkAndGetUser(customerId, username);

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();
    }

    /**
     * Updates a user.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
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
    public Response updateUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, com.rackspace.idm.jaxb.User inputUser) {

        AccessToken token = accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients, Admins and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User updatedUser = userConverter.toUserDO(inputUser);

        logger.debug("Updating User: {}", username);

        User user = checkAndGetUser(customerId, username);

        user.copyChanges(updatedUser);
        validateParam(user);

        try {
            this.userService.updateUser(user);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.debug("Updated User: {}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();
    }

    /**
     * Deletes a user.
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
     */
    @DELETE
    public Response deleteUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Deleting User :{}", username);

        AccessToken token = accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        this.userService.deleteUser(username);

        logger.debug("Deleted User: {}", user);

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

    @Path("groups")
    public UserGroupsResource getUserRolesResource() {
        return userGroupsResource;
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
        String errorMsg = String.format("User not found: %s - %s", customerId, username);
        logger.warn(errorMsg);
        throw new NotFoundException(errorMsg);
    }

    private void validateParam(Object inputParam) {
        ApiError err = inputValidator.validate(inputParam);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }
    }
}
