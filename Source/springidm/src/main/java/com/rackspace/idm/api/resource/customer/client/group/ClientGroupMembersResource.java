package com.rackspace.idm.api.resource.customer.client.group;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;

/**
 * a client group resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientGroupMembersResource extends AbstractClientConsumer {
    private AccessTokenService accessTokenService;
    private ClientService clientService;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ClientGroupMembersResource(AccessTokenService accessTokenService, ClientService clientService,
        AuthorizationService authorizationService) {
        super(clientService);
        this.accessTokenService = accessTokenService;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
    }

    /**
     * Adds a user to a client group.
     * 
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
     * @param clientId Client application ID
     * @param username The user to add
     */
    @PUT
    @Path("{username}")
    public Response addUserToClientGroup(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId, @PathParam("groupName") String groupName,
        @PathParam("username") String username) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token.isClientToken() && token.getTokenClient().getClientId().equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        this.clientService.addUserToClientGroup(username, customerId, clientId, groupName);

        return Response.ok().build();
    }

    /**
     * Removes a user from a client group.
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
     * @param username The user to add
     */
    @DELETE
    @Path("{username}")
    public Response removeUserFromClientGroup(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId, @PathParam("groupName") String groupName,
        @PathParam("username") String username) {

        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token.isClientToken() && token.getTokenClient().getClientId().equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        ClientGroup group = this.clientService
            .getClientGroup(customerId, clientId, groupName);
        
        if (group == null) {
            String errMsg = String
                .format(
                    "ClientGroup with Name %s, ClientId %s, and CustomerId %s not found.",
                    groupName, clientId, customerId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }    
     
        this.clientService.removeUserFromClientGroup(username, group);

        return Response.noContent().build();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
