package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.configuration.Configuration;
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
    private PrecedenceValidator precedenceValidator;
    @Autowired
    private Configuration config;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

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
        AuthorizationContext context = authorizationService.getAuthorizationContext(scopeAccess);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(context);

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
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, tenantRole);
        }

        List<String> identityRoleNames =  applicationService.getIdentityRoleNames();

        if (identityRoleNames.contains(tenantRole.getName())) {
            throw new BadRequestException("Cannot add identity roles to tenant.");
        }

        tenantService.addTenantRoleToUser(user, tenantRole);

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
        tenantRole.getTenantIds().add(tenantId);

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

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
