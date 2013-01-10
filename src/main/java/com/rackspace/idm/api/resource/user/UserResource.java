package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;

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
    private UserValidatorFoundation userValidator;

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
	 * @param userId
	 *            userId
	 */
	@GET
	public Response getUserById(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        boolean isApplication = authorizationService.authorizeRackspaceClient(scopeAccess);
        //verify if caller is a rackspace client, idm client or super admin
        if(!isApplication){
            authorizationService.verifyIdmSuperAdminAccess(authHeader);
        }
        
		getLogger().debug("Getting User: {}", userId);
		User user = this.userService.loadUser(userId);
		getLogger().debug("Got User :{}", user);

		return Response.ok(userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();
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
	public Response updateUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId,
			EntityHolder<com.rackspace.api.idm.v1.User> holder) throws IOException, JAXBException {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        boolean isApplication = authorizationService.authorizeRackspaceClient(scopeAccess);
        //verify if caller is a rackspace client, idm client or super admin
        if(!isApplication){
            authorizationService.verifyIdmSuperAdminAccess(authHeader);
        }

        validateRequestBody(holder);

		com.rackspace.api.idm.v1.User inputUser = holder.getEntity();

        if(!inputUser.getUsername().isEmpty()){
            userValidator.validateUsername(inputUser.getUsername());
        }

		User updatedUser = userConverter.toUserDO(inputUser);

        if(updatedUser.isDisabled()){
            scopeAccessService.expireAllTokensForUser(updatedUser.getUsername());
        }

		getLogger().debug("Updating User: {}", inputUser.getUsername());
		User user = userService.loadUser(userId);
		user.copyChanges(updatedUser);
		validateDomainObject(user);

		userService.updateUserById(user, false);
		
		getLogger().debug("Updated User: {}", user);
		
		return Response.ok(userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();
	}

	/**
	 * Deletes a user.
	 * 
	 * @param authHeader
	 *            HTTP Authorization header for authenticating the caller.
	 * @param userId userId
	 */
	@DELETE
	public Response deleteUser(
			@HeaderParam("X-Auth-Token") String authHeader,
			@PathParam("userId") String userId) throws IOException, JAXBException {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        boolean isApplication = authorizationService.authorizeRackspaceClient(scopeAccess);
        //verify if caller is a rackspace client, idm client or super admin
        if(!isApplication){
            authorizationService.verifyIdmSuperAdminAccess(authHeader);
        }

        getLogger().debug("Deleting User :{}", userId);

		User user = this.userService.loadUser(userId);
		
		this.userService.softDeleteUser(user);

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

    public void setUserValidator(UserValidatorFoundation userValidator) {
        this.userValidator = userValidator;
    }
}
