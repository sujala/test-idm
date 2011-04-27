package com.rackspace.idm.api.resource.customer.client.group;

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

import com.rackspace.idm.api.converter.GroupConverter;
import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * a client group resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientGroupResource extends AbstractClientConsumer {
    private final ClientService clientService;
    private final ScopeAccessService scopeAccessService;
    private final GroupConverter groupConverter;
    private final AuthorizationService authorizationService;
    private final ClientGroupMembersResource clientGroupMembersResource;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ClientGroupResource(
        ClientService clientService, ScopeAccessService scopeAccessService,
        GroupConverter groupConverter,
        ClientGroupMembersResource clientGroupMembersResource,
        AuthorizationService authorizationService) {
        super(clientService);
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
        this.groupConverter = groupConverter;
        this.clientGroupMembersResource = clientGroupMembersResource;
    }

    /**
     * Deletes a Client groups
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
     * @param groupName Group name
     */
    @DELETE
    public Response deleteClientGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("groupName") String groupName) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token instanceof ClientScopeAccessObject && token.getClientId()
                .equalsIgnoreCase(clientId));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        this.clientService.deleteClientGroup(customerId, clientId, groupName);

        return Response.noContent().build();
    }

    /**
     * Gets a Client group.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}clientGroup
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
     * @param groupName Group name
     */
    @GET
    public Response getClientGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("groupName") String groupName) {

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients and Specific Clients are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        ClientGroup group = checkAndGetClientGroup(customerId, clientId,
            groupName);

        return Response.ok(groupConverter.toClientGroupJaxb(group)).build();
    }

    /**
     * Updates a Client group
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}clientGroup
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}clientGroup
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
     * @param groupName Group name
     * @param clientGroup New client group
     */
    @PUT
    public Response updateClientGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("groupName") String groupName,
        EntityHolder<com.rackspace.idm.jaxb.ClientGroup> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token instanceof ClientScopeAccessObject && token.getClientId()
                .equalsIgnoreCase(clientId));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.ClientGroup clientGroup = holder.getEntity();
        ClientGroup group = checkAndGetClientGroup(clientGroup.getCustomerId(),
            clientGroup.getClientId(), clientGroup.getName());

        group.setType(clientGroup.getType());

        this.clientService.updateClientGroup(group);

        return Response.ok(group).build();
    }

    @Path("members")
    public ClientGroupMembersResource getClientGroupMembersResource() {
        return clientGroupMembersResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
