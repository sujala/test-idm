package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
     */
    @PUT
    public Response provisionApplicationForApplication(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("provisionedApplicationId") String provisionedApplicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().info("Provisioning application {} for application {}", provisionedApplicationId, applicationId);

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
    public Response removeApplicationFromUser(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("provisionedApplicationId") String provisionedApplicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().info("Removing application {} from application {}", applicationId, provisionedApplicationId);

        Application application = this.applicationService.loadApplication(applicationId);
        
        Application provisionedApplication = this.applicationService.loadApplication(provisionedApplicationId);

        this.scopeAccessService.deleteScopeAccessesForParentByApplicationId(application.getUniqueId(), provisionedApplication.getClientId());
        
        getLogger().info("Removed application {} from application {}", applicationId, provisionedApplicationId);

        return Response.noContent().build();
    }
}
