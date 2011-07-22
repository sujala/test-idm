package com.rackspace.idm.api.resource.racker;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;

/**
 * Rackers
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RackersResource {
    private final ScopeAccessService scopeAccessService;
    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final UserConverter userConverter;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public RackersResource(ScopeAccessService scopeAccessService,
        AuthorizationService authroizationService, UserService userService,
        UserConverter userConverter) {
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authroizationService;
        this.userService = userService;
        this.userConverter = userConverter;
    }

    /**
     * Gets a list of racker roles for a racker.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}rackerRoles
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param rackerId racker's username
     */
    @GET
    @Path("{rackerId}/rackerroles")
    public Response getRackerRoles(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("rackerId") String rackerId) {
        
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Only Rackspace Clients are authorized
        boolean authorized = authorizationService.authorizeRackspaceClient(token);
        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        logger.debug("Getting Racker Roles for Racker: {}", rackerId);
        
        List<String> roles = userService.getRackerRoles(rackerId);
        
        logger.debug("Got {} Racker Roles for Racker: {}", roles.size(), rackerId);
        
        return Response.ok(userConverter.toRackerRolesJaxb(roles)).build();
    }
}
