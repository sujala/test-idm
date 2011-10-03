package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;

import com.rackspace.docs.identity.api.ext.rax_ksadm.v1.UserWithOnlyEnabled;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:15 PM
 */
public interface Cloud20Service {

    ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException, JAXBException;

    ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException;

    ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException;

    ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException;

    ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException;

    ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException;

    ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException;

    ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

    ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

    ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException;

    ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException;

    ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) throws IOException;

    ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException;

    ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit) throws IOException;

    ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType, String body) throws IOException;

    ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) throws IOException;

    ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) throws IOException;

    ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantsId, String userId) throws IOException;

	ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User user) throws IOException, JAXBException;

	ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, User user) throws IOException, JAXBException;

	ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) throws IOException;

	ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, UserWithOnlyEnabled user) throws IOException, JAXBException;

	ResponseBuilder listUserRoles(HttpHeaders httpHeaders, String authToken, String userId, String serviceId) throws IOException;

	ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException;

	ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Tenant tenant) throws IOException, JAXBException;

	ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Tenant tenant) throws IOException, JAXBException;

	ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) throws IOException;

	ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String roleId, String marker, Integer limit) throws IOException;

	ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) throws IOException;

	ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) throws IOException;

	ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) throws IOException, JAXBException;

	ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException;

	ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException;

	ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException;

	ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) throws IOException, JAXBException;

	ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException;

	ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException;

    ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, int limit) throws IOException;

    ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String userId) throws IOException;

    ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId);

    ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, EndpointTemplate endpoint);

    ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId);

    ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String enpdointTemplateId);

    ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken,
        String tenantId);

    ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        EndpointTemplate endpoint);

    ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        String endpointId);

    ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId,
        String endpointId);
}
