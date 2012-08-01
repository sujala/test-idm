package com.rackspace.idm.api.resource.application;

import com.rackspace.api.idm.v1.ApplicationSecretCredentials;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Client application resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationApplicationResource")
public class ApplicationResource extends ParentResource {

	private final ApplicationTenantsResource tenantsResource;
	private final ApplicationGlobalRolesResource globalRolesResource;
    private final ProvisionedApplicationsResource provisionedApplicationsResource;
    private final ApplicationConverter applicationConverter;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();

    @Autowired
    public ApplicationResource(
    	ApplicationTenantsResource tenantsResource,
    	ApplicationGlobalRolesResource globalRolesResource,
        ProvisionedApplicationsResource customerClientServicesResource,
        ApplicationService clientService,
        ApplicationConverter clientConverter,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
    	
    	this.tenantsResource = tenantsResource;
    	this.globalRolesResource = globalRolesResource;
        this.provisionedApplicationsResource = customerClientServicesResource;
        this.applicationService = clientService;
        this.applicationConverter = clientConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets an application.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId application ID
     */
    @GET
    public Response getApplication(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
    	
        getLogger().debug("Getting Application: {}", applicationId);

        Application application = this.applicationService.loadApplication(applicationId);

        getLogger().debug("Got Application: {}", application);

        return Response.ok(applicationConverter.toClientJaxbWithoutPermissionsOrCredentials(application)).build();
    }

    /**
     * Update an application.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId  application ID
     * @param application Updated application
     */
    @PUT
    public Response updateApplication(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        EntityHolder<com.rackspace.api.idm.v1.Application> application) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
    	
    	validateRequestBody(application);
        
        getLogger().info("Updating application: {}", applicationId);
        
        Application applicationWithUpdates = applicationConverter.toClientDO(application.getEntity());
        if(applicationWithUpdates!=null && !applicationWithUpdates.getClientId().equals(applicationId)){
            throw new BadRequestException("Application id in uri and body do not match");
        }
        Application applicationDO = this.applicationService.loadApplication(applicationId);
        applicationDO.copyChanges(applicationWithUpdates);

        this.applicationService.updateClient(applicationDO);
        
        getLogger().info("Udpated application: {}", applicationId);

        return Response.ok(applicationConverter.toClientJaxbWithoutPermissionsOrCredentials(applicationDO)).build();
    }
    
    /**
     * Delete an application.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId   application ID
     */
    @DELETE
    public Response deleteApplication(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().info("Deleting Application: {}", applicationId);

        this.applicationService.delete(applicationId);

        getLogger().info("Deleted Application: {}", applicationId);

        return Response.noContent().build();
    }

    /**
     * Reset the application secret credentials
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId application ID
     */
    @Path("secretcredentials")
    @POST
    public Response resetApplicationSecretCredential(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        try {
              Application application = this.applicationService.loadApplication(applicationId);
              getLogger().debug("Got Application: {}", application);
              
              ClientSecret clientSecret = applicationService.resetClientSecret(application);
              ApplicationSecretCredentials applicationCredentials = new ApplicationSecretCredentials();
              applicationCredentials.setClientSecret(clientSecret.getValue());

              return Response.ok(objectFactory.createSecretCredentials(applicationCredentials)).build();
              
        } catch (IllegalStateException e) {
            String errorMsg = String.format(
                "Error generating secret for client: %s", applicationId);
            getLogger().warn(errorMsg);
            throw new IdmException(e);
        }
    }

    @Path("applications")
    public ProvisionedApplicationsResource getProvisionedApplicationsResource() {
        return provisionedApplicationsResource;
    }

    @Path("tenants")
    public ApplicationTenantsResource getTenantsResource() {
        return tenantsResource;
    }
    
    @Path("roles")
    public ApplicationGlobalRolesResource getGlobalRolesResource() {
        return globalRolesResource;
    }
}
