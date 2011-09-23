package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
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
    public Response getPublicCloud20VersionInfo(
            @Context HttpHeaders httpHeaders
    ) throws IOException {
        //For the pubic profile, we're just forwarding to what cloud has. Once we become the
        //source of truth, we should use the CloudContractDescriptorBuilder to render this.
        return cloudClient.get(getCloudAuthV20Url(), httpHeaders).build();
    }

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
        if(name != null) {
            return getCloud20Service().getUserByName(httpHeaders, name).build(); 
        } else {
            return getCloud20Service().listUsers(httpHeaders).build();
        }
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
    @Path("users/{userId}/credentials")
    public Response addUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().addUserCredential(httpHeaders, userId, body).build();
    }

    @GET
    @Path("users/{userId}/credentials")
    public Response listCredentials(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @QueryParam("marker") String marker,
        @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().listCredentials(httpHeaders, userId, marker, limit).build();
    }

    @POST
    @Path("users/{userId}/credentials/RAX-KSKEY:apikeyCredentials")
    public Response updateUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().updateUserCredential(httpHeaders, userId, body).build();
    }

    @GET
    @Path("users/{userId}/credentials/RAX-KSKEY:apikeyCredentials")
    public Response getUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().getUserCredential(httpHeaders, userId).build();
    }

    @DELETE
    @Path("users/{userId}/credentials/RAX-KSKEY:apikeyCredentials")
    public Response deleteUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().deleteUserCredential(httpHeaders, userId).build();
    }

    @GET
    @Path("tenants/{tenantId}/users/{userId}/roles")
    public Response listRolesForUserOnTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().listRolesForUserOnTenant(httpHeaders, tenantId, userId).build();
    }

    @POST
    @Path("users")
    public Response addUser(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().addUser(httpHeaders, body).build();
    }

    @POST
    @Path("users/{userId}")
    public Response updateUser(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().updateUser(httpHeaders, userId, body).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().deleteUser(httpHeaders, userId).build();
    }

    @PUT
    @Path("users/{userId}/OS-KSADM/enabled")
    public Response setUserEnabled(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().setUserEnabled(httpHeaders, userId, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/roles")
    public Response listUserRoles(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @QueryParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().listUserRoles(httpHeaders, userId, serviceId).build();
    }

    @PUT
    @Path("users/{userId}/OS-KSADM/roles/{roleId}")
    public Response addUserRole(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().addUserRole(httpHeaders, userId, roleId).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/roles/{roleId}")
    public Response getUserRole(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().getUserRole(httpHeaders, userId, roleId).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/roles/{roleId}")
    public Response deleteUserRole(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().deleteUserRole(httpHeaders, userId, roleId).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response OS_KSADM_addUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().OS_KSADM_addUserCredential(httpHeaders, userId, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response OS_KSADM_listCredentials(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId, @QueryParam("marker") String marker,
        @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().OS_KSADM_listCredentials(httpHeaders, userId, marker, limit).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/{credentialType}")
    public Response OS_KSADM_updateUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId,
        @PathParam("credentialType") String credentialType, String body) throws IOException {
        return getCloud20Service().OS_KSADM_updateUserCredential(httpHeaders, userId, credentialType, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/{credentialType}")
    public Response OS_KSADM_getUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId,
        @PathParam("credentialType") String credentialType) throws IOException {
        return getCloud20Service().OS_KSADM_getUserCredential(httpHeaders, userId, credentialType).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/{credentialType}")
    public Response OS_KSADM_deleteUserCredential(@Context HttpHeaders httpHeaders, @PathParam("userId") String userId,
        @PathParam("credentialType") String credentialType) throws IOException {
        return getCloud20Service().OS_KSADM_deleteUserCredential(httpHeaders, userId, credentialType).build();
    }

    @POST
    @Path("tenants")
    public Response addTenant(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().addTenant(httpHeaders, body).build();
    }

    @POST
    @Path("tenants/{tenantId}")
    public Response updateTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId, String body) throws IOException {
        return getCloud20Service().updateTenant(httpHeaders, tenantId, body).build();
    }

    @DELETE
    @Path("tenants/{tenantId}")
    public Response deleteTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId) throws IOException {
        return getCloud20Service().deleteTenant(httpHeaders, tenantId).build();
    }

    @GET
    @Path("tenants/{tenantId}/OS-KSADM/roles")
    public Response listRolesForTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
    @QueryParam("marker") String marker, @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().listRolesForTenant(httpHeaders, tenantId, marker, limit).build();
    }

    @GET
    @Path("tenants/{tenantId}/users")
    public Response listUsersForTenant_listUsersWithRoleForTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
    @QueryParam("roleId") String roleId, @QueryParam("marker") String marker, @QueryParam("limit") Integer limit) throws IOException {
        if(roleId != null) {
            return getCloud20Service().listUsersWithRoleForTenant(httpHeaders, tenantId, roleId, marker, limit).build();
        } else {
            return getCloud20Service().listUsersForTenant(httpHeaders, tenantId, marker, limit).build();
        }
    }

    @PUT
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addRolesToUserOnTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
    @PathParam("userId") String userId, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().addRolesToUserOnTenant(httpHeaders, tenantId, userId, roleId).build();
    }

    @DELETE
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteRoleFromUserOnTenant(@Context HttpHeaders httpHeaders, @PathParam("tenantId") String tenantId,
    @PathParam("userId") String userId, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().deleteRoleFromUserOnTenant(httpHeaders, tenantId, userId, roleId).build();
    }

    @GET
    @Path("OS-KSADM/roles")
    public Response listRoles(@Context HttpHeaders httpHeaders, 
    @QueryParam("serviceId") String serviceId, @QueryParam("marker") String marker, @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().listRoles(httpHeaders, serviceId, marker, limit).build();
    }

    @POST
    @Path("OS-KSADM/roles")
    public Response addRole(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().addRole(httpHeaders, body).build();
    }

    @GET
    @Path("OS-KSADM/roles/{roleId}")
    public Response getRole(@Context HttpHeaders httpHeaders, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().getRole(httpHeaders, roleId).build();
    }

    @DELETE
    @Path("OS-KSADM/roles/{roleId}")
    public Response deleteRole(@Context HttpHeaders httpHeaders, @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().deleteRole(httpHeaders, roleId).build();
    }

    @GET
    @Path("OS-KSADM/services")
    public Response listServices(@Context HttpHeaders httpHeaders, 
    @QueryParam("marker") String marker, @QueryParam("limit") Integer limit) throws IOException {
        return getCloud20Service().listServices(httpHeaders, marker, limit).build();
    }

    @POST
    @Path("OS-KSADM/services")
    public Response addService(@Context HttpHeaders httpHeaders, String body) throws IOException {
        return getCloud20Service().addService(httpHeaders, body).build();
    }

    @GET
    @Path("OS-KSADM/services/{serviceId}")
    public Response getService(@Context HttpHeaders httpHeaders, @PathParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().getService(httpHeaders, serviceId).build();
    }

    @DELETE
    @Path("OS-KSADM/services/{serviceId}")
    public Response deleteService(@Context HttpHeaders httpHeaders, @PathParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().deleteService(httpHeaders, serviceId).build();
    }

    private Cloud20Service getCloud20Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }
}
