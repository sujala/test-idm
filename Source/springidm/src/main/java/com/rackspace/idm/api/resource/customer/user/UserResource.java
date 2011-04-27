package com.rackspace.idm.api.resource.customer.user;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.api.resource.customer.user.permission.UserPermissionsResource;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A User.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserResource {
    
    private ScopeAccessService scopeAccessService;
    private ApiKeyResource apiKeyResource;
    private UserLockResource userLockResource;
    private UserPasswordResource userPasswordResource;
    private UserGroupsResource userGroupsResource;
    private UserSecretResource userSecretResource;
    private UserSoftDeleteResource userSoftDeleteResource;
    private UserStatusResource userStatusResource;
    private UserPermissionsResource userPermissionsResource;
    private UserService userService;
    private UserConverter userConverter;
    private InputValidator inputValidator;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserResource(ScopeAccessService scopeAccessService, ApiKeyResource apiKeyResource,
        UserLockResource userLockResource, UserPasswordResource userPasswordResource,
        UserGroupsResource userGroupsResource, UserSecretResource userSecretResource,
        UserSoftDeleteResource userSoftDeleteResource, UserStatusResource userStatusResource, 
        UserPermissionsResource userPermissionsResource, 
        UserService userService, UserConverter userConverter, InputValidator inputValidator,
        AuthorizationService authorizationService) {
        
        this.scopeAccessService = scopeAccessService;
        this.apiKeyResource = apiKeyResource;
        this.userLockResource = userLockResource;
        this.userPasswordResource = userPasswordResource;
        this.userGroupsResource = userGroupsResource;
        this.userSecretResource = userSecretResource;
        this.userSoftDeleteResource = userSoftDeleteResource;
        this.userStatusResource = userStatusResource;
        this.userPermissionsResource = userPermissionsResource;
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

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        logger.debug("Getting User: {}", username);
        User user = this.userService.checkAndGetUser(customerId, username);

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();
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
        @PathParam("username") String username, EntityHolder<com.rackspace.idm.jaxb.User> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients, Admins and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.User inputUser = holder.getEntity();
        if (inputUser.getApiKey() != null && !StringUtils.isEmpty(inputUser.getApiKey().getApiKey())) {
            String errMsg = String.format("Setting the apiKey is Forbidden from this call for user %s",
                inputUser.getUsername());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User updatedUser = userConverter.toUserDO(inputUser);

        logger.debug("Updating User: {}", username);

        User user = this.userService.checkAndGetUser(customerId, username);

        user.copyChanges(updatedUser);
        validateParam(user);

        try {
            // Password can only be updated via the UserPasswordResource
            this.userService.updateUser(user, false);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.debug("Updated User: {}", user);
        return Response.ok(userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();
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

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        User user = this.userService.checkAndGetUser(customerId, username);

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
    public UserGroupsResource getUserGroupsResource() {
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
    
    @Path("permissions")
    public UserPermissionsResource getUserPermissionResource() {
        return userPermissionsResource;
    }
    
    private void validateParam(Object inputParam) {
        ApiError err = inputValidator.validate(inputParam);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }
    }
}
