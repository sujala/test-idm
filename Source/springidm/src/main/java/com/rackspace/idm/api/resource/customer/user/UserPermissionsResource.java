package com.rackspace.idm.api.resource.customer.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * User Permission Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component

public class UserPermissionsResource {
    
    private AccessTokenService accessTokenService;
    private OAuthService oauthService;
    private UserService userService;
    private ClientService clientService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserPermissionsResource(OAuthService oauthService, AccessTokenService accessTokenService, UserService userService, 
        AuthorizationService authorizationService, ClientService clientService, UserConverter userConverter) {
        
        this.oauthService = oauthService;
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Gets a permission.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}user
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     */
    @GET
    public Response getPermission(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        AccessToken token = accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", token.getTokenString());
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        logger.debug("Got Permission :{}");
        return Response.ok().build();
    }

}
