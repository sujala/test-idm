package com.rackspace.idm.api.resource.cloud.v20;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:15 PM
 */
public interface Cloud20Service {
    Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException;

    Response.ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException;

    Response.ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException;

    Response.ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException;

    Response.ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException;

    Response.ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException;

    Response.ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

    Response.ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

    Response.ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException;

    Response.ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException;

    Response.ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) throws IOException;

    Response.ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException;

    Response.ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit) throws IOException;

    Response.ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType, String body) throws IOException;

    Response.ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) throws IOException;

    Response.ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) throws IOException;

    Response.ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantsId, String userId) throws IOException;

	ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User user) throws IOException;

	ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, User user) throws IOException;

	ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

	ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException;

	ResponseBuilder listUserRoles(HttpHeaders httpHeaders, String authToken, String userId, String serviceId) throws IOException;

	ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder addTenant(HttpHeaders httpHeaders, String authToken, String body) throws IOException;

	ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String body) throws IOException;

	ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) throws IOException;

	ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String roleId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRole(HttpHeaders httpHeaders, String authToken, String body) throws IOException;

	ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException;

	ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException;

	ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException;

	ResponseBuilder addService(HttpHeaders httpHeaders, String authToken, String body) throws IOException;

	ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException;

	ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException;

    ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, int limit) throws IOException;

    ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String userId) throws IOException;
}
