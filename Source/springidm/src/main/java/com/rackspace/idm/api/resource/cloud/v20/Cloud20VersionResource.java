package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
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
import javax.xml.bind.JAXBException;

import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
    MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    private final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    private DelegateCloud20Service delegateCloud20Service;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public Cloud20VersionResource(Configuration config,
        CloudClient cloudClient,
        CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
    }

    @GET
    public Response getPublicCloud20VersionInfo(@Context HttpHeaders httpHeaders)
        throws IOException {
        // For the pubic profile, we're just forwarding to what cloud has. Once
        // we become the
        // source of truth, we should use the CloudContractDescriptorBuilder to
        // render this.
        return cloudClient.get(getCloudAuthV20Url(), httpHeaders).build();
    }

    public Response getInternalCloud20VersionInfo() {
        final String responseXml = cloudContractDescriptionBuilder
            .buildInternalVersionPage(
                CloudContractDescriptionBuilder.VERSION_2_0, uriInfo);
        return Response.ok(responseXml).build();
    }

    private String getCloudAuthV20Url() {
        return config.getString("cloudAuth20url");
    }

    @POST
    @Path("tokens")
    public Response authenticate(@Context HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException,
        JAXBException {
        return getCloud20Service().authenticate(httpHeaders,
            authenticationRequest).build();
    }

    @GET
    @Path("tokens/{tokenId}")
    public Response validateToken(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tokenId") String tokenId,
        @QueryParam("belongsTo") String belongsTo) throws IOException {
        return getCloud20Service().validateToken(httpHeaders, authToken,
            tokenId, belongsTo).build();
    }

    @HEAD
    @Path("tokens/{tokenId}")
    public Response checkToken(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tokenId") String tokenId,
        @QueryParam("belongsTo") String belongsTo) throws IOException {
        return getCloud20Service().validateToken(httpHeaders, authToken,
            tokenId, belongsTo).build();
    }

    @GET
    @Path("tokens/{tokenId}/endpoints")
    public Response listEndpointsForToken(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tokenId") String tokenId) throws IOException {
        return getCloud20Service().listEndpointsForToken(httpHeaders,
            authToken, tokenId).build();
    }

    @GET
    @Path("extensions")
    public Response listExtensions(@Context HttpHeaders httpHeaders)
        throws IOException {
        return getCloud20Service().listExtensions(httpHeaders).build();
    }

    @GET
    @Path("extensions/{alias}")
    public Response getExtension(@Context HttpHeaders httpHeaders,
        @PathParam("alias") String alias) throws IOException {
        return getCloud20Service().getExtension(httpHeaders, alias).build();
    }

    @GET
    @Path("users")
    public Response getUserByName(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @QueryParam("name") String name, @QueryParam("marker") String marker,
        @QueryParam("limit") int limit) throws IOException {
        if (StringUtils.isBlank(name)) {
            return getCloud20Service().listUsers(httpHeaders, authToken,
                marker, limit).build();
        } else {
            return getCloud20Service().getUserByName(httpHeaders, authToken,
                name).build();
        }
    }

    @GET
    @Path("users/{userId}")
    public Response getUserById(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().getUserById(httpHeaders, authToken, userId)
            .build();
    }

    @GET
    @Path("users/{userId}/roles")
    public Response listUserGlobalRoles(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        @QueryParam("serviceId") String serviceId) throws IOException {
        if (!StringUtils.isBlank(serviceId)) {
            return getCloud20Service().listUserGlobalRolesByServiceId(httpHeaders,
                authToken, userId, serviceId).build();
        } else {
            return getCloud20Service().listUserGlobalRoles(httpHeaders,
                authToken, userId).build();
        }
    }

    @GET
    @Path("tenants")
    public Response listTenants_getTenantByName(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @QueryParam("name") String name, @QueryParam("marker") String marker,
        @QueryParam("limit") Integer limit) throws IOException {
        // Note: getTenantByName only available to admin
        if (!StringUtils.isBlank(name)) {
            return getCloud20Service().getTenantByName(httpHeaders, authToken,
                name).build();
        } else {
            return getCloud20Service().listTenants(httpHeaders, authToken,
                marker, limit).build();
        }
    }

    @GET
    @Path("tenants/{tenantId}")
    public Response getTenantById(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantsId) throws IOException {
        return getCloud20Service().getTenantById(httpHeaders, authToken,
            tenantsId).build();
    }

    @GET
    @Path("tenants/{tenantId}/users/{userId}/roles")
    public Response listRolesForUserOnTenant(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().listRolesForUserOnTenant(httpHeaders,
            authToken, tenantId, userId).build();
    }

    @POST
    @Path("users")
    public Response addUser(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        User user) throws IOException, JAXBException {
        return getCloud20Service().addUser(httpHeaders, uriInfo, authToken,
            user).build();
    }

    @POST
    @Path("users/{userId}")
    public Response updateUser(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, User user) throws IOException,
        JAXBException {
        return getCloud20Service().updateUser(httpHeaders, authToken, userId,
            user).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().deleteUser(httpHeaders, authToken, userId)
            .build();
    }

    @PUT
    @Path("users/{userId}/OS-KSADM/enabled")
    public Response setUserEnabled(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, User user)
        throws IOException, JAXBException {
        return getCloud20Service().setUserEnabled(httpHeaders, authToken,
            userId, user).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/roles")
    public Response listUserRoles(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        @QueryParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().listUserRoles(httpHeaders, authToken,
            userId, serviceId).build();
    }

    @GET
    @Path("users/{userId}/RAX-KSGRP")
    public Response listUserGroups(@Context HttpHeaders httpHeaders, @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().listUserGroups(httpHeaders,authToken, userId).build();
    }

    @PUT
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addUserRole(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, @PathParam("roleId") String roleId)
        throws IOException {
        return getCloud20Service().addUserRole(httpHeaders, authToken, userId,
            roleId).build();
    }

    @GET
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response getUserRole(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, @PathParam("roleId") String roleId)
        throws IOException {
        return getCloud20Service().getUserRole(httpHeaders, authToken, userId,
            roleId).build();
    }

    @DELETE
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteUserRole(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, @PathParam("roleId") String roleId)
        throws IOException {
        return getCloud20Service().deleteUserRole(httpHeaders, authToken,
            userId, roleId).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response addUserCredential(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, String body) throws IOException {
        return getCloud20Service().addUserCredential(httpHeaders, authToken,
            userId, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response listCredentials(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        @QueryParam("marker") String marker, @QueryParam("limit") Integer limit)
        throws IOException {
        return getCloud20Service().listCredentials(httpHeaders, authToken,
            userId, marker, limit).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response updateUserPasswordCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        PasswordCredentialsRequiredUsername creds) throws IOException,
        JAXBException {
        return getCloud20Service().updateUserPasswordCredentials(httpHeaders,
            authToken, userId, "passwordCredentials", creds).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response updateUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, ApiKeyCredentials creds)
        throws IOException, JAXBException {
        return getCloud20Service().updateUserApiKeyCredentials(httpHeaders,
            authToken, userId, JSONConstants.APIKEY_CREDENTIALS, creds).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response getUserCredentialKey(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        @PathParam("credentialType") String credentialType) throws IOException {
        return getCloud20Service().getUserCredential(httpHeaders, authToken,
            userId, JSONConstants.APIKEY_CREDENTIALS).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response getUserCredential(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().getUserCredential(httpHeaders, authToken,
            userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response deleteUserKeyCredential(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        @PathParam("credentialType") String credentialType) throws IOException {
        return getCloud20Service().deleteUserCredential(httpHeaders, authToken,
            userId, JSONConstants.APIKEY_CREDENTIALS).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response deleteUserCredential(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().deleteUserCredential(httpHeaders, authToken,
            userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @POST
    @Path("tenants")
    public Response addTenant(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        Tenant tenant) throws IOException, JAXBException {
        return getCloud20Service().addTenant(httpHeaders, uriInfo, authToken,
            tenant).build();
    }

    @POST
    @Path("tenants/{tenantId}")
    public Response updateTenant(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId, Tenant tenant)
        throws IOException, JAXBException {
        return getCloud20Service().updateTenant(httpHeaders, authToken,
            tenantId, tenant).build();
    }

    @DELETE
    @Path("tenants/{tenantId}")
    public Response deleteTenant(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId) throws IOException {
        return getCloud20Service().deleteTenant(httpHeaders, authToken,
            tenantId).build();
    }

    @GET
    @Path("tenants/{tenantId}/OS-KSADM/roles")
    public Response listRolesForTenant(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @QueryParam("marker") String marker, @QueryParam("limit") Integer limit)
        throws IOException {
        return getCloud20Service().listRolesForTenant(httpHeaders, authToken,
            tenantId, marker, limit).build();
    }

    @GET
    @Path("tenants/{tenantId}/users")
    public Response listUsersForTenant_listUsersWithRoleForTenant(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @QueryParam("roleId") String roleId,
        @QueryParam("marker") String marker, @QueryParam("limit") Integer limit)
        throws IOException {
        if (roleId != null) {
            return getCloud20Service().listUsersWithRoleForTenant(httpHeaders,
                authToken, tenantId, roleId, marker, limit).build();
        } else {
            return getCloud20Service().listUsersForTenant(httpHeaders,
                authToken, tenantId, marker, limit).build();
        }
    }

    @PUT
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addRolesToUserOnTenant(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId, @PathParam("roleId") String roleId)
        throws IOException {
        return getCloud20Service().addRolesToUserOnTenant(httpHeaders,
            authToken, tenantId, userId, roleId).build();
    }

    @DELETE
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteRoleFromUserOnTenant(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId, @PathParam("roleId") String roleId)
        throws IOException {
        return getCloud20Service().deleteRoleFromUserOnTenant(httpHeaders,
            authToken, tenantId, userId, roleId).build();
    }

    @GET
    @Path("OS-KSADM/roles")
    public Response listRoles(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @QueryParam("serviceId") String serviceId,
        @QueryParam("marker") String marker, @QueryParam("limit") Integer limit)
        throws IOException {
        return getCloud20Service().listRoles(httpHeaders, authToken, serviceId,
            marker, limit).build();
    }

    @POST
    @Path("OS-KSADM/roles")
    public Response addRole(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        Role role) throws IOException, JAXBException {
        return getCloud20Service().addRole(httpHeaders, uriInfo, authToken,
            role).build();
    }

    @GET
    @Path("OS-KSADM/roles/{roleId}")
    public Response getRole(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().getRole(httpHeaders, authToken, roleId)
            .build();
    }

    @DELETE
    @Path("OS-KSADM/roles/{roleId}")
    public Response deleteRole(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("roleId") String roleId) throws IOException {
        return getCloud20Service().deleteRole(httpHeaders, authToken, roleId)
            .build();
    }

    @GET
    @Path("OS-KSADM/services")
    public Response listServices(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @QueryParam("marker") String marker, @QueryParam("limit") Integer limit)
        throws IOException {
        return getCloud20Service().listServices(httpHeaders, authToken, marker,
            limit).build();
    }

    @POST
    @Path("OS-KSADM/services")
    public Response addService(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        Service service) throws IOException, JAXBException {
        return getCloud20Service().addService(httpHeaders, uriInfo, authToken,
            service).build();
    }

    @GET
    @Path("OS-KSADM/services/{serviceId}")
    public Response getService(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service()
            .getService(httpHeaders, authToken, serviceId).build();
    }

    @DELETE
    @Path("OS-KSADM/services/{serviceId}")
    public Response deleteService(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().deleteService(httpHeaders, authToken,
            serviceId).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response listEndpointTemplates(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @QueryParam("serviceId") String serviceId) throws IOException {
        return getCloud20Service().listEndpointTemplates(httpHeaders,
            authToken, serviceId).build();
    }

    @POST
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response addEndpointTemplate(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        EndpointTemplate endpoint) throws IOException, JAXBException {
        return getCloud20Service().addEndpointTemplate(httpHeaders, uriInfo,
            authToken, endpoint).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response getEndpointTemplate(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("endpointTemplateId") String endpointTemplateId)
        throws IOException {
        return getCloud20Service().getEndpointTemplate(httpHeaders, authToken,
            endpointTemplateId).build();
    }

    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response deleteEndpointTemplate(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("endpointTemplateId") String enpdointTemplateId)
        throws IOException {
        return getCloud20Service().deleteEndpointTemplate(httpHeaders,
            authToken, enpdointTemplateId).build();
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response listEndpoints(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId) throws IOException {
        return getCloud20Service().listEndpoints(httpHeaders, authToken,
            tenantId).build();
    }

    @POST
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response addEndpoint(@Context HttpHeaders httpHeaders,
        @Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId, EndpointTemplate endpoint)
        throws IOException, JAXBException {
        return getCloud20Service().addEndpoint(httpHeaders, authToken,
            tenantId, endpoint).build();
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response getEndpoint(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("endpointId") String endpointId) throws IOException {
        return getCloud20Service().getEndpoint(httpHeaders, authToken,
            tenantId, endpointId).build();
    }

    @DELETE
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response deleteEndpoint(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("endpointId") String endpointId) throws IOException {
        return getCloud20Service().deleteEndpoint(httpHeaders, authToken,
            tenantId, endpointId).build();
    }

    @GET
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response getSecretQA(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) throws IOException {
        return getCloud20Service().getSecretQA(httpHeaders, authToken, userId)
            .build();
    }

    @PUT
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response updateSecretQA(@Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, SecretQA secrets)
        throws IOException, JAXBException {
        return getCloud20Service().updateSecretQA(httpHeaders, authToken,
            userId, secrets).build();
    }

    private Cloud20Service getCloud20Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }
}
