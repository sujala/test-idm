package com.rackspace.idm.api.resource.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.ApplicationSecretCredentials;
import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

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
    private final ScopeAccessService scopeAccessService;
    private final ApplicationConverter applicationConverter;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();

    @Autowired
    public ApplicationResource(
    	ApplicationTenantsResource tenantsResource,
    	ApplicationGlobalRolesResource globalRolesResource,
        ProvisionedApplicationsResource customerClientServicesResource,
        ApplicationService clientService, ScopeAccessService scopeAccessService,
        ApplicationConverter clientConverter,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
    	
    	this.tenantsResource = tenantsResource;
    	this.globalRolesResource = globalRolesResource;
        this.provisionedApplicationsResource = customerClientServicesResource;
        this.applicationService = clientService;
        this.scopeAccessService = scopeAccessService;
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
    public Response getApplication(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {
    	
        getLogger().debug("Getting Application: {}", applicationId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Racker's, Rackspace Clients, Specific Clients, Admins and Users are
        // authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        Application application = this.applicationService.loadApplication(applicationId);

        getLogger().debug("Got Application: {}", application);

        return Response.ok(applicationConverter
            .toClientJaxbWithoutPermissionsOrCredentials(application)).build();
    }

    /**
     * Update an application.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId  application ID
     * @param application Updated application
     */
    @PUT
    public Response updateApplication(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        EntityHolder<com.rackspace.api.idm.v1.Application> holder) {
    	
    	validateRequestBody(holder);
        
        getLogger().info("Updating application: {}", applicationId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Racker's, Specific Clients, Admins and IdM are authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        
        Application applicationWithUpdates = applicationConverter.toClientDO(holder.getEntity());
        Application application = this.applicationService.loadApplication(applicationId);
        application.copyChanges(applicationWithUpdates);

        this.applicationService.updateClient(application);
        
        getLogger().info("Udpated application: {}", applicationId);

        return Response.ok(applicationConverter
            .toClientJaxbWithoutPermissionsOrCredentials(application)).build();
    }
    
    /**
     * Delete an application.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId   application ID
     */
    @DELETE
    public Response deleteApplication(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {

        getLogger().info("Deleting Application: {}", applicationId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Only Specific Clients are authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        // Load the application to ensure that it exists first, before
        // attempting to delete it
        Application client = this.applicationService.loadApplication(applicationId);

        getLogger().debug("Got Application: {}", client);

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
    public Response resetApplicationSecretCredential(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId) {

        try {
        	  ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
              // Rackers, Admins and specific clients are authorized
              // TODO: Implement authorization rules
              // authorizationService.authorizeToken(token, uriInfo);

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
