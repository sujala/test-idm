package com.rackspace.idm.api.resource.auth;

import java.util.List;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.MossoCredentials;
import com.rackspace.idm.jaxb.NastCredentials;
import com.rackspace.idm.jaxb.UsernameCredentials;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Backward Compatible Auth Methods
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class AuthResource {

    private final AuthorizationService authorizationService;
    private final EndpointService endpointService;
    private final ScopeAccessService scopeAccessService;

    private final AuthConverter authConverter;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public AuthResource(AuthConverter authConverter,
        EndpointService endpointService,
        AuthorizationService authorizationService,
        ScopeAccessService scopeAccessService) {
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.endpointService = endpointService;
        this.scopeAccessService = scopeAccessService;
    }

    /**
     * Gets an Access Token for Auth with Username and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}usernameCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}cloudAuth
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
        @HeaderParam("Authorization") String authHeader,
        EntityHolder<UsernameCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        UsernameCredentials creds = holder.getEntity();
        String username = creds.getUsername();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for username or key";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        UserScopeAccess usa = this.scopeAccessService
            .getUserScopeAccessForClientIdByUsernameAndApiCredentials(username,
                apiKey, token.getClientId());

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(username);

        return Response.ok(this.authConverter.toCloudAuthJaxb(usa, endpoints))
            .build();
    }

    /**
     * Gets an Access Token for Auth with MossoId and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}mossoCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}cloudAuth
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
    public Response authWithMossoIdAndApiKey(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        EntityHolder<MossoCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                ((hasAccessToken) token).getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        MossoCredentials creds = holder.getEntity();
        int mossoId = creds.getMossoId();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for key";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        UserScopeAccess usa = this.scopeAccessService
            .getUserScopeAccessForClientIdByMossoIdAndApiCredentials(mossoId,
                apiKey, token.getClientId());

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(usa.getUsername());

        return Response.ok(this.authConverter.toCloudAuthJaxb(usa, endpoints))
            .build();
    }

    /**
     * Gets an Access Token for Auth with NastId and Api Key
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}nastCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}cloudAuth
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
    public Response authWithNastIdAndApiKey(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        EntityHolder<NastCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                ((hasAccessToken) token).getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        NastCredentials creds = holder.getEntity();
        String nastId = creds.getNastId();
        String apiKey = creds.getKey();

        if (StringUtils.isBlank(nastId) || StringUtils.isBlank(apiKey)) {
            String errMsg = "Blank Value passed in for nastId or key";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        UserScopeAccess usa = this.scopeAccessService
            .getUserScopeAccessForClientIdByNastIdAndApiCredentials(nastId,
                apiKey, token.getClientId());

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(usa.getUsername());

        return Response.ok(this.authConverter.toCloudAuthJaxb(usa, endpoints))
            .build();
    }

    /**
     * Gets an Access Token for Auth with Username and Password
     *
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}cloudAuth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param creds Auth Credentials
     */
    @POST
    @Path("username")
    public Response authByUsernameAndPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        EntityHolder<com.rackspace.idm.jaxb.AuthCredentials> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                ((hasAccessToken) token).getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        AuthCredentials creds = holder.getEntity();

        UserScopeAccess usa = this.scopeAccessService
            .getUserScopeAccessForClientIdByUsernameAndPassword(
                creds.getUsername(), creds.getPassword(), token.getClientId());

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(usa.getUsername());

        return Response.ok(this.authConverter.toCloudAuthJaxb(usa, endpoints))
            .build();
    }

    protected DateTime getCurrentTime() {
        return new DateTime();
    }
}
