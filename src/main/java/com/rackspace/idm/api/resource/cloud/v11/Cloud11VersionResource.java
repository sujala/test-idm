package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.Encoder;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private final Configuration config;
    private final CloudClient cloudClient;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Autowired
    private DelegateCloud11Service delegateCloud11Service;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public Cloud11VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    public Response getPublicCloud11VersionInfo(@Context HttpHeaders httpHeaders) throws IOException {
        //For the pubic profile, we're just forwarding to what cloud has. Once we become the
        //source of truth, we should use the CloudContractDescriptorBuilder to render this.
        return cloudClient.get(getCloudAuthV11Url(), httpHeaders).build();
    }

    @GET
    public Response getCloud11VersionInfo() throws JAXBException {
        return defaultCloud11Service.getVersion(uriInfo).build();
    }

    @POST
    @Path("auth")
    public Response authenticate(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context HttpHeaders httpHeaders, String body)
            throws IOException, JAXBException, URISyntaxException {
        return getCloud11Service().authenticate(request, response, httpHeaders, body).build();
    }

    // this is not my fault, I promise
    @GET
    @Path("cloud/auth")
    public Response hack() throws IOException {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("auth-admin")
    public Response adminAuthenticate(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context HttpHeaders httpHeaders, String body)
            throws IOException, JAXBException, URISyntaxException {
    	
    	if (config.getBoolean("useCloudAuth")) {
            return delegateCloud11Service.adminAuthenticate(request, response, httpHeaders, body).build();
        } else {
            return defaultCloud11Service.authenticate(request, response, httpHeaders, body).build();
        }
    }

    @GET
    @Path("token/{tokenId}")
    public Response validateToken(@Context HttpServletRequest request,
                                  @PathParam("tokenId") String tokenId,
                                  @QueryParam("belongsTo") String belongsTo,
                                  @QueryParam("type") String type,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().validateToken(request, Encoder.encode(tokenId), Encoder.encode(belongsTo), Encoder.encode(type), httpHeaders).build();
    }

    @DELETE
    @Path("token/{tokenId}")
    public Response revokeToken(@Context HttpServletRequest request,
                                @PathParam("tokenId") String tokenId,
                                @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().revokeToken(request, Encoder.encode(tokenId), httpHeaders).build();
    }

    @GET
    @Path("extensions")
    public Response extensions(@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud11Service().extensions(httpHeaders).build();
    }

    @GET
    @Path("extensions/{alias}")
    public Response extensions(@PathParam("alias") String alias,@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud11Service().getExtension(httpHeaders,alias).build();
    }

    @GET
    @Path("nast/{nastId}")
    public Response getUserFromNastId(@Context HttpServletRequest request,
                                      @PathParam("nastId") String nastId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUserFromNastId(request, Encoder.encode(nastId), httpHeaders).build();
    }

    @GET
    @Path("mosso/{mossoId}")
    public Response getUserFromMossoId(@Context HttpServletRequest request,
                                       @PathParam("mossoId") int mossoId,
                                       @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUserFromMossoId(request, mossoId, httpHeaders).build();
    }

    @GET
    @Path("baseURLs")
    public Response getBaseURLs(@Context HttpServletRequest request,
                                @QueryParam("serviceName") String serviceName,
                                @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getBaseURLs(request, Encoder.encode(serviceName), httpHeaders).build();
    }

    @POST
    @Path("baseURLs")
    public Response addBaseURL(@Context HttpServletRequest request, @Context HttpHeaders httpHeaders, BaseURL baseUrl)
            throws IOException, JAXBException {
        return getCloud11Service().addBaseURL(request, httpHeaders, baseUrl).build();
    }

    @GET
    @Path("baseURLs/{baseURLId}")
    public Response getBaseURLById(@Context HttpServletRequest request,
                                   @PathParam("baseURLId") int baseURLId,
                                   @QueryParam("serviceName") String serviceName,
                                   @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getBaseURLById(request, baseURLId, Encoder.encode(serviceName), httpHeaders).build();
    }

    @GET
    @Path("baseURLs/enabled")
    public Response getEnabledBaseURLs(@Context HttpServletRequest request,
                                       @QueryParam("serviceName") String serviceName,
                                       @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getEnabledBaseURL(request, Encoder.encode(serviceName), httpHeaders).build();
    }

    @POST
    @Path("users")
    public Response createUser(@Context HttpServletRequest request,
                               @Context HttpHeaders httpHeaders, @Context UriInfo uriInfo,
                               User user
    ) throws IOException, JAXBException {
        return getCloud11Service().createUser(request, httpHeaders, uriInfo, user).build();
    }

    @GET
    @Path("users/{userId}")
    public Response getUser(@Context HttpServletRequest request,
                            @PathParam("userId") String userId,
                            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUser(request, Encoder.encode(userId), httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException, JAXBException {
        return getCloud11Service().deleteUser(request, Encoder.encode(userId), httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}")
    public Response updateUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               User user) throws IOException, JAXBException {
        return getCloud11Service().updateUser(request, Encoder.encode(userId), httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/enabled")
    public Response getUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders) throws IOException {
        return getCloud11Service().getUserEnabled(request, Encoder.encode(userId), httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}/enabled")
    public Response setUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders, UserWithOnlyEnabled user) throws IOException, JAXBException {
        return getCloud11Service().setUserEnabled(request, Encoder.encode(userId), user, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/key")
    public Response getUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUserKey(request, Encoder.encode(userId), httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}/key")
    public Response setUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               UserWithOnlyKey user
    ) throws IOException, JAXBException {
        return getCloud11Service().setUserKey(request, Encoder.encode(userId), httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/serviceCatalog")
    public Response getServiceCatalog(@Context HttpServletRequest request,
                                      @PathParam("userId") String userId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getServiceCatalog(request, Encoder.encode(userId), httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs")
    public Response getBaseURLRefs(@Context HttpServletRequest request,
                                   @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getBaseURLRefs(request, Encoder.encode(userId), httpHeaders).build();
    }

    @POST
    @Path("users/{userId}/baseURLRefs")
    public Response addBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders,
                                  @Context UriInfo uriInfo,
                                  BaseURLRef baseUrlRef
    ) throws IOException, JAXBException {
        return getCloud11Service().addBaseURLRef(request, Encoder.encode(userId), httpHeaders, uriInfo, baseUrlRef).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response getBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @PathParam("baseURLId") String baseURLId,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getBaseURLRef(request, Encoder.encode(userId), Encoder.encode(baseURLId), httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response deleteBaseURLRef(@Context HttpServletRequest request,
                                     @PathParam("userId") String userId,
                                     @PathParam("baseURLId") String baseURLId,
                                     @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().deleteBaseURLRef(request, Encoder.encode(userId), Encoder.encode(baseURLId), httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/groups")
    public Response getUserGroups(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders) throws IOException {
        return getCloud11Service().getUserGroups(request, Encoder.encode(userId), httpHeaders).build();
    }

    Cloud11Service getCloud11Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud11Service;
        } else {
            return defaultCloud11Service;
        }
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }

    public void setDefaultCloud11Service(DefaultCloud11Service defaultCloud11Service) {
        this.defaultCloud11Service = defaultCloud11Service;
    }

    public void setDelegateCloud11Service(DelegateCloud11Service delegateCloud11Service) {
        this.delegateCloud11Service = delegateCloud11Service;
    }
}
