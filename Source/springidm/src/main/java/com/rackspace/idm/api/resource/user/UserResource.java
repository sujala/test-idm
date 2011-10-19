package com.rackspace.idm.api.resource.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * A User.
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class UserResource extends ParentResource {

	private final UserApplicationsResource userApplicationsResource;
	private final ScopeAccessService scopeAccessService;
	private final UserPasswordCredentialsResource userCredentailResource;
	private final UserTenantsResource tenantsResource;
	private final UserSecretResource userSecretResource;
	private final UserDelegatedRefreshTokensResource delegatedRefreshTokenResource;
	private final UserService userService;
	private final UserConverter userConverter;
	private final AuthorizationService authorizationService;
	private final UserGlobalRolesResource globalRolesResource;

	@Autowired
	public UserResource(UserApplicationsResource userApplicationsResource,
			ScopeAccessService scopeAccessService,
			UserPasswordCredentialsResource userPasswordResource,
			UserTenantsResource tenantsResource,
			UserSecretResource userSecretResource,
			UserDelegatedRefreshTokensResource userTokenResource,
			UserService userService, UserConverter userConverter,
			InputValidator inputValidator,
			AuthorizationService authorizationService,
			UserGlobalRolesResource globalRolesResource) {

		super(inputValidator);

		this.userApplicationsResource = userApplicationsResource;
		this.scopeAccessService = scopeAccessService;
		this.userCredentailResource = userPasswordResource;
		this.tenantsResource = tenantsResource;
		this.userSecretResource = userSecretResource;
		this.delegatedRefreshTokenResource = userTokenResource;
		this.userService = userService;
		this.userConverter = userConverter;
		this.authorizationService = authorizationService;
		this.globalRolesResource = globalRolesResource;
	}

	/**
	 * Gets a user by Id.
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param username
	 *            username
	 */
	@GET
	public Response getUserById(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId) {

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);

		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);
        //authroizationService.authorize(token, Entity.create(userId), "1");
        
		getLogger().debug("Getting User: {}", userId);
		User user = this.userService.loadUser(userId);
		getLogger().debug("Got User :{}", user);

		com.rackspace.api.idm.v1.User returnedUser = userConverter
				.toUserJaxbWithoutAnyAdditionalElements(user);

		return Response.ok(returnedUser).build();
	}

	/**
	 * Updates a user.
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId
	 *            userId
	 */
	@PUT
	public Response updateUser(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			EntityHolder<com.rackspace.api.idm.v1.User> holder) {

		validateRequestBody(holder);

		ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);
		
		com.rackspace.api.idm.v1.User inputUser = holder.getEntity();
		User updatedUser = userConverter.toUserDO(inputUser);

		getLogger().debug("Updating User: {}", inputUser.getUsername());
		User user = this.userService.loadUser(userId);
		user.copyChanges(updatedUser);
		validateDomainObject(user);

		this.userService.updateUser(user, false);
		
		getLogger().debug("Updated User: {}", user);
		
		com.rackspace.api.idm.v1.User returnedUser = userConverter.toUserJaxbWithoutAnyAdditionalElements(user);
		
		return Response.ok(returnedUser).build();
	}

	/**
	 * Deletes a user.
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId userId
	 */
	@DELETE
	public Response deleteUser(@Context Request request,
			@Context UriInfo uriInfo,
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId) {

		getLogger().debug("Deleting User :{}", userId);

		ScopeAccess token = this.scopeAccessService
				.getAccessTokenByAuthHeader(authHeader);

		// TODO: Implement authorization rules
		// authorizationService.authorizeToken(token, uriInfo);

		User user = this.userService.loadUser(userId);
		
		this.userService.deleteUser(user);

		getLogger().debug("Deleted User: {}", user);

		return Response.noContent().build();
	}

	@Path("secret")
	public UserSecretResource getUserSecretResource() {
		return userSecretResource;
	}

	@Path("passwordcredentials")
	public UserPasswordCredentialsResource getPasswordCredentialsResource() {
		return userCredentailResource;
	}

	@Path("delegatedrefreshtokens")
	public UserDelegatedRefreshTokensResource getUserTokenResource() {
		return delegatedRefreshTokenResource;
	}

	@Path("applications")
	public UserApplicationsResource getApplications() {
		return userApplicationsResource;
	}

	@Path("tenants")
	public UserTenantsResource getTenantsResource() {
		return tenantsResource;
	}

	@Path("roles")
	public UserGlobalRolesResource getGlobalRolesResource() {
		return globalRolesResource;
	}
}
