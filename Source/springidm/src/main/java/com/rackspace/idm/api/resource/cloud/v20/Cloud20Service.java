package com.rackspace.idm.api.resource.cloud.v20;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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

    Response.ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker, int limit) throws IOException;

    Response.ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name) throws IOException;

    Response.ResponseBuilder getTenantById(HttpHeaders httpHeaders, String tenantsId) throws IOException;

    Response.ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder listCredentials(HttpHeaders httpHeaders, String marker, int limit) throws IOException;

    Response.ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder getUserCredential(HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String tenantsId, String userId) throws IOException;
}
