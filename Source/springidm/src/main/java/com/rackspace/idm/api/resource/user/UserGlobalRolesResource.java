package com.rackspace.idm.api.resource.user;

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
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserGlobalRolesResource {

    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final TenantService tenantService;
    private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final UserGlobalRoleResource roleResource;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserGlobalRolesResource(UserService userService,
        AuthorizationService authorizationService, TenantService tenantService,
        ScopeAccessService scopeAccessService,
        UserGlobalRoleResource roleResource,
        RolesConverter rolesConverter) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.roleResource = roleResource;
        this.rolesConverter = rolesConverter;
    }

    /**
     * Gets a list of the global roles this user has.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     */
    @GET
    public Response getRoles(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, 
        @PathParam("userId") String userId, 
        @QueryParam("applicationId") String applicationId) {

        logger.debug("Getting global roles for User: {}", userId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
   
        User user = userService.loadUser(userId);
    	FilterParam[] filters = null;
    	if (!StringUtils.isBlank(applicationId)) {
    		filters = new FilterParam[] { new FilterParam(FilterParamName.APPLICATION_ID, applicationId)};
    	}
       
        List<TenantRole> tenantRoles = this.tenantService.getGlobalRolesForUser(user, filters);
        
        com.rackspace.api.idm.v1.Roles returnRoles = rolesConverter.toRoleJaxbFromTenantRole(tenantRoles);

        return Response.ok(returnRoles).build();
    }
    
    @Path("{roleId}")
    public UserGlobalRoleResource getRoleResource() {
        return roleResource;
    }
}
