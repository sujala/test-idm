package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.event.*;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Cloud Auth 1.1 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    @Autowired
    private DefaultCloud11Service cloud11Service;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Context
    private UriInfo uriInfo;

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "v1.1 Get version")
    @GET
    public Response getCloud11VersionInfo() throws JAXBException {
        return cloud11Service.getVersion(uriInfo).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "v1.1 List extensions")
    @GET
    @Path("extensions")
    public Response extensions(@Context HttpHeaders httpHeaders) throws IOException {
        return cloud11Service.extensions(httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "v1.1 Get extension")
    @GET
    @Path("extensions/{alias}")
    public Response extensions(@PathParam("alias") String alias,@Context HttpHeaders httpHeaders) throws IOException {
        return cloud11Service.getExtension(httpHeaders, alias).build();
    }

    // this is not my fault, I promise
    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "v1.1 hack")
    @Deprecated
    @GET
    @Path("cloud/auth")
    public Response hack() throws IOException {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name = "v1.1 Authenticate")
    @POST
    @Path("auth")
    public Response authenticate(@Context HttpServletRequest request, @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, String body)
            throws IOException, JAXBException, URISyntaxException {
        return cloud11Service.authenticate(request, uriInfo, httpHeaders, body).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name = "v1.1 Auth-admin")
    @POST
    @Path("auth-admin")
    public Response adminAuthenticate(@Context HttpServletRequest request, @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, String body)
            throws IOException, JAXBException, URISyntaxException {
        return cloud11Service.adminAuthenticate(request, uriInfo, httpHeaders, body).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Validate token")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11TokenValidationAbsolutePathPatternRegex)
    @ReportableQueryParams(unsecuredQueryParams = {"belongsTo", "type"})
    @GET
    @Path("token/{tokenId}")
    public Response validateToken(@Context HttpServletRequest request,
                                  @PathParam("tokenId") String tokenId,
                                  @QueryParam("belongsTo") String belongsTo,
                                  @QueryParam("type") String type,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.validateToken(request, tokenId, belongsTo, type, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user-admin for tenant")
    @GET
    @Path("nast/{nastId}")
    public Response getUserFromNastId(@Context HttpServletRequest request,
                                      @PathParam("nastId") String nastId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getUserFromNastId(request, nastId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user-admin for cloud tenant")
    @GET
    @Path("mosso/{mossoId}")
    public Response getUserFromMossoId(@Context HttpServletRequest request,
                                       @PathParam("mossoId") int mossoId,
                                       @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getUserFromMossoId(request, mossoId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserByUsernameAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}")
    public Response getUser(@Context HttpServletRequest request,
                            @PathParam("userId") String userId,
                            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getUser(request, userId, httpHeaders).build();
    }

    public void setCloud11Service(DefaultCloud11Service cloud11Service) {
        this.cloud11Service = cloud11Service;
    }
}
