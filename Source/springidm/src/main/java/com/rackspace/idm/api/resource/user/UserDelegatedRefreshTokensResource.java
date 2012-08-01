package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserDelegatedRefreshTokensResource {

    private final ScopeAccessService scopeAccessService;
    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final TokenConverter tokenConverter;
    
    private final Logger logger = LoggerFactory.getLogger(UserDelegatedRefreshTokensResource.class);

    @Autowired
    public UserDelegatedRefreshTokensResource(ScopeAccessService scopeAccessService,
        UserService userService, AuthorizationService authorizationService,
        TokenConverter tokenConverter) {
    	
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.tokenConverter = tokenConverter;
    }

    /**
     * Gets a list of delegated refresh tokens for a user. These is a list of refresh tokens for
     * third party clients on behalf of this user
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     */
    @GET
    public Response getTokens(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        User user = this.userService.loadUser(userId);

        List<DelegatedClientScopeAccess> scopeAccessList = this.scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());

        return Response.ok(tokenConverter.toDelegatedTokensJaxb(scopeAccessList)).build();
    }

    /**
     * Get details of the token delegated to a user. 
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param tokenString token to be deleted
     */
    @GET
    @Path("{tokenString}")
    public Response getTokenDetails(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @PathParam("tokenString") String tokenString) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        logger.debug("Validating Access Token: {}", tokenString);


        User user = this.userService.loadUser(userId);

        DelegatedClientScopeAccess delegatedScopeAccess = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, tokenString);

        // Validate Token exists and is valid
        if (delegatedScopeAccess == null) {
            String errorMsg = String.format("Token not found : %s", tokenString);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Delegated Access Token Found: {}", tokenString);

        List<Permission> permsForToken = this.scopeAccessService.getPermissionsForParent(delegatedScopeAccess.getUniqueId());

        return Response.ok(tokenConverter.toDelegatedTokenJaxb(delegatedScopeAccess,permsForToken)).build();
    }

    /**
     * Delete the client access token that has been delegated to the user.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param tokenString token to be deleted
     */
    @DELETE
    @Path("{tokenString}")
    public Response deleteToken(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @PathParam("tokenString") String tokenString) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        logger.debug("Validating Access Token: {}", tokenString);

        User user = userService.loadUser(userId);

        this.scopeAccessService.deleteDelegatedToken(user, tokenString);

        logger.debug("Deleted Delegated Access Token Found: {}", tokenString);

        return Response.noContent().build();
    }
}
