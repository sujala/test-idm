package com.rackspace.idm.api.resource.customer.user.token;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.api.resource.token.TokenResource;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserTokenResource {

    private final ScopeAccessService scopeAccessService;
    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final TokenConverter tokenConverter;
    private final AuthConverter authConverter;
    final private Logger logger = LoggerFactory.getLogger(UserTokenResource.class);
    
    @Autowired
    public UserTokenResource(ScopeAccessService scopeAccessService, UserService userService, AuthorizationService authorizationService,
        TokenConverter tokenConverter, AuthConverter authConverter) {
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.tokenConverter = tokenConverter;
        this.authConverter = authConverter;
    }
    
    /**
     * Gets a list of tokens for a user
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}tokens
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameConflict
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    public Response getTokens(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        
        User user = userService.checkAndGetUser(customerId, username);
        
        List<DelegatedClientScopeAccess> scopeAccessList = this.scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());
        
        return Response.ok(tokenConverter.toTokensJaxb(scopeAccessList)).build();
    }
    
    /**
     * Get token details for a user
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}token
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.409.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameConflict
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    @Path("{tokenString}")
    public Response getTokenDetails(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username, @PathParam("tokenId") String tokenString) {
        
        logger.debug("Validating Access Token: {}", tokenString);

        ScopeAccess authToken = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Rackers, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authToken)
            || authorizationService.authorizeRackspaceClient(authToken)
            || authorizationService.authorizeClient(authToken,
                request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, authToken);
        
        User user = userService.checkAndGetUser(customerId, username);
        
        DelegatedClientScopeAccess delegatedScopeAccess = this.scopeAccessService.getDelegatedScopeAccessByAccessToken(user, tokenString);

     // Validate Token exists and is valid
        if (delegatedScopeAccess == null) {
            String errorMsg = String
                .format("Token not found : %s", tokenString);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (delegatedScopeAccess instanceof hasAccessToken) {
            boolean expired = ((hasAccessToken) delegatedScopeAccess)
                .isAccessTokenExpired(new DateTime());
            if (expired) {
                String errorMsg = String.format("Token expired : %s",
                    tokenString);
                logger.warn(errorMsg);
                throw new NotFoundException(errorMsg);
            }
        }
        logger.debug("Delegated Access Token Found: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(delegatedScopeAccess)).build();
    }
}
