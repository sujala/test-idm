package com.rackspace.idm.api.resource.cloud.v20;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:15 PM
 */
public interface Cloud20Service {
    Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException;

    Response.ResponseBuilder validateToken(HttpHeaders httpHeaders, String tokenId, String belongsTo) throws IOException;

    Response.ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String tokenId) throws IOException;

    Response.ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException;

    Response.ResponseBuilder getUserByName(HttpHeaders httpHeaders, String name) throws IOException;

    Response.ResponseBuilder getUserById(HttpHeaders httpHeaders, String userId) throws IOException;

    Response.ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String userId) throws IOException;

    Response.ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker, Integer limit) throws IOException;

    Response.ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name) throws IOException;

    Response.ResponseBuilder getTenantById(HttpHeaders httpHeaders, String tenantsId) throws IOException;

    Response.ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String userId, String body) throws IOException;

    Response.ResponseBuilder listCredentials(HttpHeaders httpHeaders, String userId, String marker, Integer limit) throws IOException;

    Response.ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String userId, String body) throws IOException;

    Response.ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String userId) throws IOException;

    Response.ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String userId) throws IOException;

    Response.ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String tenantsId, String userId) throws IOException;

	Response.ResponseBuilder listUsers(HttpHeaders httpHeaders) throws IOException;

	ResponseBuilder addUser(HttpHeaders httpHeaders, String body) throws IOException;

	ResponseBuilder updateUser(HttpHeaders httpHeaders, String userId, String body) throws IOException;

	ResponseBuilder deleteUser(HttpHeaders httpHeaders, String userId) throws IOException;

	ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String userId, String body) throws IOException;

	ResponseBuilder listUserRoles(HttpHeaders httpHeaders, String userId, String serviceId) throws IOException;

	ResponseBuilder addUserRole(HttpHeaders httpHeaders, String userId, String roleId) throws IOException;

	ResponseBuilder getUserRole(HttpHeaders httpHeaders, String userId, String roleId) throws IOException;

	ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String userId, String roleId) throws IOException;

	ResponseBuilder OS_KSADM_addUserCredential(HttpHeaders httpHeaders, String userId, String body) throws IOException;

	ResponseBuilder OS_KSADM_listCredentials(HttpHeaders httpHeaders, String userId, String marker, Integer limit) throws IOException;

	ResponseBuilder OS_KSADM_updateUserCredential(HttpHeaders httpHeaders, String userId, String credentialType, String body) throws IOException;

	ResponseBuilder OS_KSADM_getUserCredential(HttpHeaders httpHeaders, String userId, String credentialType) throws IOException;

	ResponseBuilder OS_KSADM_deleteUserCredential(HttpHeaders httpHeaders, String userId, String credentialType) throws IOException;

	ResponseBuilder addTenant(HttpHeaders httpHeaders, String body) throws IOException;

	ResponseBuilder updateTenant(HttpHeaders httpHeaders, String tenantId, String body) throws IOException;

	ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String tenantId) throws IOException;

	ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String tenantId, String roleId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder listRoles(HttpHeaders httpHeaders, String serviceId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRole(HttpHeaders httpHeaders, String body) throws IOException;

	ResponseBuilder getRole(HttpHeaders httpHeaders, String roleId) throws IOException;

	ResponseBuilder deleteRole(HttpHeaders httpHeaders, String roleId) throws IOException;

	ResponseBuilder listServices(HttpHeaders httpHeaders, String marker, Integer limit) throws IOException;

	ResponseBuilder addService(HttpHeaders httpHeaders, String body) throws IOException;

	ResponseBuilder getService(HttpHeaders httpHeaders, String serviceId) throws IOException;

	ResponseBuilder deleteService(HttpHeaders httpHeaders, String serviceId) throws IOException;
}
