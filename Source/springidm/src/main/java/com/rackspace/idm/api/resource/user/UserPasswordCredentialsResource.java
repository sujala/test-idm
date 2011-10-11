package com.rackspace.idm.api.resource.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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

import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordCredentials;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

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
    
    @Autowired
    public UserPasswordCredentialsResource(
    	ScopeAccessService scopeAccessService, UserService userService,
        PasswordConverter passwordConverter, UserRecoveryTokenResource recoveryTokenResource,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.passwordConverter = passwordConverter;
        this.recoveryTokenResource = recoveryTokenResource;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets the user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId   userId
     */
    @GET
    public Response getUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        User user = this.userService.loadUser(userId);
      
        Password password = user.getPasswordObj();

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
     * Sets a user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId   userId
     * @param userCredentials   The user's current password and new password.
     */
    @PUT
    public Response setUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        EntityHolder<com.rackspace.api.idm.v1.PasswordCredentials> holder) {
        
    	validateRequestBody(holder);
    	
        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);

        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        com.rackspace.api.idm.v1.PasswordCredentials userCred = holder.getEntity();
        PasswordCredentials passwordCredentialsDO = passwordConverter.toPasswordCredentialsDO(userCred);

        userService.setUserPassword(userId, passwordCredentialsDO, token);

        return Response.noContent().build();
    }
    /**
     * Resets a user's password credentials.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId   userId
     */
    @POST
    public Response resetUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {
    	
        getLogger().debug("Reseting Password for User: {}", userId);

        ScopeAccess token = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        User user =  userService.loadUser(userId);
        Password password = userService.resetUserPassword(user);

        getLogger().debug("Updated password for user: {}", user);
        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    @Path("recoverytoken")
    public UserRecoveryTokenResource getRecoveryTokenResource() {
        return recoveryTokenResource;
    }
}
