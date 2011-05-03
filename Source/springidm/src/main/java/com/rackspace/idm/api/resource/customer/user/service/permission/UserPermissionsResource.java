package com.rackspace.idm.api.resource.customer.user.service.permission;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
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
    private final ClientService clientService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserPermissionsResource(UserService userService,
        AuthorizationService authorizationService, ClientService clientService,
        ScopeAccessService scopeAccessService, UserConverter userConverter) {
        this.clientService = clientService;
        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
    }

    /**
     * Check to see if a user has a permission.
     * 
     * @response.representation.200.doc
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
     * @param serviceId serviceId
     * @param permissionId permissionId
     */
    @GET
    @Path("{permissionId}")
    public Response checkIfUserHasPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("serviceId") String serviceId,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can check any permissions granted to a user
        // Rackspace Clients can check their own permissions granted to a user
        // Specific Clients can check their own permissions granted to a user
        // Customer IdM can check any permissions granted to a user
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo) || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        User user = this.userService.checkAndGetUser(customerId, username);

        PermissionObject po = new PermissionObject();
        po.setClientId(serviceId);
        po.setPermissionId(permissionId);

        Client client = this.clientService.getById(serviceId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", serviceId);
            logger.info(errMsg);
            throw new NotFoundException(errMsg);
        }

        PermissionObject definedPermission = this.scopeAccessService
            .getPermissionForParent(client.getUniqueId(), po);

        if (definedPermission == null || !definedPermission.getEnabled()) {
            return Response.status(404).build();
        }

        if (definedPermission.getGrantedByDefault()
            || this.scopeAccessService.getPermissionForParent(
                user.getUniqueId(), po) != null) {
            return Response.ok().build();
        }

        return Response.status(404).build();
    }

    /**
     * Grant a permission to a User.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.doc
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
     * @param serviceId serviceId
     * @param Permission permission
     */
    @POST
    public Response grantPermissionToUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("serviceId") String serviceId,
        EntityHolder<com.rackspace.idm.jaxb.Permission> holder) {

        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = authorizeGrantRevokePermission(token, request,
            uriInfo, serviceId);
        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.Permission permission = holder.getEntity();
        PermissionObject filter = new PermissionObject();
        filter.setClientId(serviceId);
        filter.setPermissionId(permission.getPermissionId());

        Client client = this.clientService.getById(serviceId);

        if (client == null) {
            String errMsg = String.format("Client %s not found", serviceId);
            logger.info(errMsg);
            throw new NotFoundException(errMsg);
        }

        PermissionObject defined = this.scopeAccessService
            .getPermissionForParent(client.getUniqueId(), filter);
        if (defined == null) {
            String errMsg = String.format("Permission %s not found",
                permission.getPermissionId());
            logger.info(errMsg);
            throw new NotFoundException(errMsg);
        }

        PermissionObject granted = new PermissionObject();
        granted.setClientId(defined.getClientId());
        granted.setCustomerId(defined.getCustomerId());
        granted.setPermissionId(defined.getPermissionId());

        User user = this.userService.checkAndGetUser(customerId, username);

        this.scopeAccessService.grantPermissionToUser(user, granted);

        return Response.ok().build();
    }

    /**
     * Revoke a User's permission.
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
     * @param username username
     * @param serviceId serviceId
     * @param permissionId Permission ID
     */
    @DELETE
    @Path("{permissionId}")
    public Response revokePermissionFromUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("serviceId") String serviceId,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = authorizeGrantRevokePermission(token, request,
            uriInfo, serviceId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        User user = this.userService.checkAndGetUser(customerId, username);

        PermissionObject definedPermission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(serviceId,
                permissionId);

        if (definedPermission == null) {
            String errMsg = String.format("Permission %s not found",
                permissionId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        PermissionObject po = new PermissionObject();
        po.setClientId(serviceId);
        po.setPermissionId(permissionId);

        PermissionObject perm = this.scopeAccessService.getPermissionForParent(
            user.getUniqueId(), po);
        if (perm != null) {
            this.scopeAccessService.removePermission(perm);
        }

        return Response.noContent().build();
    }

    private boolean authorizeGrantRevokePermission(ScopeAccessObject token,
        Request request, UriInfo uriInfo, String clientId) {
        // Rackers can grant any permission to a user
        // Rackspace Clients can grant their own permission to a user
        // Specific Clients can grant their own permission to a user
        // Customer IdM can grant any permission to user
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeRackspaceClient(token) && token
                .getClientId().equalsIgnoreCase(clientId))
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && token.getClientId()
                .equalsIgnoreCase(clientId))
            || authorizationService.authorizeCustomerIdm(token);

        return authorized;
    }
}
