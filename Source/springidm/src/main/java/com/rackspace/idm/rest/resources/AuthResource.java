package com.rackspace.idm.rest.resources;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.jaxb.Credentials;
import com.rackspace.idm.jaxb.MossoCredentials;
import com.rackspace.idm.jaxb.NastCredentials;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * Backward Compatible Auth Methods
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class AuthResource {

    private AccessTokenService accessTokenService;
    private UserService userService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired
    public AuthResource(UserService userService, UserConverter userConverter,
        AuthorizationService authorizationService,
        AccessTokenService accessTokenService, LoggerFactoryWrapper logger) {
        this.userConverter = userConverter;
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.accessTokenService = accessTokenService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an Access Token for Auth with Username and Api Key
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param creds User Credentials
     */
    @POST
    public Response getUsernameAuth(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, Credentials creds) {
        
        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }
        
        boolean authenticated = this.userService.authenticateWithApiKey(creds.getUsername(), creds.getKey());
        
        if (!authenticated) {
            String errorMsg = String.format("", creds.getUsername());
            logger.error(errorMsg);
            throw new NotAuthenticatedException(errorMsg);
        }
        
        User user = this.userService.getUser(creds.getUsername());
        

        return Response.noContent().build();
    }

    /**
     * Gets an Access Token for Auth with MossoId and Api Key
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param creds Mosso Credentials
     */
    @POST
    @Path("mosso")
    public Response getMossoAuth(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, MossoCredentials creds) {
        
        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        return Response.noContent().build();
    }

    /**
     * Gets an Access Token for Auth with NastId and Api Key
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param creds Nast Credentials
     */
    @Path("nast")
    @POST
    public Response getNastAuth(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, NastCredentials creds) {
        
        AccessToken token = this.accessTokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        return Response.noContent().build();
    }
}
