package com.rackspace.idm.api.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.EndPointConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.BaseURL;
import com.rackspace.idm.validation.InputValidator;

/**
 * A Cloud Auth BaseUrl
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class BaseUrlsResource {

    private AccessTokenService accessTokenService;
    private AuthorizationService authorizationService;
    private EndpointService endpointService;
    private EndPointConverter endpointConverter;
    private InputValidator inputValidator;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public BaseUrlsResource(AccessTokenService accessTokenService,
        AuthorizationService authorizationService,
        EndpointService endpointService, EndPointConverter endpointConverter,
        InputValidator inputValidator) {
        this.accessTokenService = accessTokenService;
        this.authorizationService = authorizationService;
        this.endpointConverter = endpointConverter;
        this.endpointService = endpointService;
        this.inputValidator = inputValidator;
    }

    /**
     * Gets a list of baseUrls.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}baseURLs
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     */
    @GET
    public Response getBaseUrls(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        List<CloudBaseUrl> urls = this.endpointService.getBaseUrls();

        return Response.ok(this.endpointConverter.toBaseUrls(urls)).build();
    }

    /**
     * Adds a BaseUrl.
     *
     * @response.representation.201.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param baseUrl baseUrl
     */
    @POST
    public Response addBaseUrl(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, BaseURL baseUrl) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        CloudBaseUrl url = this.endpointConverter.toBaseUrlDO(baseUrl);

        ApiError err = inputValidator.validate(url);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        this.endpointService.addBaseUrl(url);

        String location = uriInfo.getPath()
            + String.valueOf(url.getBaseUrlId());

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Customer Location URI error");
        }

        return Response.created(uri).build();
    }

    /**
     * Gets a BaseUrl.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}baseURLs
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param baseUrlId baseUrlId
     */
    @GET
    @Path("{baseUrlId}")
    public Response getBaseUrl(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("baseUrlId") int baseUrlId) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        CloudBaseUrl url = this.endpointService.getBaseUrlById(baseUrlId);

        if (url == null) {
            String errMsg = String.format("BaseUrl with id %s not found.",
                baseUrlId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return Response.ok(this.endpointConverter.toBaseUrl(url)).build();
    }

    /**
     * Deletes a BaseUrl.
     *
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param baseUrlId baseUrlId
     */
    @DELETE
    @Path("{baseUrlId}")
    public Response deleteBaseUrl(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("baseUrlId") int baseUrlId) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
        
        CloudBaseUrl url = this.endpointService.getBaseUrlById(baseUrlId);
        
        if (url == null) {
            String errMsg = String.format("BaseUrlId %s not found",
                baseUrlId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        this.endpointService.deleteBaseUrl(baseUrlId);

        return Response.noContent().build();
    }
}
