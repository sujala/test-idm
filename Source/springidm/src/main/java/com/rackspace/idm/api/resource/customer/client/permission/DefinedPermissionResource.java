package com.rackspace.idm.api.resource.customer.client.permission;

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
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A Client defined permission.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionResource extends AbstractClientConsumer {

    private final ClientService clientService;
    private final PermissionConverter permissionConverter;
    private final InputValidator inputValidator;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DefinedPermissionResource(ClientService clientService,
        PermissionConverter permissionConverter, InputValidator inputValidator,
        AuthorizationService authorizationService,
        ScopeAccessService scopeAccessService) {
        super(clientService);
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;

    }

    /**
     * Updates a defined permission for a client.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client application ID
     * @param permissionId Permission ID
     */
    @PUT
    public Response updateClientDefinedPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        EntityHolder<com.rackspace.idm.jaxb.Permission> holder) {
        
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && (token instanceof ClientScopeAccessObject && token
                .getClientId().equalsIgnoreCase(clientId)));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        PermissionObject inputPermisison = permissionConverter.toPermissionDO(holder.getEntity());
        validateClientPermissionRequest(customerId, clientId, permissionId,
            inputPermisison);

        PermissionObject permissionDO = this.checkAndGetPermission(customerId,
            clientId, permissionId);
        
        permissionDO.copyChanges(inputPermisison);

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        this.clientService.updateDefinedPermission(permissionDO);

        return Response.ok(permissionConverter.toPermissionJaxb(permissionDO)).build();
    }

    /**
     * Deletes a Client defined permission.
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
     * @param clientId Client application ID
     * @param permissionId Permission ID
     */
    @DELETE
    public Response deleteClientDefinedPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && (token instanceof ClientScopeAccessObject && token
                .getClientId().equalsIgnoreCase(clientId)));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        PermissionObject permission = this.checkAndGetPermission(customerId,
            clientId, permissionId);
        this.clientService.deleteDefinedPermission(permission);

        return Response.noContent().build();
    }

    /**
     * Gets a Client defined permission.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client application ID
     * @param permissionId Permission ID
     */
    @GET
    public Response getClientDefinedPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && (token instanceof ClientScopeAccessObject && token
                .getClientId().equalsIgnoreCase(clientId)))
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetClient(customerId, clientId);
        PermissionObject permission = this.checkAndGetPermission(customerId,
            clientId, permissionId);

        return Response.ok(permissionConverter.toPermissionJaxb(permission))
            .build();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    private PermissionObject checkAndGetPermission(String customerId,
        String clientId, String permissionId) throws NotFoundException {
        PermissionObject permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (permission == null
            || !customerId.equalsIgnoreCase(permission.getCustomerId())
            || !clientId.equalsIgnoreCase(permission.getClientId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return permission;
    }

    private void validateClientPermissionRequest(String customerId,
        String clientId, String permissionId, PermissionObject permission)
        throws BadRequestException {

        if (!customerId.equalsIgnoreCase(permission.getCustomerId())) {
            String errMsg = "CustomerId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        if (!clientId.equalsIgnoreCase(permission.getClientId())) {
            String errMsg = "ClientId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        
        if (!permissionId.equalsIgnoreCase(permission.getPermissionId())) {
            String errMsg = "PermissionId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }
}
