package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.UserPasswordCredentialsValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User Password.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordCredentialsResource extends ParentResource {
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final PasswordConverter passwordConverter;
    private final UserRecoveryTokenResource recoveryTokenResource;
    private final AuthorizationService authorizationService;
    private final UserPasswordCredentialsValidator userPasswordCredentialsValidator;


    @Autowired
    public UserPasswordCredentialsResource(
            ScopeAccessService scopeAccessService, UserService userService,
            PasswordConverter passwordConverter, UserRecoveryTokenResource recoveryTokenResource,
            AuthorizationService authorizationService,
            InputValidator inputValidator, UserPasswordCredentialsValidator userPasswordCredentialsValidator) {

        super(inputValidator);
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.passwordConverter = passwordConverter;
        this.recoveryTokenResource = recoveryTokenResource;
        this.authorizationService = authorizationService;
        this.userPasswordCredentialsValidator = userPasswordCredentialsValidator;
    }

    /**
     * Gets the user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId     userId
     */
    @GET
    public Response getUserPassword(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("userId") String userId) {


        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        User user = this.userService.loadUser(userId);

        Password password = user.getPasswordObj();

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
     * Sets a user's password.
     *
     * @param authHeader      HTTP Authorization header for authenticating the caller.
     * @param userId          userId
     * @param userCredentials The user's current password and new password.
     */
    @PUT
    public Response setUserPassword(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("userId") String userId,
            EntityHolder<com.rackspace.api.idm.v1.UserPasswordCredentials> userCredentials) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        validateRequestBody(userCredentials);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);

        User user = this.userService.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        com.rackspace.api.idm.v1.UserPasswordCredentials userCred = userCredentials.getEntity();
        userPasswordCredentialsValidator.validateUserPasswordCredentials(userCred, user);
        user.setPassword(userCred.getNewPassword().getPassword());
        try {
            this.userService.updateUser(user, false);
        } catch (Exception e) {
            if(e instanceof IllegalStateException){
                throw new BadRequestException(e.getCause().getMessage(), e);
            }
        }
        return Response.noContent().build();

    }

    /**
     * Resets a user's password credentials.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId     userId
     */
    @POST
    public Response resetUserPassword(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("userId") String userId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().debug("Reseting Password for User: {}", userId);

        User user = userService.loadUser(userId);
        Password password = userService.resetUserPassword(user);

        getLogger().debug("Updated password for user: {}", user);
        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    @Path("recoverytoken")
    public UserRecoveryTokenResource getRecoveryTokenResource() {
        return recoveryTokenResource;
    }
}
