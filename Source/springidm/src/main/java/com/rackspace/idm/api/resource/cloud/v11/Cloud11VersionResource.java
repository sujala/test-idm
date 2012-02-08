package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.Encoder;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
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

/**
 * Cloud Auth 1.1 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Autowired
    private DelegateCloud11Service delegateCloud11Service;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public Cloud11VersionResource(Configuration config, CloudClient cloudClient,
                                  CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
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
            throws IOException, JAXBException {
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
            throws IOException, JAXBException {
        return getCloud11Service().adminAuthenticate(request, response, httpHeaders, body).build();
    }

    @GET
    @Path("token/{tokenId}")
    public Response validateToken(@Context HttpServletRequest request,
                                  @PathParam("tokenId") String tokenId,
                                  @QueryParam("belongsTo") String belongsTo,
                                  @QueryParam("type") String type,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        tokenId = Encoder.encode(tokenId);
        belongsTo = Encoder.encode(belongsTo);
        type = Encoder.encode(type);
        return getCloud11Service().validateToken(request, tokenId, belongsTo, type, httpHeaders).build();
    }

    @DELETE
    @Path("token/{tokenId}")
    public Response revokeToken(@Context HttpServletRequest request,
                                @PathParam("tokenId") String tokenId,
                                @Context HttpHeaders httpHeaders
    ) throws IOException {
        tokenId = Encoder.encode(tokenId);
        return getCloud11Service().revokeToken(request, tokenId, httpHeaders).build();
    }

    @GET
    @Path("extensions")
    public Response extensions(@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud11Service().extensions(httpHeaders).build();
    }

    @GET
    @Path("nast/{nastId}")
    public Response getUserFromNastId(@Context HttpServletRequest request,
                                      @PathParam("nastId") String nastId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        nastId = Encoder.encode(nastId);
        return getCloud11Service().getUserFromNastId(request, nastId, httpHeaders).build();
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
        serviceName = Encoder.encode(serviceName);
        return getCloud11Service().getBaseURLs(request, serviceName, httpHeaders).build();
    }

    @POST
    @Path("baseURLs")
    public Response addBaseURL(@Context HttpServletRequest request, @Context HttpHeaders httpHeaders, BaseURL baseUrl)
            throws IOException, JAXBException {
        return getCloud11Service().addBaseURL(request, httpHeaders, baseUrl).build();
    }

    @GET
    @Path("baseURLs/{baseURLId}")
    public Response getBaseURLId(@Context HttpServletRequest request,
                                 @PathParam("baseURLId") int baseURLId,
                                 @QueryParam("serviceName") String serviceName,
                                 @Context HttpHeaders httpHeaders
    ) throws IOException {
        serviceName = Encoder.encode(serviceName);
        return getCloud11Service().getBaseURLId(request, baseURLId, serviceName, httpHeaders).build();
    }

    @GET
    @Path("baseURLs/enabled")
    public Response getEnabledBaseURLs(@Context HttpServletRequest request,
                                       @QueryParam("serviceName") String serviceName,
                                       @Context HttpHeaders httpHeaders
    ) throws IOException {
        serviceName = Encoder.encode(serviceName);
        return getCloud11Service().getEnabledBaseURL(request, serviceName, httpHeaders).build();
    }

    @POST
    @Path("migration/{userId}/migrate")
    public Response migrate(@Context HttpServletRequest request,
                            @PathParam("userId") String user,
                            @Context HttpHeaders httpHeaders,
                            String body
    ) throws IOException {
        user = Encoder.encode(user);
        return getCloud11Service().migrate(request, user, httpHeaders, body).build();
    }

    @POST
    @Path("migration/{userId}/unmigrate")
    public Response unmigrate(@Context HttpServletRequest request,
                              @PathParam("userId") String user,
                              @Context HttpHeaders httpHeaders,
                              String body
    ) throws IOException {
        user = Encoder.encode(user);
        return getCloud11Service().unmigrate(request, user, httpHeaders, body).build();
    }

    @POST
    @Path("migration/all")
    public Response all(@Context HttpServletRequest request,
                        @Context HttpHeaders httpHeaders,
                        String body
    ) throws IOException {
        return getCloud11Service().all(request, httpHeaders, body).build();
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
        userId = Encoder.encode(userId);
        return getCloud11Service().getUser(request, userId, httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().deleteUser(request, userId, httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}")
    public Response updateUser(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               User user) throws IOException, JAXBException {
        userId = Encoder.encode(userId);
        return getCloud11Service().updateUser(request, userId, httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/enabled")
    public Response getUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().getUserEnabled(request, userId, httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}/enabled")
    public Response setUserEnabled(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders, UserWithOnlyEnabled user) throws IOException, JAXBException {
        userId = Encoder.encode(userId);
        return getCloud11Service().setUserEnabled(request, userId, user, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/key")
    public Response getUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().getUserKey(request, userId, httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}/key")
    public Response setUserKey(@Context HttpServletRequest request,
                               @PathParam("userId") String userId,
                               @Context HttpHeaders httpHeaders,
                               UserWithOnlyKey user
    ) throws IOException, JAXBException {
        userId = Encoder.encode(userId);
        return getCloud11Service().setUserKey(request, userId, httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/serviceCatalog")
    public Response getServiceCatalog(@Context HttpServletRequest request,
                                      @PathParam("userId") String userId,
                                      @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().getServiceCatalog(request, userId, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs")
    public Response getBaseURLRefs(@Context HttpServletRequest request,
                                   @PathParam("userId") String userId,
                                   @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().getBaseURLRefs(request, userId, httpHeaders).build();
    }

    @POST
    @Path("users/{userId}/baseURLRefs")
    public Response addBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders,
                                  @Context UriInfo uriInfo,
                                  BaseURLRef baseUrlRef
    ) throws IOException, JAXBException {
        userId = Encoder.encode(userId);
        return getCloud11Service().addBaseURLRef(request, userId, httpHeaders, uriInfo, baseUrlRef).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response getBaseURLRef(@Context HttpServletRequest request,
                                  @PathParam("userId") String userId,
                                  @PathParam("baseURLId") String baseURLId,
                                  @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        baseURLId = Encoder.encode(baseURLId);
        return getCloud11Service().getBaseURLRef(request, userId, baseURLId, httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response deleteBaseURLRef(@Context HttpServletRequest request,
                                     @PathParam("userId") String userId,
                                     @PathParam("baseURLId") String baseURLId,
                                     @Context HttpHeaders httpHeaders
    ) throws IOException {
        userId = Encoder.encode(userId);
        baseURLId = Encoder.encode(baseURLId);
        return getCloud11Service().deleteBaseURLRef(request, userId, baseURLId, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/groups")
    public Response getUserGroups(@Context HttpServletRequest request, @PathParam("userId") String userId,
                                  @Context HttpHeaders httpHeaders) throws IOException {
        userId = Encoder.encode(userId);
        return getCloud11Service().getUserGroups(request, userId, httpHeaders).build();
    }

    private Cloud11Service getCloud11Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud11Service;
        } else {
            return defaultCloud11Service;
        }
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
