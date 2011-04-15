package com.rackspace.idm.api.resource.customer.client.permission;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.PermissionSet;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.ForbiddenException;

/**
 * Client granted permissions
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class GrantedPermissionsResource extends AbstractClientConsumer {

    private AccessTokenService accessTokenService;
    private PermissionConverter permissionConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public GrantedPermissionsResource(AccessTokenService accessTokenService, ClientService clientService,
        PermissionConverter permissionConverter, AuthorizationService authorizationService) {
        super(clientService);
        this.accessTokenService = accessTokenService;
        this.permissionConverter = permissionConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a list of Client granted permissions.
     * 
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
    public Response getClientGrantedPermissions(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Client client = checkAndGetClient(customerId, clientId);
        if (client.getPermissions() == null || client.getPermissions().size() < 1) {
            Response.noContent().build();
        }

        PermissionSet perms = new PermissionSet();
        perms.setGranteds(client.getPermissions());

        return Response.ok(permissionConverter.toPermissionsJaxb(perms)).build();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
