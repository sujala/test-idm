package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User Application Roles Resource.
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component("applicationTenantsResource")
public class ApplicationTenantsResource extends ParentResource {

	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final TenantService tenantService;

	@Autowired
	public ApplicationTenantsResource(
			ApplicationService applicationService,
			AuthorizationService authorizationService,
			RolesConverter rolesConverter,
    		TenantService tenantService, InputValidator inputValidator) {
		
		super(inputValidator);
		this.applicationService = applicationService;
		this.authorizationService = authorizationService;
		this.tenantService = tenantService;
		this.rolesConverter = rolesConverter;
	}

	@GET
	@Path("roles")
	public Response getAllTenantRolesForApplication(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId,
			@QueryParam("applicationId") String provisionedApplicationId,
			@QueryParam("tenantId") String tenantId) {

		authorizationService.verifyIdmSuperAdminAccess(authHeader);
		
		FilterBuilder filterBuilder = createFilterBuilder();
		filterBuilder.addFilter(FilterParamName.APPLICATION_ID, provisionedApplicationId);
		filterBuilder.addFilter(FilterParamName.TENANT_ID, tenantId);

		Application application = applicationService.loadApplication(applicationId);

		List<TenantRole> tenantRoles = tenantService.getTenantRolesForApplication(application, filterBuilder.getFilters());

		return Response.ok(rolesConverter.toRoleJaxbFromTenantRole(tenantRoles)).build();

	}
}
