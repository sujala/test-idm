package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.stereotype.Component;

@Component
public class DummyCloud20Service implements Cloud20Service{

    @Override
    public ResponseBuilder authenticate(HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders,
        String tokenId, String belongsTo) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
        String tokenId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String name)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String userId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders,
        String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker,
        Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders,
        String tenantsId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders,
        String userId, String marker, Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders,
        String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders,
        String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders,
        String tenantsId, String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, String body)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String userId,
        String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String userId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
        String userId, String serviceId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String userId,
        String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String userId,
        String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
        String userId, String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder OS_KSADM_addUserCredential(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder OS_KSADM_listCredentials(HttpHeaders httpHeaders,
        String userId, String marker, Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder OS_KSADM_updateUserCredential(
        HttpHeaders httpHeaders, String userId, String credentialType,
        String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder OS_KSADM_getUserCredential(HttpHeaders httpHeaders,
        String userId, String credentialType) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder OS_KSADM_deleteUserCredential(
        HttpHeaders httpHeaders, String userId, String credentialType)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, String body)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
        String tenantId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String tenantId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
        String tenantId, String marker, Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
        String tenantId, String roleId, String marker, Integer limit)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
        String tenantId, String marker, Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
        String tenantId, String userId, String roleId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
        String tenantId, String userId, String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String serviceId,
        String marker, Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, String body)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String roleId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String roleId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String marker,
        Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, String body)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String serviceId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders,
        String serviceId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

}
