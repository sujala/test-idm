package com.rackspace.idm.api.resource.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserRecoveryTokenResource extends ParentResource {
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final TokenConverter tokenConverter;
    
    @Autowired
    public UserRecoveryTokenResource(ScopeAccessService scopeAccessService,
        UserService userService,
        TokenConverter tokenConverter,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
    }

   
    /**
     * Gets a token restricted to resetting a user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId   userId
     */
    @GET
    public Response getPasswordResetToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        User user = this.userService.loadUser(userId);

        PasswordResetScopeAccess prsa = this.scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user);

        getLogger().debug("Got Password Reset Token for User :{}", user);

        return Response.ok(tokenConverter.toTokenJaxb(prsa.getAccessTokenString(),prsa.getAccessTokenExp())).build();
    }
}
