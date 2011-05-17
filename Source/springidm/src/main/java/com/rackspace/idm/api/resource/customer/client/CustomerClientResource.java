package com.rackspace.idm.api.resource.customer.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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

import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.api.resource.customer.client.group.ClientGroupsResource;
import com.rackspace.idm.api.resource.customer.client.permission.DefinedPermissionsResource;
import com.rackspace.idm.api.resource.customer.client.service.CustomerClientServicesResource;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.ClientCredentials;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Client application resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerClientResource extends AbstractClientConsumer {

    private final DefinedPermissionsResource definedPermissionsResource;
    private final CustomerClientServicesResource customerClientServicesResource;
    private final ScopeAccessService scopeAccessService;
    private final ClientConverter clientConverter;
    private final ClientService clientService;
    private final ClientGroupsResource clientGroupsResource;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerClientResource(
        DefinedPermissionsResource definedPermissionsResource,
        CustomerClientServicesResource customerClientServicesResource,
        ClientService clientService, ScopeAccessService scopeAccessService,
        ClientConverter clientConverter,
        ClientGroupsResource clientGroupsResource,
        AuthorizationService authorizationService) {
        super(clientService);
        this.definedPermissionsResource = definedPermissionsResource;
        this.customerClientServicesResource = customerClientServicesResource;
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.clientConverter = clientConverter;
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
    public Response getClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {
        logger.debug("Getting Client: {}", clientId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and Users are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Client client = checkAndGetClient(customerId, clientId);

        logger.debug("Got Client: {}", client);

        com.rackspace.idm.jaxb.Client returnedClient = clientConverter
            .toClientJaxbWithoutPermissionsOrCredentials(client);

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
    public Response deleteClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        logger.info("Deleting Client: {}", clientId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Client client = checkAndGetClient(customerId, clientId);

        logger.debug("Got Client: {}", client);

        this.clientService.delete(clientId);

        logger.info("Deleted client: {}", clientId);

        return Response.noContent().build();
    }

    /**
     * Update a client.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId   Client application ID
     * @param client Updated client
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response updateClient(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        EntityHolder<com.rackspace.idm.jaxb.Client> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }

        logger.info("Updating Client: {}", clientId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients, Admins and IdM are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && clientId.equals(token
                .getClientId()))
            || authorizationService.authorizeCustomerIdm(token)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        com.rackspace.idm.jaxb.Client inputClient = holder.getEntity();
        Client updatedClient = clientConverter.toClientDO(inputClient);

        Client client = checkAndGetClient(customerId, clientId);
        client.copyChanges(updatedClient);

        logger.debug("Got Client: {}", client);

        this.clientService.updateClient(client);

        logger.info("Udpated client: {}", clientId);

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
    public Response resetClientSecret(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers, Admins and specific clients are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

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
            String errorMsg = String.format(
                "Error generating secret for client: %s", clientId);
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
    public DefinedPermissionsResource getDefinedPermissionsResource() {
        return definedPermissionsResource;
    }

    @Path("services")
    public CustomerClientServicesResource getCustomerClientServicesResource() {
        return customerClientServicesResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
