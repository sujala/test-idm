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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Revoke token")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11TokenValidationAbsolutePathPatternRegex)
    @DELETE
    @Path("token/{tokenId}")
    public Response revokeToken(@Context HttpServletRequest request,
                                @PathParam("tokenId") String tokenId,
                                @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.revokeToken(request, tokenId, httpHeaders).build();
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 List endpoint templates")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceName"})
    @GET
    @Path("baseURLs")
    public Response getBaseURLs(@Context HttpServletRequest request,
                                @QueryParam("serviceName") String serviceName,
                                @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getBaseURLs(request, serviceName, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Add endpoint template")
    @POST
    @Path("baseURLs")
    public Response addBaseURL(@Context HttpServletRequest request, @Context HttpHeaders httpHeaders, BaseURL baseUrl)
            throws IOException, JAXBException {
        if(identityConfig.getV11AddBaseUrlExposed()) {
            return cloud11Service.addBaseURL(request, httpHeaders, baseUrl).build();
        } else {
            throw new NotFoundException("Resource Not Found");
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get endpoint template")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceName"})
    @GET
    @Path("baseURLs/{baseURLId}")
    public Response getBaseURLById(@Context HttpServletRequest request,
                                   @PathParam("baseURLId") int baseURLId,
                                   @QueryParam("serviceName") String serviceName,
                                   @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getBaseURLById(request, String.valueOf(baseURLId), serviceName, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 List enabled endpoints")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceName"})
    @GET
    @Path("baseURLs/enabled")
    public Response getEnabledBaseURLs(@Context HttpServletRequest request,
                                       @QueryParam("serviceName") String serviceName,
                                       @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getEnabledBaseURL(request, serviceName, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Add user")
    @POST
    @Path("users")
    public Response createUser(@Context HttpServletRequest request,
                               @Context HttpHeaders httpHeaders, @Context UriInfo uriInfo,
                               User user
    ) throws IOException, JAXBException {
        return cloud11Service.createUser(request, httpHeaders, uriInfo, user).build();
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Delete user")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserByUsernameAbsolutePathPatternRegex)
    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException, JAXBException {
        return cloud11Service.deleteUser(request, userId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Update user")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserByUsernameAbsolutePathPatternRegex)
    @PUT
    @Path("users/{userId}")
    public Response updateUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               User user) throws IOException, JAXBException {
        return cloud11Service.updateUser(request, userId, httpHeaders, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user enabled")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/enabled")
    public Response getUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders) throws IOException {
        return cloud11Service.getUserEnabled(request, userId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Update user enabled status")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @PUT
    @Path("users/{userId}/enabled")
    public Response setUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders, UserWithOnlyEnabled user) throws IOException, JAXBException {
        return cloud11Service.setUserEnabled(request, userId, user, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user api key")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/key")
    public Response getUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getUserKey(request, userId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Update user api key")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @PUT
    @Path("users/{userId}/key")
    public Response setUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               UserWithOnlyKey user
    ) throws IOException, JAXBException {
        return cloud11Service.setUserKey(request, userId, httpHeaders, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get user service catalog")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/serviceCatalog")
    public Response getServiceCatalog(@Context HttpServletRequest request,
                                      @PathParam("userId") String userId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getServiceCatalog(request, userId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 List user endpoints")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/baseURLRefs")
    public Response getBaseURLRefs(@Context HttpServletRequest request,
                                   @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getBaseURLRefs(request, userId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Add endpoint to user cloud or files tenant")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @POST
    @Path("users/{userId}/baseURLRefs")
    public Response addBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders,
                                  @Context UriInfo uriInfo,
                                  BaseURLRef baseUrlRef
    ) throws IOException, JAXBException {
        return cloud11Service.addBaseURLRef(request, userId, httpHeaders, uriInfo, baseUrlRef).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Get endpoint for user")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response getBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @PathParam("baseURLId") String baseURLId,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.getBaseURLRef(request, userId, baseURLId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 Delete endpoint from user cloud and files tenants")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @DELETE
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response deleteBaseURLRef(@Context HttpServletRequest request,
                                     @PathParam("userId") String userId,
                                     @PathParam("baseURLId") String baseURLId,
                                     @Context HttpHeaders httpHeaders
    ) throws IOException {
        return cloud11Service.deleteBaseURLRef(request, userId, baseURLId, httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v1.1 List user legacy groups")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v11UserResourceAbsolutePathPatternRegex)
    @GET
    @Path("users/{userId}/groups")
    public Response getUserGroups(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders) throws IOException {
        return cloud11Service.getUserGroups(request, userId, httpHeaders).build();
    }

    public void setCloud11Service(DefaultCloud11Service cloud11Service) {
        this.cloud11Service = cloud11Service;
    }
}
