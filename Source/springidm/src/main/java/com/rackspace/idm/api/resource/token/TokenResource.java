package com.rackspace.idm.api.resource.token;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Management of OAuth 2.0 token used by IDM.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokenResource {
    private final OAuthService oauthService;
    private final AuthHeaderHelper authHeaderHelper;
    private final AuthConverter authConverter;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;
    final private Logger logger = LoggerFactory.getLogger(TokenResource.class);

    @Autowired(required = true)
    public TokenResource(OAuthService oauthService,
        AuthHeaderHelper authHeaderHelper, AuthConverter authConverter,
        AuthorizationService authorizationService,
        ScopeAccessService scopeAccessService) {
        this.oauthService = oauthService;
        this.authHeaderHelper = authHeaderHelper;
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
    }

    /**
     * Gets an instance of an access token, a refresh token, and their TTLs.
     * Will return the current access token if it has not expired.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param creds      AuthCredentials for authenticating the token request.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    public Response getAccessToken(
        @HeaderParam("Authorization") String authHeader,
        EntityHolder<com.rackspace.idm.jaxb.AuthCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }

        com.rackspace.idm.jaxb.AuthCredentials creds = holder.getEntity();
        AuthCredentials trParam = new AuthCredentials();
        trParam.setClientId(creds.getClientId());
        trParam.setClientSecret(creds.getClientSecret());
        trParam.setPassword(creds.getPassword());
        trParam.setRefreshToken(creds.getRefreshToken());
        trParam.setUsername(creds.getUsername());

        try {
            trParam.setGrantType(creds.getGrantType().value());
        } catch (Exception ex) {
            String errMsg = "Invalid Grant Type";
            logger.info(errMsg);
            throw new BadRequestException(errMsg);
        }

        OAuthGrantType grantType = this.oauthService.getGrantType(trParam
            .getGrantType());
        ApiError err = this.oauthService.validateGrantType(trParam, grantType);
        if (err != null) {
            String msg = String.format("Bad request parameters: %s",
                err.getMessage());
            logger.warn(msg);
            throw new BadRequestException(msg);
        }

        DateTime currentTime = this.getCurrentTime();
        ScopeAccessObject scopeAccess = oauthService.getTokens(grantType,
            trParam, currentTime);
        return Response.ok(authConverter.toAuthDataJaxb(scopeAccess)).build();
    }

    /**
     * Validates token and then, if valid, returns the access token and its ttl.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be validated.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * @response.representation.503.doc Service could not be reached. See the error message for details.
     */
    @GET
    @Path("{tokenString}")
    public Response validateAccessToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Validating Access Token: {}", tokenString);

        ScopeAccessObject authToken = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Rackers, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authToken)
            || authorizationService.authorizeRackspaceClient(authToken)
            || authorizationService.authorizeClient(authToken,
                request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, authToken);

        // Validate Token exists and is valid
        ScopeAccessObject scopeAccess = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenString);
        if (scopeAccess == null) {
            String errorMsg = String
                .format("Token not found : %s", tokenString);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if(scopeAccess instanceof hasAccessToken) {
            boolean expired = ((hasAccessToken)scopeAccess).isAccessTokenExpired(new DateTime());
            if(expired) {
                String errorMsg = String
                .format("Token expired : %s", tokenString);
                logger.warn(errorMsg);
                throw new NotFoundException(errorMsg);
            }
        }
        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(scopeAccess)).build();
    }

    // /**
    // * !!! ONLY OTHER IDM INSTANCES CAN CALL THIS !!!
    // * For cross-data-center token replication.
    // */
    // @GET
    // @Path("{tokenString}")
    // @Produces({MediaType.APPLICATION_OCTET_STREAM})
    // public Response getAccessTokenObj(@Context Request request,
    // @Context UriInfo uriInfo,
    // @HeaderParam("Authorization") String authHeader,
    // @PathParam("tokenString") String tokenString) {
    // logger.debug("Retrieving XDC Access Token: {}", tokenString);
    //
    // AccessToken callingToken = this.tokenService
    // .getAccessTokenByAuthHeader(authHeader);
    //
    // // Only Another IDM instance is authorized.
    // boolean authorized = authorizationService
    // .authorizeCustomerIdm(callingToken);
    //
    // if (!authorized) {
    // String errMsg = String.format("Token %s Forbidden from this call",
    // callingToken.getTokenString());
    // logger.warn(errMsg);
    // return Response.status(Status.FORBIDDEN).build();
    // }
    //
    // // Validate Token exists and is valid
    // AccessToken token = tokenService.validateToken(tokenString);
    // if (token == null) {
    // logger.warn("Token not found : {}", tokenString);
    // return Response.status(Status.NOT_FOUND).build();
    // }
    //
    // logger.debug("Retrieved XDC Access Token: {}", tokenString);
    // return Response.ok(SerializationUtils.serialize(token)).build();
    // }

    /**
     * Removes the token from IDM, across all DCs.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be revoked.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    @Path("{tokenString}")
    public Response revokeAccessToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Revoking Token: {}", tokenString);

        logger.debug("Parsing Auth Header");
        String authTokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        logger.debug("Parsed Auth Header - Token: {}", authTokenString);

        this.oauthService.revokeAccessToken(authTokenString, tokenString);

        logger.warn("Revoked Token: {}", tokenString);

        return Response.noContent().build();
    }

    /**
     * Check if the given access token as the specified permission.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}notFound
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    @Path("{tokenString}/services/{serviceId}/permissions/{permissionId}")
    public Response validateTokenPermission(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("tokenString") String tokenString,
        @PathParam("serviceId") String serviceId,
        @PathParam("permissionId") String permissionId) {

        logger.debug("Checking whether token {} has permission {}",
            tokenString, permissionId);

        ScopeAccessObject token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = token instanceof ClientScopeAccessObject && serviceId.equals(token.getClientId());

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        ScopeAccessObject tokenToCheck = this.scopeAccessService
            .getAccessTokenByAuthHeader(tokenString);

        if (tokenToCheck == null) {
            throw new NotFoundException(String.format("Token %s not found",
                tokenString));
        }
        
        if(tokenToCheck instanceof hasAccessToken) {
            boolean expired = ((hasAccessToken)tokenToCheck).isAccessTokenExpired(new DateTime());
            if(expired) {
                String errorMsg = String
                .format("Token expired : %s", tokenString);
                logger.warn(errorMsg);
                throw new NotFoundException(errorMsg);
            }
        }

        PermissionObject permission = new PermissionObject();
        permission.setClientId(token.getClientId());
        permission.setCustomerId(token.getClientRCN());
        permission.setPermissionId(permissionId);
        
        PermissionObject defined = this.scopeAccessService.getPermissionForParent(token.getUniqueId(), permission);
        
        if (defined == null || !defined.getEnabled()) {
            return Response.status(404).build();
        }
        
        if (tokenToCheck instanceof UserScopeAccessObject) {
            if (defined.getGrantedByDefault()) {
                return Response.ok().build();
            }
        }

        if (this.scopeAccessService.doesAccessTokenHavePermission(
            ((hasAccessToken) tokenToCheck).getAccessTokenString(), permission)) {
            return Response.ok().build();
        }

        return Response.status(404).build();
    }

    // private funcs
    protected DateTime getCurrentTime() {
        return new DateTime();
    }
}
