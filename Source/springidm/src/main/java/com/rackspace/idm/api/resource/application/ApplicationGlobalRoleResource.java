package com.rackspace.idm.api.resource.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;

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
     * @param userId userId
     * @param roleId roleId
     */
    @PUT
    public Response grantGlobalRoleToApplication(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("roleId") String roleId) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        
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
    public Response deleteGlobalRoleFromUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("applicationId") String applicationId,
        @PathParam("roleId") String roleId) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        
        Application application = this.applicationService.loadApplication(applicationId);              

    	TenantRole tenantRole = this.tenantService.getTenantRoleForParentById(application.getUniqueId(), roleId);

		this.tenantService.deleteTenantRole(application.getUniqueId(), tenantRole);

        return Response.noContent().build();
    }

   }
