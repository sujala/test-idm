package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * An applications provisioned applications
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class ProvisionedApplicationsResource extends ParentResource {
	
	private final ProvisionedApplicationResource provisionedApplicationResource;
	private final ApplicationConverter applicationConverter;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;

	@Autowired
	public ProvisionedApplicationsResource(
			ApplicationConverter clientConverter, ApplicationService clientService,
			AuthorizationService authorizationService,
			ProvisionedApplicationResource provisionedApplicationResource,
			InputValidator inputValidator) {

		super(inputValidator);
		this.applicationService = clientService;
		this.applicationConverter = clientConverter;
		this.authorizationService = authorizationService;
		this.provisionedApplicationResource = provisionedApplicationResource;
	}

	/**
	 * Gets the applications that have been provisioned for an application
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param applicationId
	 *            applicationId
	 */
	@GET
	public Response getApplicationsForApplication(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

		getLogger().debug("Getting applications for application {}", applicationId);

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
