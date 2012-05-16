package com.rackspace.idm.api.resource.roles;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

        List<ClientRole> roles = applicationService.getAllClientRoles(filters);

        return Response.ok(rolesConverter.toRoleJaxbFromClientRole(roles)).build();
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
		ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if(clientRole==null){
            throw new NotFoundException("role with id: " + roleId + " not found");
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
        if(role.getApplicationId()==null || role.getApplicationId().isEmpty()){
            throw new BadRequestException("Application id is not valid");
        }
    }
}
