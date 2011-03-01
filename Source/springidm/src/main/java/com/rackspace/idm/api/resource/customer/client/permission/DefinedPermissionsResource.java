package com.rackspace.idm.api.resource.customer.client.permission;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
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

import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.PermissionSet;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PermissionConflictException;
import com.rackspace.idm.validation.InputValidator;

/**
 * Client defined permissions.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionsResource {

    private DefinedPermissionResource definedPermissionResource;
    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private InputValidator inputValidator;
    private AuthorizationService authorizationService;
    private AccessTokenService accessTokenService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DefinedPermissionsResource(
        DefinedPermissionResource definedPermissionResource,
        ClientService clientService, PermissionConverter permissionConverter,
        InputValidator inputValidator,
        AuthorizationService authorizationService,
        AccessTokenService accessTokenService) {
        this.definedPermissionResource = definedPermissionResource;
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.accessTokenService = accessTokenService;
    }

    /**
     * Gets a list of Client defined permissions.
     * 
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
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
    public Response getClientDefinedPermissions(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

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

        if (defineds == null) {
            String errorMsg = String.format(
                "Permissions Not Found for client: %s", clientId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        PermissionSet permset = new PermissionSet();

        permset.setDefineds(defineds);

        return Response.ok(permissionConverter.toPermissionsJaxb(permset))
            .build();
    }

    /**
     * Adds a client defined permission.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
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
     * @param permission New permission
     */
    @POST
    public Response addClientPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        com.rackspace.idm.jaxb.Permission permission) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || (token.isClientToken() && token.getTokenClient().getClientId()
                .equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        permission.setClientId(clientId);
        permission.setCustomerId(customerId);

        Permission permissionDO = permissionConverter
            .toPermissionDO(permission);

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        try {
            this.clientService.addDefinedPermission(permissionDO);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.warn(errorMsg);
            throw new PermissionConflictException(errorMsg);
        }

        String location = uriInfo.getPath() + permission.getPermissionId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Permission Location URI error");
        }

        return Response.ok(permission).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{permissionId}")
    public DefinedPermissionResource getDefinedPermissionResource() {
        return definedPermissionResource;
    }
}
