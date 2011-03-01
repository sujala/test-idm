package com.rackspace.idm.api.resource.customer.client.permission;

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

import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.PermissionSet;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;

/**
 * Client permissions
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class PermissionsResource {

    private AccessTokenService accessTokenService;
    private DefinedPermissionsResource definedPermissionsResource;
    private GrantedPermissionsResource grantedPermissionsResource;
    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public PermissionsResource(AccessTokenService accessTokenService,
        DefinedPermissionsResource definedPermissionsResource,
        GrantedPermissionsResource grantedPermissionsResource,
        ClientService clientService, PermissionConverter permissionConverter,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.definedPermissionsResource = definedPermissionsResource;
        this.grantedPermissionsResource = grantedPermissionsResource;
        this.permissionConverter = permissionConverter;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a list of defined and granted permissions for a client.
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permissions
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
     */
    @GET
    public Response getClientPermissions(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        logger.debug(String.format("Getting Permissions for client %s",
            clientId));

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
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

        List<Permission> defineds = this.clientService
            .getDefinedPermissionsByClientId(clientId);

        PermissionSet permset = new PermissionSet();

        permset.setDefineds(defineds);
        permset.setGranteds(client.getPermissions());
        logger.debug(String.format("Got Permissions for client %s", clientId));
        return Response.ok(permissionConverter.toPermissionsJaxb(permset))
            .build();
    }

    @Path("defined")
    public DefinedPermissionsResource getDefinedPermissionsResource() {
        return definedPermissionsResource;
    }

    @Path("granted")
    public GrantedPermissionsResource getGrantedPermissionsResource() {
        return grantedPermissionsResource;
    }
}
