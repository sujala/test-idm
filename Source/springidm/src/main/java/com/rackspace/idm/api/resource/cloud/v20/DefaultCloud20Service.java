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
	public ResponseBuilder addUserCredential(HttpHeaders httpHeaders,
			String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUserCredential");
	}

	@Override
	public ResponseBuilder listCredentials(HttpHeaders httpHeaders,
			String marker, Integer limit) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listCredentials");
	}

	@Override
	public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders,
			String body) throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateUserCredential");
	}

	@Override
	public ResponseBuilder getUserCredential(HttpHeaders httpHeaders)
			throws IOException {
		//TODO write me
		throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserCredential");
	}

	@Override
	public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders)
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
}
