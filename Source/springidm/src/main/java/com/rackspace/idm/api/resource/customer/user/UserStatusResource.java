package com.rackspace.idm.api.resource.customer.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.jaxb.UserStatus;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A user status
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component

public class UserStatusResource {

    private ScopeAccessService scopeAccessService;
    private final OAuthService oauthService;
    private final UserService userService;
    private final UserConverter userConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserStatusResource(OAuthService accessTokenService, UserService userService, ScopeAccessService scopeAccessService,
        UserConverter userConverter, AuthorizationService authorizationService) {
        this.oauthService = accessTokenService;
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Sets a users status
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
     * @param inputUser The user status flag
     */
    @PUT
    public Response setUserStatus(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, EntityHolder<com.rackspace.idm.jaxb.User> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        logger.debug("Updating Status for User: {}", username);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        // get user to update
        User user = this.userService.checkAndGetUser(customerId, username);
        com.rackspace.idm.jaxb.User inputUser = holder.getEntity();
        String statusStr;
        try {
            statusStr = inputUser.getStatus().value();
        } catch (Exception ex) {
            String errMsg = "Invalid value for Status sent in.";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        try {
            this.userService.updateUserStatus(user, statusStr);
        } catch (IllegalArgumentException ex) {
            String errorMsg = String.format("Invalid status value: %s", statusStr);
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        if (UserStatus.INACTIVE == inputUser.getStatus()) {
            oauthService.revokeAllTokensForUser(username);
        }

        logger.debug("Updated status for user: {}", user);

        User outputUser = this.userService.getUser(customerId, username);
        return Response.ok(userConverter.toUserWithOnlyStatusJaxb(outputUser)).build();
    }
}
