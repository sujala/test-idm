package com.rackspace.idm.api.resource.cloud.v20;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class
        DefaultCloud20Service implements Cloud20Service{

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException {
        //TODO write me
        throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.authenticate");
    }

	@Override
	public ResponseBuilder validateToken(HttpHeaders httpHeaders,
			String tokenId, String belongsTo) throws IOException {
        //TODO write me
        throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.validateToken");
	}

	@Override
	public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
			String tokenId) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listEndpointsForToken");
	}

	@Override
	public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listExtensions");
	}

	@Override
	public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getExtension");
	}

	@Override
	public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String name)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserByName");
	}

	@Override
	public ResponseBuilder getUserById(HttpHeaders httpHeaders, String userId)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserById");
	}

	@Override
	public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders,
			String userId) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUserGlobalRoles");
	}

	@Override
	public ResponseBuilder getTenantById(HttpHeaders httpHeaders,
			String tenantsId) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getTenantById");
	}

	@Override
	public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String userId,
			String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUserCredential");
	}

	@Override
	public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String userId,
			String marker, Integer limit) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listCredentials");
	}

	@Override
	public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String userId,
			String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateUserCredential");
	}

	@Override
	public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String userId)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserCredential");
	}

	@Override
	public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String userId)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUserCredential");
	}

	@Override
	public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders,
			String tenantsId, String userId) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listRolesForUserOnTenant");
	}

	@Override
	public ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker,
			Integer limit) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listTenants");
	}

	@Override
	public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getTenantByName");
	}

	@Override
	public ResponseBuilder listUsers(HttpHeaders httpHeaders) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsers");
	}

	@Override
	public ResponseBuilder addUser(HttpHeaders httpHeaders, String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUser");
	}

	@Override
	public ResponseBuilder updateUser(HttpHeaders httpHeaders, String userId,
			String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateUser");
	}

	@Override
	public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String userId)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUser");
	}

	@Override
	public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
			String userId, String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.setUserEnabled");
	}

	@Override
	public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
			String userId, String serviceId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUserRoles");
	}

	@Override
	public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String userId,
			String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUserRole");
	}

	@Override
	public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String userId,
			String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserRole");
	}

	@Override
	public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
			String userId, String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUserRole");
	}

	@Override
	public ResponseBuilder OS_KSADM_addUserCredential(HttpHeaders httpHeaders,
			String userId, String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_addUserCredential");
	}

	@Override
	public ResponseBuilder OS_KSADM_listCredentials(HttpHeaders httpHeaders,
			String userId, String marker, Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_listCredentials");
	}

	@Override
	public ResponseBuilder OS_KSADM_updateUserCredential(
			HttpHeaders httpHeaders, String userId, String credentialType,
			String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_updateUserCredential");
	}

	@Override
	public ResponseBuilder OS_KSADM_getUserCredential(HttpHeaders httpHeaders,
			String userId, String credentialType) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_getUserCredential");
	}

	@Override
	public ResponseBuilder OS_KSADM_deleteUserCredential(
			HttpHeaders httpHeaders, String userId, String credentialType) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_deleteUserCredential");
	}

	@Override
	public ResponseBuilder addTenant(HttpHeaders httpHeaders, String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addTenant");
	}

	@Override
	public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
			String tenantId, String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateTenant");
	}

	@Override
	public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String tenantId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteTenant");
	}

	@Override
	public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
			String tenantId, String marker, Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listRolesForTenant");
	}

	@Override
	public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
			String tenantId, String roleId, String marker, Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsersWithRoleForTenant");
	}

	@Override
	public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
			String tenantId, String marker, Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsersForTenant");
	}

	@Override
	public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
			String tenantId, String userId, String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addRolesToUserOnTenant");
	}

	@Override
	public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
			String tenantId, String userId, String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteRoleFromUserOnTenant");
	}

	@Override
	public ResponseBuilder listRoles(HttpHeaders httpHeaders, String serviceId,
			String marker, Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listRoles");
	}

	@Override
	public ResponseBuilder addRole(HttpHeaders httpHeaders, String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addRole");
	}

	@Override
	public ResponseBuilder getRole(HttpHeaders httpHeaders, String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getRole");
	}

	@Override
	public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String roleId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteRole");
	}

	@Override
	public ResponseBuilder listServices(HttpHeaders httpHeaders, String marker,
			Integer limit) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listServices");
	}

	@Override
	public ResponseBuilder addService(HttpHeaders httpHeaders, String body) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addService");
	}

	@Override
	public ResponseBuilder getService(HttpHeaders httpHeaders, String serviceId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getService");
	}

	@Override
	public ResponseBuilder deleteService(HttpHeaders httpHeaders,
			String serviceId) {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteService");
	}
}
