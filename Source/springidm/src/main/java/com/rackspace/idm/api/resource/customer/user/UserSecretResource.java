package com.rackspace.idm.api.resource.customer.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.UserSecret;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A users secret question and answer
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserSecretResource {

    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserSecretResource(ScopeAccessService scopeAccessService, UserService userService,
        AuthorizationService authorizationService) {

        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a users secret question and answer
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
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
    public Response getUserSecret(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting Secret Q&A for User: {}", username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        // get user to update
        User user = this.userService.checkAndGetUser(customerId, username);

        com.rackspace.api.idm.v1.UserSecret secret = new com.rackspace.api.idm.v1.UserSecret();
        secret.setSecretAnswer(user.getSecretAnswer());
        secret.setSecretQuestion(user.getSecretQuestion());

        logger.debug("Got Secret Q&A for user: {}", user);

        return Response.ok(secret).build();
    }

    /**
     * Sets a users secret question and answer
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
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
    public Response setUserSecret(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, EntityHolder<com.rackspace.api.idm.v1.UserSecret> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        UserSecret userSecret = holder.getEntity();
        validateUserSecretParam(userSecret);

        logger.debug("Updating Secret Q&A for User: {}", username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeUser(token, customerId, username);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        // get user to update
        User user = this.userService.checkAndGetUser(customerId, username);

        user.setSecretQuestion(userSecret.getSecretQuestion());
        user.setSecretAnswer(userSecret.getSecretAnswer());
        this.userService.updateUser(user, false);

        logger.debug("Updated Secret Q&A for user: {}", user);

        return Response.ok(userSecret).build();
    }

    private void validateUserSecretParam(UserSecret userSecret) {

        if (StringUtils.isBlank(userSecret.getSecretQuestion())
            || StringUtils.isBlank(userSecret.getSecretAnswer())) {
            String errMsg = "";
            if (StringUtils.isBlank(userSecret.getSecretQuestion())) {
                errMsg = "Secret Question cannot be blank. ";
            }
            if (StringUtils.isBlank(userSecret.getSecretAnswer())) {
                errMsg += "Secret Answer cannot be blank.";
            }
            throw new BadRequestException(errMsg);
        }
    }
}
