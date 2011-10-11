package com.rackspace.idm.api.resource.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class ProvisionedApplicationResource extends ParentResource {

    private final ScopeAccessService scopeAccessService;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public ProvisionedApplicationResource(
        ScopeAccessService scopeAccessService, ApplicationService clientService,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.applicationService = clientService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
    }

    /**
     * Provision an application for an application.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId applicationId
     * @param provisionedApplicationId provisionedApplicationId
     * @param application New Application.
     */
    @PUT
    public Response provisionApplicationForApplication(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("provisionedApplicationId") String provisionedApplicationId) {

        getLogger().info("Provisioning application {} for application {}", provisionedApplicationId, applicationId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        Application application = this.applicationService.loadApplication(applicationId);
        Application provisionedApplication = this.applicationService.loadApplication(provisionedApplicationId);
        
        ClientScopeAccess sa = new ClientScopeAccess();
        sa.setClientId(provisionedApplication.getClientId());
        sa.setClientRCN(provisionedApplication.getRCN());

        this.scopeAccessService.addDirectScopeAccess(application.getUniqueId(), sa);

        getLogger().info("Provisioned application {} to application {}", provisionedApplicationId, applicationId);

        return Response.noContent().build();
    }
    
    /**
     * Remove an application from an application
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId applicationId
     * @param provisionedApplicationId provisionedApplicationId
     */
    @DELETE
    public Response removeApplicationFromUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("provisionedApplicationId") String provisionedApplicationId) {

        getLogger().info("Removing application {} from application {}", applicationId, provisionedApplicationId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        Application application = this.applicationService.loadApplication(applicationId);
        Application provisionedApplication = this.applicationService.loadApplication(provisionedApplicationId);

        this.scopeAccessService.deleteScopeAccessesForParentByApplicationId(application.getUniqueId(), provisionedApplication.getClientId());
        
        getLogger().info("Removed application {} from application {}", applicationId, provisionedApplicationId);

        return Response.noContent().build();
    }
}
