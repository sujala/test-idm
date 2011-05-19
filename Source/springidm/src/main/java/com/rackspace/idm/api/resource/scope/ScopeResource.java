package com.rackspace.idm.api.resource.scope;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ScopeResource {
    private final AuthorizationService authorizationService;
    private final ClientService clientService;
    private final ClientConverter clientConverter;
   
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ScopeResource(AuthConverter authConverter, 
        AuthorizationService authorizationService, ClientService clientService, ClientConverter clientConverter) {
       
        this.authorizationService = authorizationService;
        this.clientService = clientService;
        this.clientConverter = clientConverter;
    }
    
    /**
     * Get all scopes defined in the system.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}customer
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @Path("/scopes")
    @GET
    public Response getAvailableScopes(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader) {
        
        List<Client> clientList = this.clientService.getScopeAccessesDefinedForThisApplication();
        
        return Response.ok(clientConverter.toScopeAccessListFromClientList(clientList)).build();
    }
}
