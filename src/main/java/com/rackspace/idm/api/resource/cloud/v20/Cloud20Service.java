package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface Cloud20Service {

    ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest);

    ResponseBuilder authenticateFederated(HttpHeaders httpHeaders, byte[] samlResponse);

    ResponseBuilder upgradeUserToCloud(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User upgradeUser);

    Response.ResponseBuilder authenticateForForgotPassword(HttpHeaders httpHeaders, ForgotPasswordCredentials forgotPasswordCredentials);

    ResponseBuilder logoutFederatedUser(HttpHeaders httpHeaders, byte[] logoutRequest);

    ResponseBuilder addIdentityProvider(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, IdentityProvider provider);

    ResponseBuilder updateIdentityProvider(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String identityProviderId, IdentityProvider provider);

    ResponseBuilder updateIdentityProviderPolicy(HttpHeaders httpHeaders, String authToken, String identityProviderId, String policy);

    ResponseBuilder getIdentityProviderPolicy(HttpHeaders httpHeaders, String authToken, String identityProviderId);

    ResponseBuilder getIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId);

    ResponseBuilder getIdentityProviders(HttpHeaders httpHeaders, String authToken, String name, String issuer, String domainId, String idpType, String approvedTenantId);

    ResponseBuilder deleteIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId);

    ResponseBuilder addIdentityProviderCert(HttpHeaders httpHeaders, String authToken, String identityProviderId, PublicCertificate publicCertificate);

    ResponseBuilder deleteIdentityProviderCert(HttpHeaders httpHeaders, String authToken, String identityProviderId, String certificateId);

    ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo);

    ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken) throws IOException, JAXBException;

    ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken, String userToken) throws IOException, JAXBException;

    ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) ;

    ResponseBuilder listExtensions(HttpHeaders httpHeaders) ;

    ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) ;

    ResponseBuilder getUserByTenantId(HttpHeaders httpHeaders, String authToken, String tenantId) ;

    ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) ;

    ResponseBuilder getUsersByEmail(HttpHeaders httpHeaders, String authToken, String email) ;

    ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId, boolean applyRcnRoles) ;

    ResponseBuilder deleteUserRoles(HttpHeaders httpHeaders, String authToken, String userId, String roleType);

    ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) ;

    ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) ;

    ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) ;

    ResponseBuilder addUserCredential(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String userId, String body) ;

    ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, Integer marker, Integer limit) ;

    ResponseBuilder getUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder getUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) ;

    ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, boolean applyRcnRoles) ;

	ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User user);

	ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user);

	ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId);

	ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, User user);

	ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) ;

	ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Tenant tenant);

	ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Tenant tenant);

	ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) ;

	ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Integer marker, Integer limit) ;

	ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String tenantId, String roleId, Integer marker, Integer limit) ;

	ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String tenantId, Integer marker, Integer limit) ;

	ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) ;

	ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) ;

	ResponseBuilder listRoles(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String serviceId, String roleName, Integer marker, Integer limit) ;

	ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role);

	ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) ;

	ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) ;

	ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String name, Integer marker, Integer limit) ;

	ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service);

	ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) ;

	ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) ;

    ResponseBuilder listUsers(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Integer marker, Integer limit) ;

    ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId) ;

    ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, EndpointTemplate endpoint);

    ResponseBuilder updateEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String endpointTemplateId, EndpointTemplate endpoint);

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
        PasswordCredentialsBase creds);

    ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType,
        ApiKeyCredentials creds);

    ResponseBuilder resetUserApiKeyCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType);

    ResponseBuilder passwordReset(HttpHeaders httpHeaders, String authToken, PasswordReset passwordReset);

    ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken,
        String userId) ;

    ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken,
        String userId, SecretQA secrets);

    ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId, String serviceId,  boolean applyRcnRoles) ;

    ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String group, Integer marker, Integer limit) ;

    ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) ;

    ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId) ;

    ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Group group);

    ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId, Group group);

    ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId) ;

    ResponseBuilder addUserToGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) ;

    ResponseBuilder removeUserFromGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) ;

    ResponseBuilder getUsersForGroup(HttpHeaders httpHeaders, String authToken, String groupId, Integer marker, Integer limit) ;

    ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName) ;

    ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest);

    ResponseBuilder listDefaultRegionServices(String authToken);

    ResponseBuilder setDefaultRegionServices(String authToken, DefaultRegionServices defaultRegionServices);

    ResponseBuilder addDomain(String authToken, UriInfo uriInfo, Domain domain);

    ResponseBuilder getDomain(String authToken, String domainId);

    ResponseBuilder updateDomain(String authToken, String domainId, Domain domain);

    ResponseBuilder deleteDomain(String authToken, String domainId);

    ResponseBuilder updateDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId, String policy);

    ResponseBuilder deleteDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId);

    ResponseBuilder getDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId);

    ResponseBuilder getDomainTenants(String authToken, String domainId, String enabled);

    ResponseBuilder getUsersByDomainIdAndEnabledFlag(String authToken, String domainId, String enabled);

    ResponseBuilder addUserToDomain(String authToken, String domainId, String userId) throws IOException, JAXBException;

    ResponseBuilder getEndpointsByDomainId(String authToken, String domainId);

    ResponseBuilder addTenantToDomain(String authToken, String domainId, String tenantId);

    ResponseBuilder removeTenantFromDomain(String authToken, String domainId, String tenantId);

    ResponseBuilder getAccessibleDomains(UriInfo uriInfo, String authToken, Integer marker, Integer limit);

    ResponseBuilder getAccessibleDomainsForUser(String authToken, String userId);

    ResponseBuilder getAccessibleDomainsEndpointsForUser(String authToken, String userId, String domainId);

    ResponseBuilder listUsersWithRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String roleId, Integer marker, Integer limit);

    ResponseBuilder addRegion(UriInfo uriInfo, String authToken, Region region);

    ResponseBuilder getRegion(String authToken, String name);

    ResponseBuilder getRegions(String authToken);

    ResponseBuilder updateRegion(String authToken, String name, Region region);

    ResponseBuilder deleteRegion(String authToken, String name);

    ResponseBuilder addQuestion(UriInfo uriInfo, String authToken, Question question);

    ResponseBuilder getQuestion(String authToken, String questionId);

    ResponseBuilder getQuestions(String authToken);

    ResponseBuilder updateQuestion(String authToken, String questionId, Question question);

    ResponseBuilder deleteQuestion(String authToken, String questionId);

    ResponseBuilder getSecretQAs(String authToken, String userId);

    ResponseBuilder createSecretQA(String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQA);

    ResponseBuilder getUserAdminsForUser(String authToken, String userId);

    ResponseBuilder addTenantType(UriInfo uriInfo, String authToken, TenantType tenantType);

    ResponseBuilder getTenantType(UriInfo uriInfo, String authToken, String tenantTypeName);

    ResponseBuilder listTenantTypes(UriInfo uriInfo, String authToken, Integer marker, Integer limit);

    ResponseBuilder deleteTenantType(String authToken, String tenantTypeId);

    ResponseBuilder changeUserPassword(HttpHeaders httpHeaders, ChangePasswordCredentials changePasswordCredentials);
}
