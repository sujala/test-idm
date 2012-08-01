package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationGlobalRolesResource")
public class ApplicationGlobalRolesResource {

    private final TenantService tenantService;
    private final ApplicationService clientService;
    private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final ApplicationGlobalRoleResource roleResource;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ApplicationGlobalRolesResource(TenantService tenantService,
        AuthorizationService authorizationService, ApplicationService clientService,
        ApplicationGlobalRoleResource roleResource, RolesConverter rolesConverter) {
        this.clientService = clientService;
        this.tenantService = tenantService;
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
    public Response getRoles(
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("applicationId") String applicationId, 
        @QueryParam("applicationId") String provisionedApplicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        logger.debug("Getting roles for Application: {}", applicationId);

        Application application = clientService.loadApplication(applicationId);
        
    	FilterParam[] filters = null;
    	if (!StringUtils.isBlank(provisionedApplicationId)) {
    		filters = new FilterParam[] { new FilterParam(FilterParamName.APPLICATION_ID, applicationId)};
    	}
       
        List<TenantRole> tenantRoles = this.tenantService.getGlobalRolesForApplication(application, filters);

        return Response.ok(rolesConverter.toRoleJaxbFromTenantRole(tenantRoles)).build();
    }
    
    @Path("{roleId}")
    public ApplicationGlobalRoleResource getRoleResource() {
        return roleResource;
    }
}
