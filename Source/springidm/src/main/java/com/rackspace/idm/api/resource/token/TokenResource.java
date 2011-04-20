package com.rackspace.idm.api.resource.token;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.config.LoggerFactoryWrapper;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.exception.ApiException;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.TokenExpiredException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Management of OAuth 2.0 token used by IDM.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokenResource {
    private final AccessTokenService tokenService;
    private final OAuthService oauthService;
    private final AuthHeaderHelper authHeaderHelper;
    private final AuthConverter authConverter;
    private final PermissionConverter permissionConverter;
    private final AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(TokenResource.class);

    @Autowired(required = true)
    public TokenResource(AccessTokenService tokenService, OAuthService oauthService,
        AuthHeaderHelper authHeaderHelper, AuthConverter authConverter, PermissionConverter permissionConverter,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.tokenService = tokenService;
        this.oauthService = oauthService;
        this.authHeaderHelper = authHeaderHelper;
        this.authConverter = authConverter;
        this.permissionConverter = permissionConverter;
        this.authorizationService = authorizationService;
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
    public Response getAccessToken(@HeaderParam("Authorization") String authHeader,
        EntityHolder<com.rackspace.idm.jaxb.AuthCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        
        com.rackspace.idm.jaxb.AuthCredentials creds = holder.getEntity();
        AuthCredentials trParam = new AuthCredentials();
        trParam.setClientId(creds.getClientId());
        trParam.setClientSecret(creds.getClientSecret());
        trParam.setGrantType(creds.getGrantType().value());
        trParam.setPassword(creds.getPassword());
        trParam.setRefreshToken(creds.getRefreshToken());
        trParam.setUsername(creds.getUsername());

        // if request includes an authHeader then the values for clientId and
        // clientSecret need to be parsed out. Also, the AuthHeader values will
        // override the values for client_id and client_secret passed in the
        // request
        if (!StringUtils.isBlank(authHeader)) {
            Map<String, String> authParams = authHeaderHelper.parseBasicParams(authHeader);
            if (authParams != null) {
                trParam.setClientId(authParams.get("username"));
                trParam.setClientSecret(authParams.get("password"));
            }
        }

        OAuthGrantType grantType = this.oauthService.getGrantType(trParam.getGrantType());
        ApiError err = this.oauthService.validateGrantType(trParam, grantType);
        if (err != null) {
            String msg = String.format("Bad request parameters: %s", err.getMessage());
            logger.warn(msg);
            throw new BadRequestException(msg);
        }

        DateTime currentTime = this.getCurrentTime();
        AuthData authData = oauthService.getTokens(grantType, trParam, currentTime);
        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();
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
    public Response validateAccessToken(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("tokenString") String tokenString) {

        logger.debug("Validating Access Token: {}", tokenString);

        AccessToken authToken = this.tokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Rackers, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authToken)
            || authorizationService.authorizeRackspaceClient(authToken)
            || authorizationService.authorizeClient(authToken, request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", authToken.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        AuthData auth = new AuthData();

        // Validate Token exists and is valid
        AccessToken token = tokenService.validateToken(tokenString);
        if (token == null) {
            String errorMsg = String.format("Token not found : %s", tokenString);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        auth.setAccessToken(token);
        auth.setUser(token.getTokenUser());
        auth.setClient(token.getTokenClient());

        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(auth)).build();
    }

    /**
     * !!! ONLY OTHER IDM INSTANCES CAN CALL THIS !!!
     * For cross-data-center token replication.
     */
    @GET
    @Path("{tokenString}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getAccessTokenObj(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("tokenString") String tokenString) {
        logger.debug("Retrieving XDC Access Token: {}", tokenString);

        AccessToken callingToken = this.tokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Another IDM instance is authorized.
        boolean authorized = authorizationService.authorizeCustomerIdm(callingToken);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", callingToken.getTokenString());
            logger.warn(errMsg);
            return Response.status(Status.FORBIDDEN).build();
        }

        // Validate Token exists and is valid
        AccessToken token = tokenService.validateToken(tokenString);
        if (token == null) {
            logger.warn("Token not found : {}", tokenString);
            return Response.status(Status.NOT_FOUND).build();
        }

        logger.debug("Retrieved XDC Access Token: {}", tokenString);
        return Response.ok(SerializationUtils.serialize(token)).build();
    }

    /**
     * Removes the token from IDM, across all DCs.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be revoked.
     * @param isGlobal If false (default is true), will revoke tokens in the local DC only. Only
     *        Customer IDM can make the local removal call.
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
    public Response revokeAccessToken(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("tokenString") String tokenString,
        @DefaultValue("true") @QueryParam("global") boolean isGlobal) {

        logger.debug("Revoking Token: {}", tokenString);

        try {
            logger.debug("Parsing Auth Header");
            String authTokenString = authHeaderHelper.getTokenFromAuthHeader(authHeader);
            logger.debug("Parsed Auth Header - Token: {}", authTokenString);

            if (isGlobal) {
                // Propagate the revoke call to all DCs
                oauthService.revokeTokensGlobally(authTokenString, tokenString);
            } else {
                // Most likely a revoke call coming in from the IDM instance
                // where the revoke call originated.
                oauthService.revokeTokensLocally(authTokenString, tokenString);
            }

            logger.warn("Revoked Token: {}", tokenString);

        } catch (TokenExpiredException ex) {
            String errorMsg = String.format("Authorization failed, token is expired: %s", authHeader);
            logger.warn(errorMsg);
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, ErrorMsg.UNAUTHORIZED, errorMsg);

        } catch (IllegalStateException ex) {
            String errorMsg = String.format("IllegalState encountered when revoking token: %s", tokenString);
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, ErrorMsg.BAD_REQUEST, errorMsg);

        }

        return Response.noContent().build();
    }

    /**
     * To be used only by a remote instance of IDM.
     * 
     * @param request
     * @param uriInfo
     * @param authHeader
     * @param id
     * @return
     */
    @DELETE
    public Response revokeAccessTokensForOwnerOrCustomer(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @QueryParam("querytype") GlobalConstants.TokenDeleteByType queryType, @QueryParam("id") String id) {
        logger.debug("Revoking Token for query type {} and id {}", queryType, id);

        logger.debug("Parsing Auth Header");
        String idmAuthTokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        logger.debug("Parsed Auth Header - Token: {}", idmAuthTokenStr);

        if (queryType == null || StringUtils.isBlank(id)) {
            String msg = "Both the querytype (either owner or customer) and the id values are required.";
            logger.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        
        try {
            oauthService.revokeTokensLocallyForOwnerOrCustomer(idmAuthTokenStr, queryType, id);         
        } catch (TokenExpiredException ex) {
            String errorMsg = String.format("Authorization failed, token is expired: %s", authHeader);
            logger.warn(errorMsg);
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, ErrorMsg.UNAUTHORIZED, errorMsg);
        }
        return Response.noContent().build();
    }
    
    /**
     * Check if the given access token as the specified permission.
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
     @GET
     @Path("{tokenString}/permissions/{permissionId}")
     public Response validateTokenPermission(@Context Request request, @Context UriInfo uriInfo,
         @HeaderParam("Authorization") String authHeader, @PathParam("tokenString") String tokenString,
         @PathParam("permissionId") String permissionId) {
         
         logger.debug("Validating Access Token: {}", tokenString);
      
         AccessToken accessToken = this.tokenService.getAccessTokenByTokenString(tokenString);
         
         if (accessToken == null || !accessToken.getTokenString().equals(tokenString)) {
             throw new NotFoundException("Token " + tokenString + " not found");
         }
 
         if (this.tokenService.checkAndReturnPermission(accessToken, permissionId)) {
             return Response.ok("Token " + tokenString + " has the permission" + permissionId).build();
         }
         
         return Response.status(404).build();
    } 

    // private funcs
    protected DateTime getCurrentTime() {
        return new DateTime();
    }
}
