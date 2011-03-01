package com.rackspace.idm.api.resources;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;

/**
 * A Mosso User.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class MossoUserResource {

    private AccessTokenService accessTokenService;
    private UserService userService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    public MossoUserResource(AccessTokenService accessTokenService,
        UserService userService, UserConverter userConverter,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.userConverter = userConverter;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a nast user.
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
     * @param mossoId username
     */
    @GET
    @Path("{mossoId}")
    public Response getUser(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("mossoId") int mossoId) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        logger.debug("Getting User: {}", mossoId);
        User user = this.userService.getUserByMossoId(mossoId);

        if (user == null) {
            String errMsg = String.format("User with mossoId %s not found",
                mossoId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        logger.debug("Got User :{}", user);
        return Response.ok(userConverter.toUserWithOnlyRolesJaxb(user)).build();

    }
}
