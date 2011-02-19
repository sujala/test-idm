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

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;

/**
 * User soft delete flag
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserSoftDeleteResource {

    private OAuthService oauthService;
    private UserService userService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public UserSoftDeleteResource(OAuthService oauthService, UserService userService,
        UserConverter userConverter, AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.oauthService = oauthService;
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    @Deprecated
    public UserSoftDeleteResource(AccessTokenService accessTokenService, UserService userService,
        UserConverter userConverter, AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Sets a users soft delete flag.
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
    public Response setUserSoftDelete(@HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId, @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Updating SoftDelete for User: {} - {}", username, inputUser.isSoftDeleted());

        AccessToken token = this.oauthService.getAccessTokenByAuthHeader(authHeader);

        // Racker's and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeCustomerIdm(token);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (inputUser.isSoftDeleted() == null) {
            String errMsg = "Blank value for softDelted passed in.";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        boolean softDelete = inputUser.isSoftDeleted();

        // get user to update
        User user;
        if (softDelete) {
            user = checkAndGetUser(customerId, username);
        } else {
            user = this.userService.getSoftDeletedUser(customerId, username);
        }

        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }

        user.setSoftDeleted(softDelete);

        if (softDelete) {
            this.userService.softDeleteUser(username);
            oauthService.revokeTokensGloballyForOwner(username);
        } else {
            this.userService.restoreSoftDeletedUser(user);
        }

        logger.info("Updated SoftDelete for user: {} - []", user);

        return Response.ok(userConverter.toUserWithOnlySoftDeletedJaxb(user)).build();
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
        logger.error(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
