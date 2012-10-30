package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    private final Configuration config;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    private DelegateCloud20Service delegateCloud20Service;

    @Autowired
    public Cloud20VersionResource(Configuration config,
        CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.config = config;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
    }

    @GET
    public Response getCloud20VersionInfo() throws JAXBException {
        String responseXml = cloudContractDescriptionBuilder.buildVersion20Page();
        JAXBContext context = JAXBContext.newInstance("org.openstack.docs.common.api.v1:org.w3._2005.atom");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoice> versionChoice = (JAXBElement<VersionChoice>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice.getValue()).build();
    }
    // Methods are currently not being used
    /*
    public Response getInternalCloud20VersionInfo() {
        final String responseXml =
                cloudContractDescriptionBuilder.buildInternalVersionPage(CloudContractDescriptionBuilder.VERSION_2_0, uriInfo);
        return Response.ok(responseXml).build();
    }

    private String getCloudAuthV20Url() {
        return config.getString("cloudAuth20url");
    }
    */
    @POST
    @Path("tokens")
    public Response authenticate(@Context HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        return getCloud20Service().authenticate(httpHeaders, authenticationRequest).build();
    }

    @DELETE
    @Path("tokens")
    public Response revokeToken(@Context HttpHeaders httpHeaders, @HeaderParam(X_AUTH_TOKEN) String authToken) {
        return defaultCloud20Service.revokeToken(httpHeaders, authToken).build();
    }

    @DELETE
    @Path("tokens/{tokenId}")
    public Response revokeUserToken(@Context HttpHeaders httpHeaders,
                                    @HeaderParam(X_AUTH_TOKEN) String authToken,
                                    @PathParam("tokenId") String tokenId) {
        return defaultCloud20Service.revokeToken(httpHeaders, authToken, tokenId).build();
    }

    @GET
    @Path("tokens/{tokenId}")
    public Response validateToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo) {
        return getCloud20Service().validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
    }

    @HEAD
    @Path("tokens/{tokenId}")
    public Response checkToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo) {
        return getCloud20Service().validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
    }

    @GET
    @Path("tokens/{tokenId}/endpoints")
    public Response listEndpointsForToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId) {
        return getCloud20Service().listEndpointsForToken(httpHeaders, authToken, tokenId).build();
    }

    @GET
    @Path("RAX-AUTH/default-region/services")
    public Response listDefaultRegionServices(
            @HeaderParam(X_AUTH_TOKEN) String authToken){
        return defaultCloud20Service.listDefaultRegionServices(authToken).build();
    }

    @PUT
    @Path("RAX-AUTH/default-region/services")
    public Response setDefaultRegionServices(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                             DefaultRegionServices defaultRegionServices){
        return defaultCloud20Service.setDefaultRegionServices(authToken,defaultRegionServices).build();
    }

    @POST
    @Path("RAX-AUTH/impersonation-tokens")
    public Response impersonate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            ImpersonationRequest impersonationRequest) {
        return defaultCloud20Service.impersonate(httpHeaders, authToken, impersonationRequest).build();
    }

    @POST
    @Path("RAX-AUTH/domains")
    public Response addDomain(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Domain domain) {
        return defaultCloud20Service.addDomain(authToken, uriInfo, domain).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}")
    public Response getDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return defaultCloud20Service.getDomain(authToken, domainId).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}")
    public Response updateDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            Domain domain) {
        return defaultCloud20Service.updateDomain(authToken, domainId, domain).build();
    }

    @DELETE
    @Path("RAX-AUTH/domains/{domainId}")
    public Response deleteDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return defaultCloud20Service.deleteDomain(authToken, domainId).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/tenants")
    public Response getDomainTenantsByDomainId(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled) {
        return defaultCloud20Service.getDomainTenants(authToken, domainId, enabled).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response addTenantToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return defaultCloud20Service.addTenantToDomain(authToken, domainId, tenantId).build();
    }

    @DELETE
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response removeTenantFromDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return defaultCloud20Service.removeTenantFromDomain(authToken, domainId, tenantId).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/users")
    public Response getUsersByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled) {
        return defaultCloud20Service.getUsersByDomainId(authToken, domainId, enabled).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}/users/{userId}")
    public Response addUserToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("userId") String userId) {
        return defaultCloud20Service.addUserToDomain(authToken, domainId, userId).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/endpoints")
    public Response getEndpointsByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return defaultCloud20Service.getEndpointsByDomainId(authToken, domainId).build();
    }

    @GET
    @Path("RAX-AUTH/policies")
    public Response getPolicies(
            @HeaderParam(X_AUTH_TOKEN) String authToken) {
        return defaultCloud20Service.getPolicies(authToken).build();
    }

    @POST
    @Path("RAX-AUTH/policies")
    public Response addPolicy(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @Context UriInfo uriInfo,
            Policy policy) {
        return defaultCloud20Service.addPolicy(uriInfo, authToken, policy).build();
    }

    @GET
    @Path("RAX-AUTH/policies/{policyId}")
    public Response getPolicy(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("policyId") String policyId) {
        return defaultCloud20Service.getPolicy(authToken, policyId).build();
    }

    @PUT
    @Path("RAX-AUTH/policies/{policyId}")
    public Response updatePolicy(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("policyId") String policyId,
            Policy policy) {
        return defaultCloud20Service.updatePolicy(authToken, policyId, policy).build();
    }

    @DELETE
    @Path("RAX-AUTH/policies/{policyId}")
    public Response deletePolicy(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("policyId") String policyId) {
        return defaultCloud20Service.deletePolicy(authToken, policyId).build();
    }

    @GET
    @Path("RAX-AUTH/domains")
    public Response getAccessibleDomains(
            @HeaderParam(X_AUTH_TOKEN) String authToken) {
        return defaultCloud20Service.getAccessibleDomains(authToken).build();
    }

    @GET
    @Path("extensions")
    public Response listExtensions(@Context HttpHeaders httpHeaders) {
        return getCloud20Service().listExtensions(httpHeaders).build();
    }

    @GET
    @Path("extensions/{alias}")
    public Response getExtension(@Context HttpHeaders httpHeaders, @PathParam("alias") String alias) {
        return getCloud20Service().getExtension(httpHeaders, alias).build();
    }

    @GET
    @Path("users")
    public Response getUserByName(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("marker") int marker,
            @QueryParam("limit") int limit) {
        if (StringUtils.isBlank(name)) {
            return getCloud20Service().listUsers(httpHeaders, authToken, marker, limit).build();
        } else {
            return getCloud20Service().getUserByName(httpHeaders, authToken, name).build();
        }
    }

    @GET
    @Path("users/{userId}")
    public Response getUserById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().getUserById(httpHeaders, authToken, userId).build();
    }

    @GET
    @Path("users/{userId}/roles")
    public Response listUserGlobalRoles(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("serviceId") String serviceId) {
        if (!StringUtils.isBlank(serviceId)) {
            return getCloud20Service().listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, serviceId).build();
        } else {
            return getCloud20Service().listUserGlobalRoles(httpHeaders, authToken, userId).build();
        }
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/domains")
    public Response getAccessibleDomainsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().getAccessibleDomainsForUser(authToken, userId).build();
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/domains/{domainId}/endpoints")
    public Response getAccessibleDomainsEndpointsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("domainId") String domainId) {
        return getCloud20Service().getAccessibleDomainsEndpointsForUser(authToken, userId, domainId).build();
    }

    @GET
    @Path("tenants")
    public Response listTenantsAndGetTenantByName(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        // Note: getTenantByName only available to admin
        if (!StringUtils.isBlank(name)) {
            return getCloud20Service().getTenantByName(httpHeaders, authToken, name).build();
        } else {
            return getCloud20Service().listTenants(httpHeaders, authToken, marker, limit).build();
        }
    }

    @GET
    @Path("tenants/{tenantId}")
    public Response getTenantById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantsId) {
        return getCloud20Service().getTenantById(httpHeaders, authToken, tenantsId).build();
    }

    @GET
    @Path("tenants/{tenantId}/users/{userId}/roles")
    public Response listRolesForUserOnTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("userId") String userId) {
        return getCloud20Service().listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId).build();
    }

    @POST
    @Path("users")
    public Response addUser(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, UserForCreate user) {

        return getCloud20Service().addUser(httpHeaders, uriInfo, authToken, user).build();
    }

    @POST
    @Path("users/{userId}")
    public Response updateUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, UserForCreate user) {
        return getCloud20Service().updateUser(httpHeaders, authToken, userId, user).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().deleteUser(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("users/{userId}/OS-KSADM/enabled")
    public Response setUserEnabled(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, User user) {
        return getCloud20Service().setUserEnabled(httpHeaders, authToken, userId, user).build();
    }

    @GET
    @Path("users/{userId}/RAX-KSGRP")
    public Response listUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().listUserGroups(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().addUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @GET
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response getUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().getUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @DELETE
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().deleteUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response addUserCredential(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, String body) {
        return getCloud20Service().addUserCredential(httpHeaders, uriInfo, authToken, userId, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response listCredentials(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        return getCloud20Service().listCredentials(httpHeaders, authToken, userId, marker, limit).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response updateUserPasswordCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        PasswordCredentialsRequiredUsername creds) {
        return getCloud20Service().updateUserPasswordCredentials(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS, creds).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response updateUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, ApiKeyCredentials creds) {
        return getCloud20Service().updateUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.APIKEY_CREDENTIALS, creds).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS + "/RAX-AUTH/reset")
    public Response resetUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) {
        return getCloud20Service().resetUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.APIKEY_CREDENTIALS).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response getUserCredentialKey(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().getUserCredential(httpHeaders, authToken, userId, JSONConstants.APIKEY_CREDENTIALS).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response getUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().getUserCredential(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.APIKEY_CREDENTIALS)
    public Response deleteUserKeyCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.APIKEY_CREDENTIALS).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response deleteUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @POST
    @Path("tenants")
    public Response addTenant(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Tenant tenant) {
        return getCloud20Service().addTenant(httpHeaders, uriInfo, authToken, tenant).build();
    }

    @POST
    @Path("tenants/{tenantId}")
    public Response updateTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, Tenant tenant) {
        return getCloud20Service().updateTenant(httpHeaders, authToken, tenantId, tenant).build();
    }

    @DELETE
    @Path("tenants/{tenantId}")
    public Response deleteTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return getCloud20Service().deleteTenant(httpHeaders, authToken, tenantId).build();
    }

    @GET
    @Path("tenants/{tenantId}/OS-KSADM/roles")
    public Response listRolesForTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        return getCloud20Service().listRolesForTenant(httpHeaders, authToken, tenantId, marker, limit).build();
    }

    @GET
    @Path("tenants/{tenantId}/users")
    public Response listUsersForTenantAndListUsersWithRoleForTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("roleId") String roleId,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        if (roleId != null) {
            return getCloud20Service().listUsersWithRoleForTenant(httpHeaders, authToken, tenantId, roleId, marker, limit).build();
        } else {
            return getCloud20Service().listUsersForTenant(httpHeaders, authToken, tenantId, marker, limit).build();
        }
    }

    @PUT
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addRolesToUserOnTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().addRolesToUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId).build();
    }

    @DELETE
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteRoleFromUserOnTenant(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId,
        @PathParam("roleId") String roleId) {
        return getCloud20Service().deleteRoleFromUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId).build();
    }

    @GET
    @Path("OS-KSADM/roles")
    public Response listRoles(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("serviceId") String serviceId,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        return getCloud20Service().listRoles(httpHeaders, authToken, serviceId, marker, limit).build();
    }

    @POST
    @Path("OS-KSADM/roles")
    public Response addRole(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Role role) {
        return getCloud20Service().addRole(httpHeaders, uriInfo, authToken, role).build();
    }

    @GET
    @Path("OS-KSADM/roles/{roleId}")
    public Response getRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().getRole(httpHeaders, authToken, roleId).build();
    }

    @DELETE
    @Path("OS-KSADM/roles/{roleId}")
    public Response deleteRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        return getCloud20Service().deleteRole(httpHeaders, authToken, roleId).build();
    }

    @GET
    @Path("OS-KSADM/services")
    public Response listServices(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit)
        {
        return getCloud20Service().listServices(httpHeaders, authToken, marker, limit).build();
    }

    @POST
    @Path("OS-KSADM/services")
    public Response addService(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Service service) {
        return getCloud20Service().addService(httpHeaders, uriInfo, authToken, service).build();
    }

    @GET
    @Path("OS-KSADM/services/{serviceId}")
    public Response getService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        return getCloud20Service().getService(httpHeaders, authToken, serviceId).build();
    }

    @DELETE
    @Path("OS-KSADM/services/{serviceId}")
    public Response deleteService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        return getCloud20Service().deleteService(httpHeaders, authToken, serviceId).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response listEndpointTemplates(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("serviceId") String serviceId) {
        return getCloud20Service().listEndpointTemplates(httpHeaders, authToken, serviceId).build();
    }

    @POST
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response addEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, EndpointTemplate endpoint) {
        return getCloud20Service().addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response getEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId) {
        return getCloud20Service().getEndpointTemplate(httpHeaders, authToken, endpointTemplateId).build();
    }

    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response deleteEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String enpdointTemplateId) {
        return getCloud20Service().deleteEndpointTemplate(httpHeaders, authToken, enpdointTemplateId).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/policies")
    public Response getPoliciesForEndpointTemplate(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId) {
        return getCloud20Service().getPoliciesForEndpointTemplate(authToken, endpointTemplateId).build();
    }

    @PUT
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/policies")
    public Response updatePoliciesForEndpointTemplate(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            Policies policies
            ) {
        return getCloud20Service().updatePoliciesForEndpointTemplate(authToken, endpointTemplateId, policies).build();
    }

    @PUT
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/policies/{policyId}")
    public Response addPolicyToEndpointTemplate(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            @PathParam("policyId") String policyId){
        return getCloud20Service().addPolicyToEndpointTemplate(authToken, endpointTemplateId, policyId).build();
    }

    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/policies/{policyId}")
    public Response deletePolicyToEndpointTemplate(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            @PathParam("policyId") String policyId){
        return getCloud20Service().deletePolicyToEndpointTemplate(authToken, endpointTemplateId, policyId).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/capabilities")
    public Response getCapabilities(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId){
        return getCloud20Service().getCapabilities(authToken, endpointTemplateId).build();
    }

    @PUT
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/capabilities")
    public Response updateCapabilities(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            Capabilities capabilities){
        return getCloud20Service().updateCapabilities(authToken, endpointTemplateId, capabilities).build();
    }

    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/capabilities")
    public Response removeCapabilities(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId){
        return getCloud20Service().removeCapabilities(authToken, endpointTemplateId).build();
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}/RAX-AUTH/capabilities/{capabilityId}")
    public Response getCapabilities(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            @PathParam("capabilityId") String capabilityId){
        return getCloud20Service().getCapability(authToken, capabilityId, endpointTemplateId).build();
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response listEndpoints(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return getCloud20Service().listEndpoints(httpHeaders, authToken, tenantId).build();
    }

    @POST
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response addEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, EndpointTemplate endpoint) {
        return getCloud20Service().addEndpoint(httpHeaders, authToken, tenantId, endpoint).build();
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response getEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return getCloud20Service().getEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @DELETE
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response deleteEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return getCloud20Service().deleteEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @GET
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response getSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return getCloud20Service().getSecretQA(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response updateSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, SecretQA secrets) {
        return getCloud20Service().updateSecretQA(httpHeaders, authToken, userId, secrets).build();
    }

    // ******************************************************* //
    // RAX-GRPADM Extension //
    // ******************************************************* //

    @POST
    @Path("/RAX-GRPADM/groups")
    public Response addGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Group group) {
        return getCloud20Service().addGroup(httpHeaders, uriInfo, authToken, group).build();
    }

    @GET
    @Path("/RAX-GRPADM/groups")
    public Response getGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String groupName,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        if(groupName != null){
            return getCloud20Service().getGroup(httpHeaders, authToken, groupName).build();
        }
        return getCloud20Service().listGroups(httpHeaders, authToken, groupName, marker, limit).build();
    }

    @GET
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response getGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        return getCloud20Service().getGroupById(httpHeaders, authToken, groupId).build();
    }

    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response updateGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            Group group) {
        return getCloud20Service().updateGroup(httpHeaders, authToken, groupId, group).build();
    }

    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response deleteGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        return getCloud20Service().deleteGroup(httpHeaders, authToken, groupId).build();
    }

    @GET
    @Path("/RAX-GRPADM/groups/{groupId}/users")
    public Response getUsersFromGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @QueryParam("marker") String marker,
            @QueryParam("limit") Integer limit) {
        return getCloud20Service().getUsersForGroup(httpHeaders, authToken, groupId, marker, limit).build();
    }

    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response putUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return getCloud20Service().addUserToGroup(httpHeaders, authToken, groupId, userId).build();
    }

    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response deleteUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return getCloud20Service().removeUserFromGroup(httpHeaders, authToken, groupId, userId).build();
    }


    @DELETE
    @Path("softDeleted/users/{userId}")
    public Response deleteSoftDeletedUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        if(config.getBoolean("allowSoftDeleteDeletion")){
            return getCloud20Service().deleteUserFromSoftDeleted(httpHeaders, authToken, userId).build();
        }
        else{
            throw new NotFoundException("Not Found");
        }
    }

    @POST
    @Path("RAX-AUTH/regions")
    public Response createRegion(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, Region region) {
        return getCloud20Service().addRegion(uriInfo, authToken, region).build();
    }

    @GET
    @Path("RAX-AUTH/regions/{name}")
    public Response getRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        return getCloud20Service().getRegion(authToken, name).build();
    }

    @GET
    @Path("RAX-AUTH/regions")
    public Response getRegions(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return getCloud20Service().getRegions(authToken).build();
    }

    @PUT
    @Path("RAX-AUTH/regions/{name}")
    public Response updateRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name, Region region) {
        return getCloud20Service().updateRegion(authToken, name, region).build();
    }

    @DELETE
    @Path("RAX-AUTH/regions/{name}")
    public Response deleteRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        return getCloud20Service().deleteRegion(authToken, name).build();
    }

    Cloud20Service getCloud20Service() {
        if (config.getBoolean("useCloudAuth")) {
            return delegateCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }

    public void setDefaultCloud20Service(DefaultCloud20Service defaultCloud20Service) {
        this.defaultCloud20Service = defaultCloud20Service;
    }

    public void setDelegateCloud20Service(DelegateCloud20Service delegateCloud20Service) {
        this.delegateCloud20Service = delegateCloud20Service;
    }
}



