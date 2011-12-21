package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.io.IOException;

@Component
public class DummyCloud20Service implements Cloud20Service{

    @Override
    public ResponseBuilder authenticate(HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken,
        String tokenId, String belongsTo) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken,
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
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken,
        String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker,
        Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken,
        String tenantsId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String body) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken,
        String userId, String marker, Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String credentialType) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String credentialType) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantsId, String userId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User user)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId,
        User user) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken,
        String userId, User user) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId,
        String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId,
        String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken,
        String userId, String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Tenant tenant)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, Tenant tenant) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String marker, Integer limit) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String roleId, String marker, Integer limit)
        throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String marker, Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String userId, String roleId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String userId, String roleId) throws IOException {
        
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId,
        String marker, Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker,
        Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId)
        throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken,
        String serviceId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, Integer limit) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, EndpointTemplate endpoint) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String enpdointTemplateId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders,
        String authToken, String tenantId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, EndpointTemplate endpoint) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, String endpointId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, String endpointId) {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(
        HttpHeaders httpHeaders, String authToken, String userId,
        String credentialType, PasswordCredentialsRequiredUsername creds)
        throws IOException, JAXBException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType,
        ApiKeyCredentials creds) throws IOException, JAXBException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders,
        String authToken, String userId, SecretQA secrets) throws IOException,
        JAXBException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(
        HttpHeaders httpHeaders, String authToken, String userId,
        String serviceId) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

}
