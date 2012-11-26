package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class UserGlobalRoleResource {

	private final ScopeAccessService scopeAccessService;
	private final UserService userService;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;
	private final TenantService tenantService;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public UserGlobalRoleResource(UserService userService,
			AuthorizationService authorizationService,
			ApplicationService applicationService,
			ScopeAccessService scopeAccessService, TenantService tenantService) {
		this.applicationService = applicationService;
		this.userService = userService;
		this.scopeAccessService = scopeAccessService;
		this.authorizationService = authorizationService;
		this.tenantService = tenantService;
	}

    @Autowired
    private Configuration config;

	/**
	 * Grant a global role for a user
	 *
	 *
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param roleId
	 *            roleId
	 */
	@PUT
	public Response grantGlobalRoleToUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

		// TODO: Refactor. This logic should be in the tenant role service
		User user = this.userService.loadUser(userId);
		ClientRole role = this.applicationService.getClientRoleById(roleId);
		if (role == null) {
			String errMsg = String.format("Role %s not found", roleId);
			logger.warn(errMsg);
			throw new BadRequestException(errMsg);
		}

		TenantRole tenantRole = new TenantRole();
		tenantRole.setClientId(role.getClientId());
		tenantRole.setRoleRsId(role.getId());
		tenantRole.setName(role.getName());
		tenantRole.setUserId(userId);

        if (isIdentityRole(role.getName())) {
            List<TenantRole> tenantRoles = this.tenantService.getGlobalRolesForUser(user);
            if (tenantRoles != null && tenantRoles.size() > 0) {
                throw new BadRequestException("User already has global role assigned");
            }
        }
		this.tenantService.addTenantRoleToUser(user, tenantRole);

		return Response.noContent().build();
	}

	/**
	 * Revoke a global role from a user
	 *
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param roleId
	 *            roleId
	 */
	@DELETE
	public Response deleteGlobalRoleFromUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

		User user = userService.loadUser(userId);
		TenantRole tenantRole = tenantService.getTenantRoleForParentById(user.getUniqueId(), roleId);
		this.tenantService.deleteTenantRole(user.getUniqueId(), tenantRole);

		return Response.noContent().build();
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
    @Path("tenants/{tenantId}")
	public Response grantTenantRoleToUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

		User user = userService.loadUser(userId);

        Tenant tenant = tenantService.getTenant(tenantId);

        if(tenant==null){
            throw new BadRequestException("Tenant with id: " + tenantId + " not found.");
        }

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
    @Path("tenants/{tenantId}")
	public Response deleteTenantRoleFromUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

		User user = this.userService.loadUser(userId);

		TenantRole tenantRole = new TenantRole();
		tenantRole.setRoleRsId(roleId);
		tenantRole.setTenantIds(new String[]{tenantId});

		this.tenantService.deleteTenantRole(user.getUniqueId(), tenantRole);

		return Response.noContent().build();
	}

	TenantRole createTenantRole(String tenantId, String roleId) {
		ClientRole role = applicationService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        TenantRole tenantRole = new TenantRole();
        tenantRole.setClientId(role.getClientId());
        tenantRole.setRoleRsId(role.getId());
        tenantRole.setName(role.getName());
        tenantRole.setTenantIds(new String[]{tenantId});

        return tenantRole;
	}

    private boolean isIdentityRole(String roleName) {
        return (roleName.equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.userRole")));
    }

    public void setConfig(Configuration configuration) {
        this.config = configuration;
    }
}