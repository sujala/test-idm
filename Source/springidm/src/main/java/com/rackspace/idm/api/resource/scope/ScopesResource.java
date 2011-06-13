package com.rackspace.idm.api.resource.scope;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import com.rackspace.idm.domain.service.ClientService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ScopesResource {
    private final ClientService clientService;
    private final ClientConverter clientConverter;
   
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ScopesResource(AuthConverter authConverter, 
        ClientService clientService, ClientConverter clientConverter) {

        this.clientService = clientService;
        this.clientConverter = clientConverter;
    }
    
    /**
     * Get all scopes defined in the system.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}scopes
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     */
    @GET
    public Response getAvailableScopes(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader) {
        
        logger.debug("Getting available scopes");
        
        List<Client> clientList = this.clientService.getAvailableScopes();
        
        logger.debug(String.format("Got %s availalbe scopes", clientList.size()));
        
        return Response.ok(clientConverter.toScopesFromClientList(clientList)).build();
    }
}
