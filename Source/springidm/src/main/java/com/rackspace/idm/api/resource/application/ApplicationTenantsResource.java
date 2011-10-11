package com.rackspace.idm.api.resource.application;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;

/**
 * User Application Roles Resource.
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component("applicationTenantsResource")
public class ApplicationTenantsResource extends ParentResource {

	private final ScopeAccessService scopeAccessService;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final TenantService tenantService;

	@Autowired
	public ApplicationTenantsResource(ScopeAccessService scopeAccessService,
			ApplicationService applicationService,
			AuthorizationService authorizationService,
			RolesConverter rolesConverter,
    		TenantService tenantService, InputValidator inputValidator) {
		
		super(inputValidator);
		this.scopeAccessService = scopeAccessService;
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

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);
		
		FilterBuilder filterBuilder = createFilterBuilder();
		filterBuilder.addFilter(FilterParamName.APPLICATION_ID, provisionedApplicationId);
		filterBuilder.addFilter(FilterParamName.TENANT_ID, tenantId);

		Application application = applicationService.loadApplication(applicationId);

		List<TenantRole> tenantRoles = 
			tenantService.getTenantRolesForApplication(application, filterBuilder.getFilters());

		com.rackspace.api.idm.v1.Roles returnRoles 
		      = rolesConverter.toRoleJaxbFromTenantRole(tenantRoles);

		return Response.ok(returnRoles).build();

	}

	/**
	 * Grant a role to an application on a tenant.
	 * 
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param tenantId
	 *            tenantId
	 * @param roleId
	 *            roleId
	 */
	@PUT
	@Path("{tenantId}/roles/{roleId}")
	public Response grantTenantRoleToApplication(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);

		Application client = this.applicationService.loadApplication(applicationId);

		TenantRole tenantRole = createTenantRole(tenantId, roleId);
		
		this.tenantService.addTenantRoleToClient(client, tenantRole);

		return Response.noContent().build();
	}

	/**
	 * Revoke a role on a tenant from an application.
	 * 
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param applicationId
	 *            applicationId
	 * @param tenantId
	 *            tenantId
	 * @param roleId
	 *            roleId
	 */
	@DELETE
	@Path("{tenantId}/roles/{roleId}")
	public Response deleteTenantRoleFromApplication(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);

		Application application = applicationService.loadApplication(applicationId);

		TenantRole tenantRole = createTenantRole(tenantId, roleId);
		
		this.tenantService.deleteTenantRole(application.getUniqueId(), tenantRole);

		return Response.noContent().build();
	}
	
	private TenantRole createTenantRole(String tenantId, String roleId) {
		ClientRole role = applicationService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            getLogger().warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        TenantRole tenantRole = new TenantRole();
        tenantRole.setClientId(role.getClientId());
        tenantRole.setRoleRsId(role.getId());
        tenantRole.setName(role.getName());
        tenantRole.setTenantIds(new String[]{tenantId});
        
        return tenantRole;
	}
}
