package com.rackspace.idm.api.resource.roles;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User Application Roles Resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RolesResource extends ParentResource {

    private final ScopeAccessService scopeAccessService;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;
    private final RoleResource roleResource;

    @Autowired
    public RolesResource(RolesConverter rolesConverter,
                         AuthorizationService authorizationService,
                         ApplicationService applicationService, ScopeAccessService scopeAccessService,
                         RoleResource roleResource, InputValidator inputValidator) {

        super(inputValidator);
        this.applicationService = applicationService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.roleResource = roleResource;
        this.rolesConverter = rolesConverter;
    }

    /**
     * Gets a list of all the roles
     *
     * @param authHeader    HTTP Authorization header for authenticating the caller.
     * @param name          name
     * @param applicationId applicationId
     */
    @GET
    public Response getAllRoles(@Context Request request,
                                @Context UriInfo uriInfo,
                                @HeaderParam("X-Auth-Token") String authHeader,
                                @QueryParam("name") String name,
                                @QueryParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        List<FilterParam> filters = new ArrayList<FilterParam>();
        if (!StringUtils.isBlank(applicationId)) {
            filters.add(new FilterParam(FilterParamName.APPLICATION_ID, applicationId));
        }

        if (!StringUtils.isBlank(name)) {
            filters.add(new FilterParam(FilterParamName.ROLE_NAME, name));
        }

        List<ClientRole> roles = applicationService.getAllClientRoles(filters.toArray(new FilterParam[]{}));

        return Response.ok(rolesConverter.toRoleJaxbFromClientRole(roles)).build();
    }

    /**
     * Create a new role
     *
     * @param request
     * @param uriInfo
     * @param authHeader
     * @param holder
     */
    @POST
    public Response addRole(@Context Request request, @Context UriInfo uriInfo,
                            @HeaderParam("X-Auth-Token") String authHeader,
                            EntityHolder<com.rackspace.api.idm.v1.Role> holder) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        
        validateRequestBody(holder);

        ClientRole role = rolesConverter.toClientRole(holder.getEntity());

        applicationService.addClientRole(role);

        String locationUri = role.getId();

        return Response.created(URI.create(locationUri)).build();
    }

    @Path("{roleId}")
    public RoleResource getRoleResource() {
        return roleResource;
    }
}
