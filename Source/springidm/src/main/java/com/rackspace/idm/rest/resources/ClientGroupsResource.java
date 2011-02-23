package com.rackspace.idm.rest.resources;

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
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.converters.GroupConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;

/**
 * Client groups resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientGroupsResource {
    private AccessTokenService accessTokenService;
    private ClientService clientService;
    private ClientGroupResource clientGroupResource;
    private GroupConverter groupConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ClientGroupsResource(AccessTokenService accessTokenService,
        ClientService clientService, GroupConverter groupConverter,
        ClientGroupResource clientGroupResource,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.clientGroupResource = clientGroupResource;
        this.groupConverter = groupConverter;
    }

    /**
     * Gets a list of Client groups.
     * 
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}clientGroups
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
    public Response getClientGroups(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

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

        List<ClientGroup> groups = this.clientService
            .getClientGroupsByClientId(clientId);

        return Response.ok(groupConverter.toClientGroupsJaxb(groups)).build();
    }

    /**
     * Adds a client group.
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
     * @param clientGroup New client group
     */
    @POST
    public Response addClientGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        com.rackspace.idm.jaxb.ClientGroup clientGroup) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token.isClientToken() && token.getTokenClient().getClientId()
                .equals(clientId));

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
        
        if (!clientGroup.getCustomerId().toLowerCase().equals(customerId.toLowerCase())) {
            throw new BadRequestException("CustomerId in clientGroup does not match CustomerId in url");
        }
        
        if (!clientGroup.getClientId().toLowerCase().equals(clientId.toLowerCase())) {
            throw new BadRequestException("ClientId in clientGroup does not match ClientId in url");
        }
        
        if (StringUtils.isBlank(clientGroup.getName())) {
            throw new BadRequestException("Client Group Name cannot be blank");
        }

        ClientGroup group = groupConverter.toClientGroupDO(clientGroup);

        this.clientService.addClientGroup(group);

        String location = uriInfo.getPath() + group.getName();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Group Location URI error");
        }

        return Response.ok(groupConverter.toClientGroupJaxb(group))
            .location(uri).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{groupName}")
    public ClientGroupResource getClientGroupResource() {
        return clientGroupResource;
    }
}
