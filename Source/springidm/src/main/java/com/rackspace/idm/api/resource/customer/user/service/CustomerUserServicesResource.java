package com.rackspace.idm.api.resource.customer.user.service;

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

import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A users services
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerUserServicesResource {

    private final CustomerUserServiceResource customerUserServiceResource;
    private final ScopeAccessService scopeAccessService;
    private final ClientConverter clientConverter;
    private final ClientService clientService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerUserServicesResource(CustomerUserServiceResource customerUserServiceResource,
        ScopeAccessService scopeAccessService,
        ClientConverter clientConverter,
        ClientService clientService, UserService userService,
        AuthorizationService authorizationService) {

        this.customerUserServiceResource = customerUserServiceResource;
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.clientConverter = clientConverter;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    /**
     * Adds a service to the user.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}client
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     * @param client New Service.
     */
    @POST
    public Response addServiceToUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        EntityHolder<com.rackspace.idm.jaxb.Client> holder) {

        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }

        com.rackspace.idm.jaxb.Client inputClient = holder.getEntity();

        if (inputClient.getClientId() == null) {
            throw new BadRequestException("Client must contain a clientId");
        }

        logger.info("Adding service {} to user {}", inputClient.getClientId(),
            username);

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeRackspaceClient(token) && token
                .getClientId().equalsIgnoreCase(inputClient.getClientId()))
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && token.getClientId()
                .equalsIgnoreCase(inputClient.getClientId()))
            || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Client client = this.clientService.getById(inputClient.getClientId());

        if (client == null) {
            String errMsg = String.format("Client %s not found",
                inputClient.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        User user = this.userService.getUser(username);
        if (user == null) {
            String errMsg = String.format("User %s not found",
                username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setUsername(user.getUsername());
        sa.setUserRCN(user.getCustomerId());
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getCustomerId());

        this.scopeAccessService.addScopeAccess(user.getUniqueId(), sa);

        logger.info("Added service {} to user {}", inputClient.getClientId(),
            username);

        return Response.ok().build();
    }
    
    /**
     * Gets the services a user has.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}clients
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     * @param client New Client.
     */
    @GET
    public Response getServicesForUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting services for user {}", username);

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token) 
            || authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo)
            || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        
        User user = this.userService.getUser(username);
        if (user == null) {
            String errMsg = String.format("User %s not found",
                username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        Clients services = this.userService.getUserServices(user);

        logger.debug("Got services for user {} - {}", username, services);

        return Response.ok(clientConverter.toClientListJaxb(services)).build();
    }
    
    @Path("{serviceId}")
    public CustomerUserServiceResource getCustomerUserServiceResource() {
        return customerUserServiceResource;
    }
}
