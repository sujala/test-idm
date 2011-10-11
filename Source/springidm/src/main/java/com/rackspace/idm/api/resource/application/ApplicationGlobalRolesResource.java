package com.rackspace.idm.api.resource.application;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationGlobalRolesResource")
public class ApplicationGlobalRolesResource {

    private final ScopeAccessService scopeAccessService;
    private final TenantService tenantService;
    private final ApplicationService clientService;
    private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final ApplicationGlobalRoleResource roleResource;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ApplicationGlobalRolesResource(TenantService tenantService,
        AuthorizationService authorizationService, ApplicationService clientService,
        ScopeAccessService scopeAccessService,
        ApplicationGlobalRoleResource roleResource, RolesConverter rolesConverter) {
        this.clientService = clientService;
        this.tenantService = tenantService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.roleResource = roleResource;
        this.rolesConverter = rolesConverter;
    }

    /**
     * Gets a list of the global roles this application has.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param applicationId applicationId
     */
    @GET
    public Response getRoles(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("applicationId") String applicationId, 
        @QueryParam("applicationId") String provisionedApplicationId) {

        logger.debug("Getting roles for Application: {}", applicationId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        Application application = clientService.loadApplication(applicationId);
        
    	FilterParam[] filters = null;
    	if (!StringUtils.isBlank(provisionedApplicationId)) {
    		filters = new FilterParam[] { new FilterParam(FilterParamName.APPLICATION_ID, applicationId)};
    	}
       
        List<TenantRole> tenantRoles = this.tenantService.getGlobalRolesForApplication(application, filters);
        
        com.rackspace.api.idm.v1.Roles returnRoles = rolesConverter.toRoleJaxbFromTenantRole(tenantRoles);

        return Response.ok(returnRoles).build();
    }
    
    @Path("{roleId}")
    public ApplicationGlobalRoleResource getRoleResource() {
        return roleResource;
    }
}
