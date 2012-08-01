package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:15 PM
 */
public interface Cloud20Service {

    ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest);

    ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo);

    ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) ;

    ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) ;

    ResponseBuilder listExtensions(HttpHeaders httpHeaders) ;

    ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) ;

    ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) ;

    ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) ;

    ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) ;

    ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) ;

    ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) ;

    ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit) ;

    ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) ;

    ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) ;

    ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantsId, String userId) ;

	ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, UserForCreate user);

	ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user);

	ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId);

	ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, User user);

	ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Tenant tenant);

	ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Tenant tenant);

	ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) ;

	ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) ;

	ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String roleId, String marker, Integer limit) ;

	ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) ;

	ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) ;

	ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) ;

	ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) ;

	ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role);

	ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) ;

	ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) ;

	ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) ;

	ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service);

	ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) ;

	ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) ;

    ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) ;

    ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId) ;

    ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, EndpointTemplate endpoint);

    ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) ;

    ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String enpdointTemplateId) ;

    ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken,
        String tenantId) ;

    ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        EndpointTemplate endpoint);

    ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        String endpointId) ;

    ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        String endpointId) ;

    ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType,
        PasswordCredentialsRequiredUsername creds);

    ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType,
        ApiKeyCredentials creds);

    ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken,
        String userId) ;

    ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken,
        String userId, SecretQA secrets);

    ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders,
        String authToken, String userId, String serviceId) ;

    ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String marker, String s, Integer limit) ;

    ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId) ;

    ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Group group);

    ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId, Group group);

    ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId) ;

    ResponseBuilder addUserToGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) ;

    ResponseBuilder removeUserFromGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) ;

    ResponseBuilder getUsersForGroup(HttpHeaders httpHeaders, String authToken, String groupId, String marker, Integer limit) ;

    ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName) ;

    ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest);

    ResponseBuilder listDefaultRegionServices(String authToken);

    ResponseBuilder setDefaultRegionServices(String authToken, DefaultRegionServices defaultRegionServices);
}
