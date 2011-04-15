package com.rackspace.idm.api.resource.customer.client;

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

import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.api.resource.customer.client.group.ClientGroupsResource;
import com.rackspace.idm.api.resource.customer.client.permission.PermissionsResource;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.ClientCredentials;

/**
 * Client application resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerClientResource extends AbstractClientConsumer {

    private AccessTokenService accessTokenService;
    private ClientConverter clientConverter;
    private ClientService clientService;
    private PermissionsResource permissionsResource;
    private ClientGroupsResource clientGroupsResource;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientResource(AccessTokenService accessTokenService, ClientService clientService,
        ClientConverter clientConverter, PermissionsResource permissionsResource,
        ClientGroupsResource clientGroupsResource, AuthorizationService authorizationService) {
        super(clientService);
        this.accessTokenService = accessTokenService;
        this.clientService = clientService;
        this.clientConverter = clientConverter;
        this.permissionsResource = permissionsResource;
        this.authorizationService = authorizationService;
        this.clientGroupsResource = clientGroupsResource;
    }

    /**
     * Gets the client data.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId   Client application ID
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getClient(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {
        logger.debug("Getting Client: {}", clientId);

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and Users are
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

        logger.debug("Got Client: {}", client);

        com.rackspace.idm.jaxb.Client returnedClient = clientConverter.toClientJaxbWithPermissions(client);

        return Response.ok(returnedClient).build();
    }

    /**
     * Delete a client.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId   Client application ID
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    public Response deleteClient(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        logger.debug("Deleting Client: {}", clientId);

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token, request.getMethod(),
            uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Client client = checkAndGetClient(customerId, clientId);

        logger.debug("Got Client: {}", client);

        this.clientService.delete(clientId);

        logger.debug("Deleted client: {}", clientId);

        return Response.noContent().build();
    }

    /**
     * Reset the client secret
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId   Client application ID
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}clientCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @Path("secret")
    @POST
    public Response resetClientSecret(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Rackers, Admins and specific clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        Client client = checkAndGetClient(customerId, clientId);

        logger.debug("Got Client: {}", client);

        ClientSecret clientSecret = null;
        try {
            clientSecret = clientService.resetClientSecret(client);
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("Invalid client: %s", clientId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Error generating secret for client: %s", clientId);
            logger.warn(errorMsg);
            throw new IdmException(e);
        }

        ClientCredentials clientCredentials = new ClientCredentials();
        clientCredentials.setClientSecret(clientSecret.getValue());

        return Response.ok(clientCredentials).build();

    }

    @Path("groups")
    public ClientGroupsResource getClientGroupsResource() {
        return clientGroupsResource;
    }

    @Path("permissions")
    public PermissionsResource getPermissionsResource() {
        return permissionsResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
