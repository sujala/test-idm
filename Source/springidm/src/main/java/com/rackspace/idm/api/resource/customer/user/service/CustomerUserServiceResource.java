package com.rackspace.idm.api.resource.customer.user.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.customer.user.service.permission.UserPermissionsResource;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;

/**
 * A user service
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerUserServiceResource {

    private final UserPermissionsResource userPermissionsResource;
    private final ScopeAccessService scopeAccessService;
    private final ClientService clientService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public CustomerUserServiceResource(
        UserPermissionsResource userPermissionsResource,
        ScopeAccessService scopeAccessService,
        ClientService clientService, UserService userService,
        AuthorizationService authorizationService) {
        this.clientService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.userPermissionsResource = userPermissionsResource;
    }
    
    /**
     * Remove a service from a user
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     * @param serviceId  Service Id
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    public Response removeServiceFromUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("serviceId") String serviceId) {

        logger.info("Removing service {} from user {}", serviceId, username);

        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        boolean authorized = authorizationService.authorizeRacker(token)
            || (authorizationService.authorizeRackspaceClient(token) && token
                .getClientId().equalsIgnoreCase(serviceId))
            || (authorizationService.authorizeClient(token,
                request.getMethod(), uriInfo) && token.getClientId()
                .equalsIgnoreCase(serviceId))
            || authorizationService.authorizeCustomerIdm(token);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        Client client = this.clientService.getById(serviceId);

        if (client == null) {
            String errMsg = String.format("Client %s not found",
                serviceId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        User user = this.userService.checkAndGetUser(customerId, username);
        if (user == null) {
            String errMsg = String.format("User %s not found",
                username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccess service = this.scopeAccessService.getScopeAccessForParentByClientId(user.getUniqueId(), serviceId);
        
        if (service != null) {
            this.scopeAccessService.deleteScopeAccess(service);
        }

        logger.info("Removed service {} from user {}", serviceId, username);

        return Response.noContent().build();
    }
    
    @Path("permissions")
    public UserPermissionsResource getUserPermissionResource() {
        return userPermissionsResource;
    }
}
