package com.rackspace.idm.api.resource.customer.client.group;

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

import com.rackspace.idm.api.converter.GroupConverter;
import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Client groups resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ClientGroupsResource extends AbstractClientConsumer {
    private final ClientService clientService;
    private final ScopeAccessService scopeAccessService;
    private final ClientGroupResource clientGroupResource;
    private final GroupConverter groupConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ClientGroupsResource(ClientService clientService, ScopeAccessService scopeAccessService,
        GroupConverter groupConverter, ClientGroupResource clientGroupResource,
        AuthorizationService authorizationService) {
        super(clientService);
        this.scopeAccessService = scopeAccessService;
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
    public Response getClientGroups(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients andSpecific Clients are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetClient(customerId, clientId);

        List<ClientGroup> groups = this.clientService.getClientGroupsByClientId(clientId);

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
    public Response addClientGroup(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId, EntityHolder<com.rackspace.idm.jaxb.ClientGroup> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, CustomerIdm and the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (token instanceof ClientScopeAccessObject && 
                token.getClientId().equalsIgnoreCase(clientId));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.ClientGroup clientGroup = holder.getEntity();
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

        return Response.ok(groupConverter.toClientGroupJaxb(group)).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{groupName}")
    public ClientGroupResource getClientGroupResource() {
        return clientGroupResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
