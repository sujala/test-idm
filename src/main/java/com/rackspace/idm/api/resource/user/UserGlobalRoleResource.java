package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.RolePrecedenceValidator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * User Application Roles Resource.
 *
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class UserGlobalRoleResource {

    @Autowired
	private ScopeAccessService scopeAccessService;
    @Autowired
	private UserService userService;
    @Autowired
	private ApplicationService applicationService;
    @Autowired
	private AuthorizationService authorizationService;
    @Autowired
	private TenantService tenantService;
    @Autowired
    private RolePrecedenceValidator precedenceValidator;
    @Autowired
    private Configuration config;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

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
        ScopeAccess scopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authHeader);

		// TODO: Refactor. This logic should be in the tenant role service
		ClientRole role = this.applicationService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        User user = this.userService.loadUser(userId);

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller, role);
        }

        if (StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
            ClientRole userIdentityRole = applicationService.getUserIdentityRole(user, getCloudAuthClientId(), getIdentityRoleNames());
            if (userIdentityRole != null) {
                throw new BadRequestException("A user cannot have more than one identity:* role");
            }
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
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authHeader);

        User user = this.userService.loadUser(userId);

        TenantRole tenantRole = this.tenantService.getTenantRoleForUserById(user, roleId);
        if (tenantRole == null) {
            throw new NotFoundException(String.format("Role with id: %s not found", roleId));
        }

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller, tenantRole);

            if (user.getId().equals(caller.getId()) && StringUtils.startsWithIgnoreCase(tenantRole.getName(), "identity:")) {
                throw new BadRequestException("A user cannot delete their own identity role");
            }
        }

        this.tenantService.deleteTenantRoleForUser(user, tenantRole);

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
        if (tenant == null) {
            throw new BadRequestException("Cannot add tenant role to user on tenant");
        }
        TenantRole tenantRole = checkAndGetTenantRole(tenantId, roleId);
        if (tenantRole == null) {
            throw new BadRequestException(String.format("Cannot find role %s to add", roleId));
        }

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller, tenantRole);
        }

        List<String> identityRoleNames = getIdentityRoleNames();

        if (identityRoleNames.contains(tenantRole.getName())) {
            throw new BadRequestException("Cannot add identity roles to tenant.");
        }

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

        TenantRole tenantRole = tenantService.getTenantRoleForUserById(user, roleId);
        if (tenantRole == null) {
            throw new NotFoundException("Tenant Role not found");
        }

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedence(caller, tenantRole);
        }
		
		this.tenantService.deleteTenantRoleForUser(user, tenantRole);

		return Response.noContent().build();
	}

	TenantRole checkAndGetTenantRole(String tenantId, String roleId) {
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

    private List<String> getIdentityRoleNames() {
        List<String> names = new ArrayList<String>();
        names.add(config.getString("cloudAuth.userRole"));
        names.add(config.getString("cloudAuth.userAdminRole"));
        names.add(config.getString("cloudAuth.adminRole"));
        names.add(config.getString("cloudAuth.serviceAdminRole"));
        return names;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    public void setPrecedenceValidator(RolePrecedenceValidator validator) {
        this.precedenceValidator = validator;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setScopeAccessService(DefaultScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setUserService(DefaultUserService userService) {
        this.userService = userService;
    }

    public void setApplicationService(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setAuthorizationService(DefaultAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setTenantService(DefaultTenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    private boolean isIdentityRole(String roleName) {
        return (roleName.equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
                    || roleName.equalsIgnoreCase(config.getString("cloudAuth.userRole")));
    }
}
