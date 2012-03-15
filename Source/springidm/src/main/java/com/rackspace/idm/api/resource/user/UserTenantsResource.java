package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserTenantsResource extends ParentResource {

	private final ScopeAccessService scopeAccessService;
	private final UserService userService;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final TenantService tenantService;

	@Autowired
	public UserTenantsResource(ScopeAccessService scopeAccessService,
			UserService userService, ApplicationService applicationService,
			AuthorizationService authorizationService,
			RolesConverter rolesConverter,
    		TenantService tenantService, InputValidator inputValidator) {
		
		super(inputValidator);
		this.scopeAccessService = scopeAccessService;
		this.userService = userService;
		this.authorizationService = authorizationService;
		this.tenantService = tenantService;
		this.rolesConverter = rolesConverter;
		this.applicationService = applicationService;
	}

    /**
     * Gets all the roles on a tenant that this user has
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param applicationId applicationId
     * @param tenantId tenantId
     */
    @GET
    @Path("roles")
    public Response getAllTenantRolesForUser(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @QueryParam("tenantId") String tenantId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
		FilterBuilder filterBuilder = createFilterBuilder();
		filterBuilder.addFilter(FilterParamName.APPLICATION_ID, applicationId);
		filterBuilder.addFilter(FilterParamName.TENANT_ID, tenantId);
    	
        User user = this.userService.loadUser(userId);        
       
        List<TenantRole> tenantRoles = this.tenantService.getTenantRolesForUser(user, filterBuilder.getFilters());

        return Response.ok(rolesConverter.toRoleJaxbFromTenantRole(tenantRoles)).build();
    }
    
    /**
	 * Grant a role to a user on a tenant.
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
	public Response grantTenantRoleToUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

		User user = userService.loadUser(userId);

		TenantRole tenantRole = createTenantRole(tenantId, roleId);

	    tenantService.addTenantRoleToUser(user, tenantRole);

		return Response.noContent().build();
	}

	/**
	 * Revoke a role on a tenant from a user.
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
	@DELETE
    @Path("{tenantId}/roles/{roleId}")
	public Response deleteTenantRoleFromUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

		User user = this.userService.loadUser(userId);

		TenantRole tenantRole = new TenantRole();
		tenantRole.setRoleRsId(roleId);
		tenantRole.setTenantIds(new String[]{tenantId});
		
		this.tenantService.deleteTenantRole(user.getUniqueId(), tenantRole);

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
