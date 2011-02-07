package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.GroupConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;

/**
 * a client group resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientGroupResource {
    private AccessTokenService accessTokenService;
    private ClientService clientService;
    private GroupConverter groupConverter;
    private AuthorizationService authorizationService;
    private ClientGroupMembersResource clientGroupMembersResource;
    private Logger logger;

    @Autowired
    public ClientGroupResource(AccessTokenService accessTokenService,
        ClientService clientService, GroupConverter groupConverter,
        ClientGroupMembersResource clientGroupMembersResource,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.groupConverter = groupConverter;
        this.clientGroupMembersResource = clientGroupMembersResource;
        this.logger = logger.getLogger(this.getClass());
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
     * @param groupName Group name
     */
    @DELETE
    public Response deleteClientGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("groupName") String groupName) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || (token.isClientToken() && token.getTokenClient().getClientId()
                .equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        this.clientService.deleteClientGroup(clientId, groupName);

        return Response.noContent().build();
    }

    /**
     * Gets a Client defined permission.
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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients andSpecific Clients are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        ClientGroup group = this.clientService
            .getClientGroupByClientIdAndGroupName(clientId, groupName);

        if (group == null) {
            throw new NotFoundException("Client Group not found.");
        }

        return Response.ok(groupConverter.toClientGroupJaxb(group)).build();
    }

    @Path("members")
    public ClientGroupMembersResource getClientGroupMembersResource() {
        return clientGroupMembersResource;
    }
}
