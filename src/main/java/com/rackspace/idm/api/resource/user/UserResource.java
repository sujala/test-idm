package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.AuthorizationContext;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A User.
 * 
 */
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Component
public class UserResource extends ParentResource {

	private final ScopeAccessService scopeAccessService;
	private final UserPasswordCredentialsResource userCredentailResource;
	private final UserService userService;
	private final UserConverter userConverter;
	private final AuthorizationService authorizationService;
	private final UserGlobalRolesResource globalRolesResource;

    @Autowired
    private UserValidatorFoundation userValidator;

	@Autowired
	public UserResource(
			ScopeAccessService scopeAccessService,
			UserPasswordCredentialsResource userPasswordResource,
			UserService userService, UserConverter userConverter,
			InputValidator inputValidator,
			AuthorizationService authorizationService,
			UserGlobalRolesResource globalRolesResource) {

		super(inputValidator);

		this.scopeAccessService = scopeAccessService;
		this.userCredentailResource = userPasswordResource;
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
        AuthorizationContext context = authorizationService.getAuthorizationContext(scopeAccess);
        boolean isApplication = authorizationService.authorizeRackspaceClient(context);
        //verify if caller is a rackspace client, idm client or super admin
        if(!isApplication){
            authorizationService.verifyIdmSuperAdminAccess(authHeader);
        }
        
		getLogger().debug("Getting User: {}", userId);
		User user = this.userService.loadUser(userId);
		getLogger().debug("Got User :{}", user);

		return Response.ok(userConverter.toUserJaxbWithoutAnyAdditionalElements(user)).build();
	}

	@Path("passwordcredentials")
	public UserPasswordCredentialsResource getPasswordCredentialsResource() {
		return userCredentailResource;
	}

	@Path("roles")
	public UserGlobalRolesResource getGlobalRolesResource() {
		return globalRolesResource;
	}

    public void setUserValidator(UserValidatorFoundation userValidator) {
        this.userValidator = userValidator;
    }
}
