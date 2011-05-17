package com.rackspace.idm.api.resource.customer.client.service;

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

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A clients service
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerClientServiceResource {

    private final ScopeAccessService scopeAccessService;
    private final ClientService clientService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientServiceResource(ScopeAccessService scopeAccessService,
        ClientService clientService, AuthorizationService authorizationService) {
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Checks to see if a permission has been granted to client
     * 
     * @response.representation.200
     * @response.representation.204
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client Id
     * @param serviceId Service Id
     * @param permissionId Permission Id
     */
    @GET
    @Path("permissions/{permissionId}")
    public Response checkIfPermissionGrantedToClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("serviceId") String serviceId,
        @PathParam("permissionId") String permissionId) {
        
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);
        
        // Rackers can check any permissions granted to a client
        // Rackspace Clients can check their own permissions granted to a client
        // Specific Clients can check their own permissions granted to a client
        // Customer IdM can check any permissions granted to a client
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token) 
            || authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo)
            || authorizationService.authorizeCustomerIdm(token);
        
        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        Client client = this.clientService.getClient(customerId, clientId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        Permission po = new Permission();
        po.setClientId(serviceId);
        po.setPermissionId(permissionId);
        
        Permission perm = this.scopeAccessService.getPermissionForParent(client.getUniqueId(), po);
        if (perm == null) {
            String errMsg = String.format("Client %s does not have permission %s", clientId, permissionId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        return Response.ok().build();
    }

   /**
    * Grants a permission to a client
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
    * @param clientId Client Id
    * @param serviceId Service Id
    * @param Permission Permission to grant
    */
    @POST
    @Path("permissions")
    public Response grantPermissionToClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("serviceId") String serviceId,
        EntityHolder<com.rackspace.idm.jaxb.Permission> holder) {

        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }


        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any permission to a client
        // Rackspace Clients can add their permission to a client
        // Specific Clients can add their permission to a client
        // Customer IdM can add any permission to a client
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeRackspaceClient(token) && token
                .getClientId().equalsIgnoreCase(serviceId))
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && token.getClientId()
                .equalsIgnoreCase(serviceId))
            || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        if (clientId.equals(serviceId)) {
            String errMsg = "Client is forbidden from granting permission to itself";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        com.rackspace.idm.jaxb.Permission permission = holder.getEntity();
        Permission filter = new Permission();
        filter.setClientId(serviceId);
        filter.setPermissionId(permission.getPermissionId());

        Client client = this.clientService.getClient(customerId, clientId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        Client grantingClient = this.clientService.getById(serviceId);
        if (grantingClient == null) {
            String errMsg = String.format("Client %s not found", serviceId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        DefinedPermission defined = (DefinedPermission) this.scopeAccessService.getPermissionForParent(grantingClient.getUniqueId(), filter);
        if (defined == null) {
            String errMsg = String.format("Permission %s not found", permission.getPermissionId());
            logger.info(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        GrantedPermission granted = new GrantedPermission();
        granted.setClientId(defined.getClientId());
        granted.setCustomerId(defined.getCustomerId());
        granted.setPermissionId(defined.getPermissionId());
        
        this.scopeAccessService.grantPermissionToClient(client.getUniqueId(), granted);
        
        return Response.ok().build();
    }

    /**
     * Revoke a permission from a client.
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
     * @param clientId Client Id
     * @param serviceId Service Id
     * @param permissionId Permission Id
     */
    @DELETE
    @Path("permissions/{permissionId}")
    public Response revokePermissionFromClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("serviceId") String serviceId,
        @PathParam("permissionId") String permissionId) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any permission to a client
        // Rackspace Clients can add their permission to a client
        // Specific Clients can add their permission to a client
        // Customer IdM can add any permission to a client
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeRackspaceClient(token) && token
                .getClientId().equalsIgnoreCase(serviceId))
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && token.getClientId()
                .equalsIgnoreCase(serviceId))
            || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        if (clientId.equals(serviceId)) {
            String errMsg = "Client is forbidden from revoking permission from itself";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        //Does client exist?
        Client client = this.clientService.getClient(customerId, clientId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        DefinedPermission definedPermission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(
                serviceId, permissionId);
        
        //Does permission exist?
        if (definedPermission == null) {
            String errMsg = String.format("Permission %s not found", permissionId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        Permission po = new Permission();
        po.setClientId(serviceId);
        po.setPermissionId(permissionId);
        
        Permission perm = this.scopeAccessService.getPermissionForParent(client.getUniqueId(), po);
        if (perm != null) {
            this.scopeAccessService.removePermission(perm);
        }


        return Response.noContent().build();
    }
}
