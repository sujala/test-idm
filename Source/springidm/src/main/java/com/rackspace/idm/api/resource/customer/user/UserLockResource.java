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
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * User lock.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserLockResource {

    private final OAuthService oauthService;
    private final UserService userService;
    private final ScopeAccessService scopeAccessService;
    private final UserConverter userConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserLockResource(OAuthService oauthService, ScopeAccessService scopeAccessService, UserService userService, UserConverter userConverter,
        AuthorizationService authorizationService) {
        this.oauthService = oauthService;
        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Sets the value for the user lock.
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
     * @param inputUser User lock
     */
    @PUT
    public Response setUserLock(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, EntityHolder<com.rackspace.idm.jaxb.User> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        logger.info("Locking User: {} - {}", username);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Rcker's and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.User inputUser = holder.getEntity();
        if (inputUser.isLocked() == null) {
            String errMsg = "Invalid value for locked sent in.";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        User user = this.userService.checkAndGetUser(customerId, username);

        user.setLocked(inputUser.isLocked());
        this.userService.updateUser(user, false);
        if (inputUser.isLocked()) {
            oauthService.revokeAllTokensForUser(username);
        }

        return Response.ok(userConverter.toUserWithOnlyLockJaxb(user)).build();
    }
}
