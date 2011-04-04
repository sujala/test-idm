package com.rackspace.idm.api.resource.customer.client.permission;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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

import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;

/**
 * A Client defined permission.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionResource {

    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private InputValidator inputValidator;
    private AuthorizationService authorizationService;
    private AccessTokenService accessTokenService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DefinedPermissionResource(ClientService clientService,
        PermissionConverter permissionConverter, InputValidator inputValidator,
        AuthorizationService authorizationService,
        AccessTokenService accessTokenService) {
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.accessTokenService = accessTokenService;
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
    public Response updateClientPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        com.rackspace.idm.jaxb.Permission permission) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo.getPath()) && token
                .getTokenClient().getClientId().equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (!permission.getPermissionId().equals(permissionId)) {
            String errMsg = "PermissionId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        if (!permission.getCustomerId().equals(customerId)) {
            String errMsg = "CustomerId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        if (!permission.getClientId().equals(clientId)) {
            String errMsg = "ClientId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        Permission permissionDO = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (permissionDO == null
            || !customerId.equals(permissionDO.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        permissionDO.setType(permission.getType());
        permissionDO.setValue(permission.getValue());

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        this.clientService.updateDefinedPermission(permissionDO);

        return Response.ok(permission).build();
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
    public Response deleteClientPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo.getPath()) && token
                .getTokenClient().getClientId().equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Permission permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (permission == null
            || !customerId.equals(permission.getCustomerId()) || 
               !clientId.equals(permission.getClientId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

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
    public Response getClientPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo.getPath()) && token
                .getTokenClient().getClientId().equals(clientId))
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Client client = this.clientService.getById(clientId);
        if (client == null || !client.getCustomerId().equals(customerId)) {
            String errMsg = String.format("Client with Id %s not found.",
                clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        Permission permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (permission == null || !clientId.equals(permission.getClientId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return Response.ok(permissionConverter.toPermissionJaxb(permission))
            .build();
    }

    /**
     * Grants a defined permission to a client.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
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
    @Path("grant")
    public Response grantPermissionToClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        com.rackspace.idm.jaxb.Client targetClient) {

        checkGrantRevokePermissionAuthorization(authHeader, clientId,
            request.getMethod(), uriInfo.getPath());

        Permission permissionToGrant = checkAndGetPermission(customerId,
            clientId, permissionId);

        this.clientService.grantPermission(targetClient.getClientId(),
            permissionToGrant);

        return Response.ok(
            permissionConverter.toPermissionJaxb(permissionToGrant)).build();
    }

    /**
     * Revokes a defined permission from a client.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
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
    @Path("revoke")
    public Response revokePermissionToClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        com.rackspace.idm.jaxb.Client targetClient) {

        checkGrantRevokePermissionAuthorization(authHeader, clientId,
            request.getMethod(), uriInfo.getPath());

        Permission permissionToRevoke = checkAndGetPermission(customerId,
            clientId, permissionId);

        this.clientService.revokePermission(targetClient.getClientId(),
            permissionToRevoke);

        return Response.ok(
            permissionConverter.toPermissionJaxb(permissionToRevoke)).build();
    }

    private void checkGrantRevokePermissionAuthorization(String authHeader,
        String clientId, String method, String uri) {
        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only the client who "owns" the permission and IDM is allowed to grant
        // or revoke it.
        boolean authorized = (token.isClientToken() && token.getTokenClient()
            .getClientId().equals(clientId))
            || authorizationService.authorizeClient(token, method, uri);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private Permission checkAndGetPermission(String customerId,
        String clientId, String permissionId) {
        Permission permission = this.clientService
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
}
