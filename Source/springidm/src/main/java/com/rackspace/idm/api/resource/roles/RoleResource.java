package com.rackspace.idm.api.resource.roles;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * User Application Roles Resource.
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class RoleResource extends ParentResource {

	private final RolesConverter rolesConverter;
	private final ScopeAccessService scopeAccessService;
	private final ApplicationService applicationService;
	private final AuthorizationService authorizationService;

	@Autowired
	public RoleResource(
			AuthorizationService authorizationService,
			ApplicationService applicationService, ScopeAccessService scopeAccessService,
			RolesConverter rolesConverter, InputValidator inputValidator) {
		
		super(inputValidator);
		this.applicationService = applicationService;
		this.scopeAccessService = scopeAccessService;
		this.authorizationService = authorizationService;
		this.rolesConverter = rolesConverter;
	}

	/**
	 * Get a role
	 * 
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param roleId
	 *            roleId
	 */
	@GET
	public Response getRole(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("roleId") String roleId) {

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);

		ClientRole clientRole = applicationService.getClientRoleById(roleId); 
		
		JAXBElement<com.rackspace.api.idm.v1.Role> jaxbRole = rolesConverter.toRoleJaxbFromClientRole(clientRole);

		return Response.ok(jaxbRole).build();
	}
	
	/**
	 * Delete a role
	 * 
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param roleId
	 *            roleId
	 */
	@DELETE
	public Response deleteRole(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("roleId") String roleId) {

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);

		ClientRole clientRole = applicationService.getClientRoleById(roleId); 
		
		applicationService.deleteClientRole(clientRole);
		
		return Response.noContent().build();
	}
	
	/**
	 * Updates a role
	 * 
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 * @param roleId
	 *            roleId
	 * @param role
	 */
	@PUT
	public Response updateRole(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("roleId") String roleId,
			EntityHolder<com.rackspace.api.idm.v1.Role> holder) {

		validateRequestBody(holder);
		
		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);
		ClientRole updatedRole = rolesConverter.toClientRole(holder.getEntity());
		ClientRole clientRole = applicationService.getClientRoleById(roleId); 
		
		clientRole.copyChanges(updatedRole);
		
		applicationService.updateClientRole(clientRole);
		
		return Response.noContent().build();
	}
}
