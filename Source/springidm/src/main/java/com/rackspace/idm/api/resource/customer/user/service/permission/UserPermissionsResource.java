package com.rackspace.idm.api.resource.customer.user.service.permission;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * User Permission Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPermissionsResource {

    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserPermissionsResource(UserService userService,
        AuthorizationService authorizationService, ClientService clientService, ScopeAccessService scopeAccessService,
        UserConverter userConverter, PermissionConverter permissionConverter) {

        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a permission.
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
     * @param permissionId permissionId
     */
    @GET
    public Response getPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients and Specific Clients are
        // authorized
        // TODO Need to find out the correct authn
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);
        
        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Permission perm = this.userService.getGrantedPermission(username,
            permissionId);

        logger.debug("Got Permission :{}", permissionId);

        return Response.ok(permissionConverter.toPermissionJaxb(perm)).build();

    }

    /**
     * Grant a permission to a User.
     * 
     * @response.representation.204.doc
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @PUT
    public Response grantPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        EntityHolder<com.rackspace.idm.jaxb.Permission> holder) {

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients and Specific Clients are
        // authorized
        // TODO Need to find out the correct authn
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.Permission perm = holder.getEntity();

        String clientId = token.getClientId();

        Permission permissionToGrant = this.clientService
            .checkAndGetPermission(customerId, clientId, perm.getPermissionId());

        this.userService.grantPermission(username, permissionToGrant);

        return Response.ok(
            permissionConverter.toPermissionJaxb(permissionToGrant)).build();
    }

    /**
     * Revoke a User permission.
     * 
     * @response.representation.204.doc
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param permissionId Permission ID
     */
    @DELETE
    public Response revokePermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);
        // Racker's, Rackspace Clients and Specific Clients are
        // authorized

        // TODO Need to find out the correct authn
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        String clientId = token.getClientId();

        Permission permissionToGrant = this.clientService
            .checkAndGetPermission(customerId, clientId, permissionId);

        this.userService.revokePermission(permissionToGrant);

        return Response.noContent().build();
    }

}
