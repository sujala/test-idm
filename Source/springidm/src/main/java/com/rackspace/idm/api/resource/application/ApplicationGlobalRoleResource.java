package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationGlobalRoleResource")
public class ApplicationGlobalRoleResource {

    private final ScopeAccessService scopeAccessService;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    private final TenantService tenantService;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ApplicationGlobalRoleResource(
        AuthorizationService authorizationService, ApplicationService applicationService,
        ScopeAccessService scopeAccessService, TenantService tenantService) {
        this.applicationService = applicationService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.tenantService = tenantService;
    }

    /**
     * Grant a global role for an application
     * 
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId applicationId
     * @param roleId roleId
     */
    @PUT
    public Response grantGlobalRoleToApplication(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        
		// TODO: Refactor. This logic should be in the tenant role service
        Application application = this.applicationService.loadApplication(applicationId); 
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

		this.tenantService.addTenantRoleToClient(application, tenantRole);

        return Response.noContent().build();
    }
    
    /**
     * Revoke a global role from an application
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId applicationId
     * @param roleId roleId
     */
    @DELETE
    public Response deleteGlobalRoleFromApplication(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("applicationId") String applicationId,
            @PathParam("roleId") String roleId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        
        Application application = this.applicationService.loadApplication(applicationId);

        if(application==null){
            throw new BadRequestException("Application with id: " + applicationId + " not found.");
        }

    	TenantRole tenantRole = this.tenantService.getTenantRoleForParentById(application.getUniqueId(), roleId);
        if(tenantRole==null){
            throw new NotFoundException("Role with id: " + roleId + " not found.");
        }
		this.tenantService.deleteTenantRole(application.getUniqueId(), tenantRole);

        return Response.noContent().build();
    }

	/**
	 * Grant a role to an application on a tenant.
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
	@PUT
	@Path("tenants/{tenantId}")
	public Response grantTenantRoleToApplication(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

		authorizationService.verifyIdmSuperAdminAccess(authHeader);

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
	@Path("tenants/{tenantId}")
	public Response deleteTenantRoleFromApplication(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("applicationId") String applicationId,
			@PathParam("tenantId") String tenantId,
			@PathParam("roleId") String roleId) {

		authorizationService.verifyIdmSuperAdminAccess(authHeader);

		Application application = applicationService.loadApplication(applicationId);

		TenantRole tenantRole = createTenantRole(tenantId, roleId);
		
		this.tenantService.deleteTenantRole(application.getUniqueId(), tenantRole);

		return Response.noContent().build();
	}

	private TenantRole createTenantRole(String tenantId, String roleId) {
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
}
