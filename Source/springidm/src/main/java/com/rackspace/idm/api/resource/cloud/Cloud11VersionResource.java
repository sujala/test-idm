package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.cloudv11.jaxb.BaseURLRef;
import com.rackspace.idm.cloudv11.jaxb.User;
import com.rackspace.idm.cloudv11.jaxb.UserWithOnlyKey;

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


    @Autowired
    public Cloud11VersionResource(Configuration config, CloudClient cloudClient) {
        this.config = config;
        this.cloudClient = cloudClient;
    }

    @GET
    public Response getCloud11VersionInfo(@Context HttpHeaders httpHeaders)
            throws IOException {
        return cloudClient.get(getCloudAuthV11Url(), httpHeaders).build();
    }

    @POST
    @Path("auth")
    public Response authenticate(@Context HttpServletResponse response, @Context HttpHeaders httpHeaders, String body)
            throws IOException {
        return getCloud11Service().authenticate(response,httpHeaders, body).build();
    }
    
    @POST
    @Path("auth-admin")
    public Response adminAuthenticate(@Context HttpServletResponse response, @Context HttpHeaders httpHeaders, String body)
            throws IOException {
        return getCloud11Service().adminAuthenticate(response,httpHeaders, body).build();
    }

    @GET
    @Path("token/{tokenId}")
    public Response validateToken(
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo,
            @QueryParam("type") String type,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().validateToken(tokenId, belongsTo, type, httpHeaders).build();
    }

    @DELETE
    @Path("token/{tokenId}")
    public Response revokeToken(
            @PathParam("tokenId") String tokenId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().revokeToken(tokenId,httpHeaders).build();
    }

    @GET
    @Path("nast/{nastId}")
    public Response getUserFromNastId(@Context HttpServletRequest request,
            @PathParam("nastId") String nastId,
            @Context HttpHeaders httpHeaders
    )  throws IOException {
        return getCloud11Service().getUserFromNastId(request, nastId, httpHeaders).build();
    }

    @GET
    @Path("mosso/{mossoId}")
    public Response getUserFromMossoId(@Context HttpServletRequest request, 
            @PathParam("mossoId") int mossoId,
            @Context HttpHeaders httpHeaders
    )  throws IOException {
        return getCloud11Service().getUserFromMossoId(request, mossoId, httpHeaders).build();
    }

    @GET
    @Path("baseURLs")
    public Response getBaseURLs(
            @QueryParam("serviceName") String serviceName,
            @Context HttpHeaders httpHeaders
    )  throws IOException {
        return getCloud11Service().getBaseURLs(serviceName, httpHeaders).build();
    }

    @GET
    @Path("baseURLs/{baseURLId}")
    public Response getBaseURLId(
            @PathParam("baseURLId") int baseURLId,
            @QueryParam("serviceName") String serviceName,
            @Context HttpHeaders httpHeaders
    )  throws IOException {
        return getCloud11Service().getBaseURLId(baseURLId, serviceName, httpHeaders).build();
    }

    @GET
    @Path("baseURLs/enabled")
    public Response getEnabledBaseURLs(
            @QueryParam("serviceName") String serviceName,
            @Context HttpHeaders httpHeaders
    )  throws IOException {
        return getCloud11Service().getEnabledBaseURL(serviceName, httpHeaders).build();
    }

    @POST
    @Path("migration/{userId}/migrate")
    public Response migrate(
            @PathParam("userId") String user,
            @Context HttpHeaders httpHeaders,
            String body
    )  throws IOException {
        return getCloud11Service().migrate(user, httpHeaders, body).build();
    }

    @POST
    @Path("migration/{userId}/unmigrate")
    public Response unmigrate(
            @PathParam("userId") String user,
            @Context HttpHeaders httpHeaders,
            String body
    )  throws IOException {
        return getCloud11Service().unmigrate(user, httpHeaders, body).build();
    }

    @POST
    @Path("migration/all")
    public Response all(
            @Context HttpHeaders httpHeaders,
            String body
    )  throws IOException {
        return getCloud11Service().all(httpHeaders, body).build();
    }

    @POST
    @Path("users")
    public Response createUser(
            @Context HttpHeaders httpHeaders,
            User user
    ) throws IOException {
        return getCloud11Service().createUser(httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}")
    public Response getUser(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUser(userId, httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().deleteUser(userId, httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}")
    public Response updateUser(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders,
            User user) throws IOException {
        return getCloud11Service().updateUser(userId, httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/enabled")
    public Response getUserEnabled(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().getUserEnabled(userId, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/key")
    public Response getUserKey(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
         return getCloud11Service().getUserKey(userId, httpHeaders).build();
    }

    @PUT
    @Path("users/{userId}/key")
    public Response setUserKey(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders,
            UserWithOnlyKey user
    ) throws IOException {
         return getCloud11Service().setUserKey(userId, httpHeaders, user).build();
    }

    @GET
    @Path("users/{userId}/serviceCatalog")
    public Response getServiceCatalog(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
         return getCloud11Service().getServiceCatalog(userId, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs")
    public Response getBaseURLRefs(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
         return getCloud11Service().getBaseURLRefs(userId, httpHeaders).build();
    }

    @POST
    @Path("users/{userId}/baseURLRefs")
    public Response addBaseURLRef(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            BaseURLRef baseUrlRef
    ) throws IOException {
        return getCloud11Service().addBaseURLRef(userId, httpHeaders, uriInfo, baseUrlRef).build();
    }

    @GET
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response getBaseURLRef(
            @PathParam("userId") String userId,
            @PathParam("baseURLId") String baseURLId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
         return getCloud11Service().getBaseURLRef(userId, baseURLId, httpHeaders).build();
    }

    @DELETE
    @Path("users/{userId}/baseURLRefs/{baseURLId}")
    public Response deleteBaseURLRef(
            @PathParam("userId") String userId,
            @PathParam("baseURLId") String baseURLId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        return getCloud11Service().deleteBaseURLRef(userId, baseURLId, httpHeaders).build();
    }

    @GET
    @Path("users/{userId}/groups")
    public Response getUserGroups(
            @PathParam("userId") String userId,
            @Context HttpHeaders httpHeaders
    ) throws IOException {
         return getCloud11Service().getUserGroups(userId, httpHeaders).build();
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
