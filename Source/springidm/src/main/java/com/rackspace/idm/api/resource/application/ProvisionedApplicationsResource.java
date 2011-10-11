package com.rackspace.idm.api.resource.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;

/**
 * An applications provisioned applications
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class ProvisionedApplicationsResource extends ParentResource {
	
	private final ProvisionedApplicationResource provisionedApplicationResource;
	private final ScopeAccessService scopeAccessService;
	private final ApplicationConverter applicationConverter;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;

	@Autowired
	public ProvisionedApplicationsResource(
			CustomerService customerService,
			ScopeAccessService scopeAccessService,
			ApplicationConverter clientConverter, ApplicationService clientService,
			AuthorizationService authorizationService,
			ProvisionedApplicationResource provisionedApplicationResource,
			InputValidator inputValidator) {

		super(inputValidator);
		this.applicationService = clientService;
		this.scopeAccessService = scopeAccessService;
		this.applicationConverter = clientConverter;
		this.authorizationService = authorizationService;
		this.provisionedApplicationResource = provisionedApplicationResource;
	}

	/**
	 * Gets the applications that have been provisioned for an application
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param aplicationId
	 *            applicationId
	 */
	@GET
	public Response getApplicationsForApplication(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId) {

		getLogger().debug("Getting applications for application {}", applicationId);

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// Rackers can add any service to a user
		// Rackspace Clients can add their own service to a user
		// Specific Clients can add their own service to a user
		// Customer IdM can add any service to user
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);
		Application application = this.applicationService.loadApplication(applicationId);

		Applications applications = this.applicationService.getClientServices(application);

		getLogger().debug("Got applications for application {} - {}", applicationId, applications);

		return Response.ok(applicationConverter.toApplicationJaxbMin(applications)).build();
	}
    
    @Path("{provisionedApplicationId}")
    public ProvisionedApplicationResource getProvisionedApplicationResource() {
        return provisionedApplicationResource;
    }
}
