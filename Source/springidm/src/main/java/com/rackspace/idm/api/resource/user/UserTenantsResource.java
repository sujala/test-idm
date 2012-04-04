package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
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

	private final UserService userService;
	private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final TenantService tenantService;

	@Autowired
	public UserTenantsResource(
			UserService userService,
			AuthorizationService authorizationService,
			RolesConverter rolesConverter,
    		TenantService tenantService, InputValidator inputValidator) {
		
		super(inputValidator);
		this.userService = userService;
		this.authorizationService = authorizationService;
		this.tenantService = tenantService;
		this.rolesConverter = rolesConverter;
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
}
