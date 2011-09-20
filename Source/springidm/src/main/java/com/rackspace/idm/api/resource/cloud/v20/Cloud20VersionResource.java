package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBElement;
import java.io.IOException;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    private DelegateCloud20Service delegateCloud20Service;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public Cloud20VersionResource(Configuration config, CloudClient cloudClient,
                                  CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
    }

    @GET()
    @Path("public")
    public Response getPublicCloud20VersionInfo(
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        //For the pubic profile, we're just forwarding to what cloud has. Once we become the
        //source of truth, we should use the CloudContractDescriptorBuilder to render this.
        return cloudClient.get(getCloudAuthV20Url(), httpHeaders).build();
    }

    @GET
    public Response getInternalCloud20VersionInfo() {
        final String responseXml = cloudContractDescriptionBuilder.buildInternalVersionPage(CloudContractDescriptionBuilder.VERSION_2_0, uriInfo);
        return Response.ok(responseXml).build();
    }

    private String getCloudAuthV20Url() {
        return  config.getString("cloudAuth20url");
    }

    @POST
    @Path("tokens")
    public Response authenticate(@Context HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException {
        return getCloud20Service().authenticate(httpHeaders,authenticationRequest).build();
    }

    @GET
    @Path("tokens/{tokenId}")
    public Response validateToken(@Context HttpHeaders httpHeaders, @PathParam("tokenId") String tokenId,
        @QueryParam("belongsTo") String belongsTo) throws IOException {
        return getCloud20Service().validateToken(httpHeaders, tokenId, belongsTo).build();
    }

    @GET
    @Path("tokens/{tokenId}/endpoints")
    public Response listEndpointsForToken(@Context HttpHeaders httpHeaders, @PathParam("tokenId") String tokenId) throws IOException {
        return getCloud20Service().listEndpointsForToken(httpHeaders, tokenId).build();
    }

    @GET
    @Path("extensions")
    public Response listExtensions(@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud20Service().listExtensions(httpHeaders).build();
    }

    @GET
    @Path("extensions/{alias}")
    public Response getExtension(@Context HttpHeaders httpHeaders, @PathParam("alias") String alias) throws IOException {
        return getCloud20Service().getExtension(httpHeaders, alias).build();
    }

    @GET
    @Path("users")
    public Response getUserByName(@Context HttpHeaders httpHeaders, @QueryParam("name") String name) throws IOException {
        return getCloud20Service().getUserByName(httpHeaders, name).build(); 
    }

    @GET
    @Path("users/{userId}")
    public Response getUserById(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().getUserById(httpHeaders, userId).build();
    }

    @GET
    @Path("users/{userId}/roles")
    public Response listUserGlobalRoles(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().listUserGlobalRoles(httpHeaders, userId).build();
    }

    @GET
    @Path("tenants")
    public Response listTenants_getTenantByName(
        @Context HttpHeaders httpHeaders, @QueryParam("name") String name, @QueryParam("marker") String marker,
        @QueryParam("limit") Integer limit) throws IOException {
        //Note: getTenantByName only available to admin
        if(name != null) {
            return getCloud20Service().getTenantByName(httpHeaders, name).build();
        } else {
            return getCloud20Service().listTenants(httpHeaders, marker, limit).build();
        }
    }

    @GET
    @Path("tenants/{tenantId}")
    public Response getTenantById(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantsId) throws IOException {
        return getCloud20Service().getTenantById(httpHeaders, tenantsId).build();
    }

    @POST
    @Path("users/credentials")
    public Response addUserCredential(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().addUserCredential(httpHeaders, body).build();
    }

    @GET
    @Path("users/credentials")
    public Response listCredentials(@Context HttpHeaders httpHeaders, @QueryParam("marker") String marker,
        @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().listCredentials(httpHeaders, marker, limit).build();
    }

    @POST
    @Path("users/credentials/RAX-KSKEY:apikeyCredentials")
    public Response updateUserCredential(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().updateUserCredential(httpHeaders, body).build();
    }

    @GET
    @Path("users/credentials/RAX-KSKEY:apikeyCredentials")
    public Response getUserCredential(@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud20Service().getUserCredential(httpHeaders).build();
    }

    @DELETE
    @Path("users/credentials/RAX-KSKEY:apikeyCredentials")
    public Response deleteUserCredential(@Context HttpHeaders httpHeaders) throws IOException {
        return getCloud20Service().deleteUserCredential(httpHeaders).build();
    }

    @GET
    @Path("tenants/{tenantId}/users/{userId}")
    public Response listRolesForUserOnTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().listRolesForUserOnTenant(httpHeaders, tenantId, userId).build();
    }

    private Cloud20Service getCloud20Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }
}
