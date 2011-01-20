package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.jaxb.MossoCredentials;
import com.rackspace.idm.jaxb.NastCredentials;
import com.rackspace.idm.jaxb.UsernameCredentials;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;

/**
 * Backward Compatible Auth Methods
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class AuthResource {

    private AccessTokenService accessTokenService;
    private TokenConverter tokenConverter;
    private AuthorizationService authorizationService;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public AuthResource(TokenConverter tokenConverter,
        AuthorizationService authorizationService, OAuthService oauthService,
        AccessTokenService accessTokenService, LoggerFactoryWrapper logger) {
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
        this.oauthService = oauthService;
        this.accessTokenService = accessTokenService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an Access Token for Auth with Username and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}accessToken
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param creds User Credentials
     */
    @POST
    public Response getUsernameAuth(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, UsernameCredentials creds) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        String username = creds.getUsername();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for username or key";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        int expirationSeconds = accessTokenService
            .getCloudAuthDefaultTokenExpirationSeconds();

        AccessToken userToken = accessTokenService
            .getTokenByUsernameAndApiCredentials(token.getTokenClient(),
                username, apiKey, expirationSeconds, new DateTime());

        return Response.ok(tokenConverter.toAccessTokenJaxb(userToken)).build();
    }

    /**
     * Gets an Access Token for Auth with MossoId and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}mossoCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}accessToken
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        int mossoId = creds.getMossoId();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for key";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        int expirationSeconds = accessTokenService
            .getCloudAuthDefaultTokenExpirationSeconds();

        AccessToken userToken = accessTokenService
            .getTokenByMossoIdAndApiCredentials(token.getTokenClient(),
                mossoId, apiKey, expirationSeconds, new DateTime());

        return Response.ok(tokenConverter.toAccessTokenJaxb(userToken)).build();
    }

    /**
     * Gets an Access Token for Auth with NastId and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}nastCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}accessToken
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        String nastId = creds.getNastId();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(nastId) || StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for nastId or key";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        int expirationSeconds = accessTokenService.getCloudAuthDefaultTokenExpirationSeconds();

        AccessToken userToken = accessTokenService
            .getTokenByNastIdAndApiCredentials(token.getTokenClient(), nastId,
                apiKey, expirationSeconds, new DateTime());

        return Response.ok(tokenConverter.toAccessTokenJaxb(userToken)).build();
    }
}
