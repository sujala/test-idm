package com.rackspace.idm.api.resource.roles;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RolePrecedenceValidator;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
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

    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    private final RolesConverter rolesConverter;

    @Autowired
    public RolesResource(RolesConverter rolesConverter, AuthorizationService authorizationService,
                         ApplicationService applicationService, InputValidator inputValidator) {
        super(inputValidator);
        this.applicationService = applicationService;
        this.authorizationService = authorizationService;
        this.rolesConverter = rolesConverter;
    }

    @Autowired
    private RolePrecedenceValidator precedenceValidator;
    @Autowired
    private Paginator<ClientRole> paginator;
    @Autowired
    private Configuration config;
    @Autowired
    private UserService userService;
    @Autowired
    private ScopeAccessService scopeAccessService;

    /**
     * Get a role
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param roleId     roleId
     */
    @GET
    @Path("{roleId}")
    public Response getRole(@HeaderParam("X-Auth-Token") String authHeader, @PathParam("roleId") String roleId) {
        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if(clientRole==null){
            throw new NotFoundException("Role with id: "+ roleId + " not found.");
        }
        JAXBElement<Role> jaxbRole = rolesConverter.toRoleJaxbFromClientRole(clientRole);
        return Response.ok(jaxbRole).build();
    }

    /**
     * Gets a list of all the roles
     *
     * @param authHeader    HTTP Authorization header for authenticating the caller.
     * @param name          name
     * @param applicationId applicationId
     */
    @GET
    public Response getAllRoles(
            @HeaderParam("X-Auth-Token") String authHeader,
            @Context UriInfo uriInfo,
            @QueryParam("name") String name,
            @QueryParam("applicationId") String applicationId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        PaginatorContext<ClientRole> context = applicationService.getClientRolesPaged(applicationId, name, 0, config.getInt("ldap.paging.limit.default"));

        String linkHeader = paginator.createLinkHeader(uriInfo, context);

        return Response.status(200)
                .header("Link", linkHeader)
                .entity(rolesConverter.toRoleJaxbFromClientRole(context.getValueList())).build();
    }

    /**
     * Create a new role
     *
     * @param role
     */
    @POST
    public Response addRole(@HeaderParam("X-Auth-Token") String authHeader, Role role) {
        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        validateRole(role);
        ClientRole clientRole = rolesConverter.toClientRole(role);
        clientRole.setRsWeight(config.getInt("cloudAuth.special.rsWeight"));
        applicationService.addClientRole(clientRole);
        String locationUri = clientRole.getId();
        return Response.created(URI.create(locationUri)).build();
    }

    /**
     * Updates a role
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param roleId     roleId
     */
    @PUT
    @Path("{roleId}")
    public Response updateRole(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("roleId") String roleId,
            Role role) {
        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        ScopeAccess scopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authHeader);

        validateRole(role);
        Application application = applicationService.getById(role.getApplicationId());
        if(application==null){
            throw new BadRequestException("Application with id: " + role.getApplicationId() + " not found.");
        }

        ClientRole updatedRole = rolesConverter.toClientRole(role);
        ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if(clientRole==null){
            throw new NotFoundException("Role with id: " + roleId + " not found.");
        }

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerRolePrecedence(caller, clientRole);
        }

        clientRole.copyChanges(updatedRole);
        applicationService.updateClientRole(clientRole);
        return Response.noContent().build();
    }

    /**
	 * Delete a role
	 *
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param roleId
	 *            roleId
	 */
	@DELETE
    @Path("{roleId}")
	public Response deleteRole(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("roleId") String roleId) {
		authorizationService.verifyIdmSuperAdminAccess(authHeader);
        ScopeAccess scopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authHeader);

		ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if(clientRole==null){
            throw new NotFoundException("role with id: " + roleId + " not found");
        }

        if (!(scopeAccess instanceof ClientScopeAccess)) {
            User caller = userService.getUserByAuthToken(authHeader);
            precedenceValidator.verifyCallerRolePrecedence(caller, clientRole);
        }

		applicationService.deleteClientRole(clientRole);
		return Response.noContent().build();
	}

    void validateRole(Role role){
        if(role==null){
           throw new BadRequestException("Role cannot be null");
        }
        if(StringUtils.isBlank(role.getName())){
            throw new BadRequestException("Role name is not valid");
        }
        if(StringUtils.isBlank(role.getApplicationId())){
            throw new BadRequestException("Application id is not valid");
        }
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setPaginator(Paginator<ClientRole> paginator) {
        this.paginator = paginator;
    }

    public void setPrecedenceValidator(RolePrecedenceValidator validator) {
        this.precedenceValidator = validator;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setScopeAccessService(ScopeAccessService service) {
        this.scopeAccessService = service;
    }
}
