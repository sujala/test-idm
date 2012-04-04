package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.CloudUserExtractor;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ApiException;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DelegateCloud20Service implements Cloud20Service {

    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private Configuration config;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    @Autowired
    private DummyCloud20Service dummyCloud20Service;

    @Autowired
    private CloudUserExtractor cloudUserExtractor;

    public static final String CLOUD_AUTH_ROUTING = "useCloudAuth";

    public static final String GA_SOURCE_OF_TRUTH = "gaIsSourceOfTruth";

    private org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = new org.openstack.docs.identity.api.v2.ObjectFactory();

    private org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory objectFactoryOSADMN = new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory();

    private org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory objectFactoryOSCATALOG = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory objectFactoryRAXKSKEY = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory objectFactorySECRETQA = new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory objectFactoryRAXGRP = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest)
            throws IOException, JAXBException {

        //Get "user" from LDAP
        com.rackspace.idm.domain.entity.User user = cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);

        //Get Cloud Auth response
        String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
        Response.ResponseBuilder serviceResponse = cloudClient.post(getCloudAuthV20Url() + "tokens", httpHeaders, body);
        Response dummyResponse = serviceResponse.clone().build();
        //If SUCCESS and "user" is not null, store token to "user" and return cloud response
        int status = dummyResponse.getStatus();
        if (status == HttpServletResponse.SC_OK && user != null) {
            AuthenticateResponse authenticateResponse = (AuthenticateResponse) unmarshallResponse(dummyResponse.getEntity().toString(), AuthenticateResponse.class);
            if (authenticateResponse != null) {
                String token = authenticateResponse.getToken().getId();
                Date expires = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTime();
                scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(user, getCloudAuthClientId(), token, expires);
            }
            return serviceResponse;
        } else if (user == null) { //If "user" is null return cloud response
            return serviceResponse;
        } else { //If we get this far, return Default Service Response
            return getCloud20Service().authenticate(httpHeaders, authenticationRequest);
        }

        /*
        Response.ResponseBuilder serviceResponse = getCloud20Service().authenticate(httpHeaders, authenticationRequest);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
            return cloudClient.post(getCloudAuthV20Url() + "tokens", httpHeaders, body);
        }
        return serviceResponse;
        */
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws Exception, JAXBException {
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenId);
        if (isCloudAuthRoutingEnabled() && scopeAccess == null) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            ScopeAccess impersonatedUserScopeAccess = scopeAccessService.getScopeAccessByAccessToken(impersonatedScopeAccess.getImpersonatingToken());
            if (impersonatedUserScopeAccess == null) {
                return validateImpersonatedTokenFromCloud(httpHeaders, impersonatedScopeAccess.getImpersonatingToken(), belongsTo, impersonatedScopeAccess);
            } else {
                return defaultCloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo);
            }
        }
        return defaultCloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo);
    }

    private ResponseBuilder validateImpersonatedTokenFromCloud(HttpHeaders httpHeaders, String impersonatedCloudToken, String belongsTo, ImpersonatedScopeAccess impersonatedScopeAccess) throws Exception, JAXBException {
        String gaXAuthToken = getXAuthToken(config.getString("ga.userName"), config.getString("ga.apiKey")).getToken().getId();
        httpHeaders.getRequestHeaders().get("x-auth-token").set(0, gaXAuthToken);
        httpHeaders.getRequestHeaders().get("accept").set(0, "application/xml");
        Response cloudValidateResponse = checkToken(httpHeaders, gaXAuthToken, impersonatedCloudToken, belongsTo).build();
        AuthenticateResponse validateResponse = (AuthenticateResponse) unmarshallResponse(cloudValidateResponse.getEntity().toString(), AuthenticateResponse.class);
        ImpersonationResponse impersonationResponse = new ImpersonationResponse();
        impersonationResponse.setUser(validateResponse.getUser());
        impersonationResponse.setToken(validateResponse.getToken());
        com.rackspace.idm.domain.entity.User impersonator = userService.getUserByScopeAccess(impersonatedScopeAccess);
        List<TenantRole> impRoles = tenantService.getGlobalRolesForUser(impersonator, null);
        impersonationResponse.setImpersonator(this.userConverterCloudV20.toUserForAuthenticateResponse(impersonator, impRoles));
        return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtRaxgaV1Factory().createAccess(impersonationResponse));
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws IOException {

        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.checkToken(httpHeaders, authToken, tokenId, belongsTo);
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId + "/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listEndpointsForToken(httpHeaders, authToken, tokenId);
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "extensions";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listExtensions(httpHeaders);
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException {

        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "extensions/" + alias;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getExtension(httpHeaders, alias);
    }

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            // TODO: Implement routing to DefaultCloud20Service
            String request = getCloudAuthV20Url() + "users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsers(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/RAX-KSGRP";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGroups(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getGroupById(httpHeaders, authToken, groupId);
    }

    @Override
    public ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Group group) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            String body = marshallObjectToString(objectFactoryRAXGRP.createGroup(group));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addGroup(httpHeaders, uriInfo, authToken, group);
    }

    @Override
    public ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId, Group group) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId;
            String body = marshallObjectToString(objectFactoryRAXGRP.createGroup(group));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateGroup(httpHeaders, authToken, groupId, group);
    }

    @Override
    public ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteGroup(httpHeaders, authToken, groupId);
    }

    @Override
    public ResponseBuilder addGroupToUser(HttpHeaders httpHeaders, String authToken, String groupId, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId + "/users/" + userId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addGroupToUser(httpHeaders, authToken, groupId, userId);
    }

    @Override
    public ResponseBuilder removeGroupFromUser(HttpHeaders httpHeaders, String authToken, String groupId, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId + "/users/" + userId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.removeGroupFromUser(httpHeaders, authToken, groupId, userId);
    }

    @Override
    public ResponseBuilder listUsersWithGroup(HttpHeaders httpHeaders, String authToken, String groupId, String marker, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            // TODO: Implement routing to DefaultCloud20Service
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups/" + groupId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsersWithGroup(httpHeaders, authToken, groupId, marker, limit);
    }

    @Override
    public ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", groupName);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listGroups(httpHeaders, authToken, groupName, null, null);
    }

    @Override
    public ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest) throws IOException {
        return null;
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {
        if (isCloudAuthRoutingEnabled() && !userService.userExistsByUsername(name)) {
            String request = getCloudAuthV20Url() + "users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserByName(httpHeaders, authToken, name);
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserById(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/roles";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGlobalRoles(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId,
                                                          String serviceId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/roles";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, serviceId);
    }

    @Override
    public ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String marker, String groupName, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", groupName);
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listGroups(httpHeaders, authToken, groupName, marker, limit);
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listTenants(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getTenantByName(httpHeaders, authToken, name);
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) throws IOException {
        com.rackspace.idm.domain.entity.Tenant tenant = tenantService.getTenant(tenantsId);
        if (isCloudAuthRoutingEnabled() && tenant==null) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantsId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getTenantById(httpHeaders, authToken, tenantsId);
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String xmlBody = body;
            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                xmlBody = convertCredentialToXML(body);
            }
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials";
            return cloudClient.post(request, httpHeaders, xmlBody);
        }
        return defaultCloud20Service.addUserCredential(httpHeaders, authToken, userId, body);
    }

    private String convertCredentialToXML(String body) {
        JAXBElement<? extends CredentialType> jaxbCreds = null;
        String xml = null;

        CredentialType creds = JSONReaderForCredentialType
                .checkAndGetCredentialsFromJSONString(body);

        if (creds instanceof PasswordCredentialsRequiredUsername) {
            PasswordCredentialsRequiredUsername userCreds = (PasswordCredentialsRequiredUsername) creds;
            jaxbCreds = objectFactory.createPasswordCredentials(userCreds);
        } else if (creds instanceof ApiKeyCredentials) {
            ApiKeyCredentials userCreds = (ApiKeyCredentials) creds;
            jaxbCreds = objectFactoryRAXKSKEY
                    .createApiKeyCredentials(userCreds);
        }

        try {
            xml = marshallObjectToString(jaxbCreds);
        } catch (JAXBException e) {
            throw new IllegalStateException("error marshalling creds");
        }

        return xml;
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listCredentials(httpHeaders, authToken, userId, marker, limit);
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsRequiredUsername creds)
            throws JAXBException, IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
            String body = marshallObjectToString(objectFactory.createPasswordCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUserPasswordCredentials(httpHeaders, authToken, userId, credentialType, creds);
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId, String credentialType,
                                                       ApiKeyCredentials creds) throws JAXBException, IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
            String body = marshallObjectToString(objectFactoryRAXKSKEY.createApiKeyCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUserApiKeyCredentials(httpHeaders, authToken, userId, credentialType, creds);
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserCredential(httpHeaders, authToken, userId, credentialType);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteUserCredential(httpHeaders, authToken, userId, credentialType);
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users/" + userId + "/roles";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId);
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, UserForCreate user)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "users";
            String body = marshallObjectToString(objectFactory.createUser(user));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addUser(httpHeaders, uriInfo, authToken, user);
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId;
            String body = marshallObjectToString(objectFactory.createUser(user));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUser(httpHeaders, authToken, userId, user);
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteUser(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) throws IOException, NotFoundException {
        Response.ResponseBuilder serviceResponse = getCloud20Service().deleteUserFromSoftDeleted(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        if (config.getBoolean("allowSoftDeleteDeletion")) {
            return defaultCloud20Service.deleteUserFromSoftDeleted(httpHeaders, authToken, userId);
        } else {
            throw new NotFoundException("Not found");
        }
//        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
//        int status = clonedServiceResponse.build().getStatus();
//        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
//            throw new NotFoundException("Not Found");
//        }
//        return serviceResponse;
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, User user)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/enabled";
            String body = marshallObjectToString(objectFactory.createUser(user));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.setUserEnabled(httpHeaders, authToken, userId, user);
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, org.openstack.docs.identity.api.v2.Tenant tenant)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants";
            String body = marshallObjectToString(objectFactory.createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addTenant(httpHeaders, uriInfo, authToken, tenant);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, org.openstack.docs.identity.api.v2.Tenant tenant)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId;
            String body = marshallObjectToString(objectFactory
                    .createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateTenant(httpHeaders, authToken, tenantId, tenant);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteTenant(httpHeaders, authToken, tenantId);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker,
                                              Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/OS-KSADM/roles";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRolesForTenant(httpHeaders, authToken, tenantId, marker, limit);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String roleId, String marker, Integer limit)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("roleId", roleId);
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsersWithRoleForTenant(httpHeaders, authToken, tenantId, roleId, marker, limit);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsersForTenant(httpHeaders, authToken, tenantId, marker, limit);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addRolesToUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId);
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
                                                      String authToken, String tenantId, String userId, String roleId)
            throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
                .deleteRoleFromUserOnTenant(httpHeaders, authToken, tenantId,
                        userId, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
                .clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                    + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRoles(httpHeaders, authToken, serviceId, marker, limit);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles";
            String body = marshallObjectToString(objectFactory.createRole(role));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addRole(httpHeaders, uriInfo, authToken, role);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getRole(httpHeaders, authToken, roleId);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteRole(httpHeaders, authToken, roleId);
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listServices(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services";
            String body = marshallObjectToString(objectFactoryOSADMN.createService(service));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addService(httpHeaders, uriInfo, authToken, service);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getService(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteService(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders, String authToken, String serviceId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listEndpointTemplates(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, EndpointTemplate endpoint)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates";
            String body = marshallObjectToString(objectFactoryOSCATALOG.createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint);
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken, String tenantId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listEndpoints(httpHeaders, authToken, tenantId);
    }

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints";
            String body = marshallObjectToString(objectFactoryOSCATALOG.createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addEndpoint(httpHeaders, authToken, tenantId, endpoint);
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String endpointId, String tenantId)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                    + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getEndpoint(httpHeaders, authToken, endpointId, tenantId);
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String endpointId, String tenantId)
            throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteEndpoint(httpHeaders, authToken, endpointId, tenantId);
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/RAX-KSQA/secretqa";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getSecretQA(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken, String userId, SecretQA secrets)
            throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/RAX-KSQA/secretqa";
            String body = marshallObjectToString(objectFactorySECRETQA.createSecretQA(secrets));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateSecretQA(httpHeaders, authToken, userId, secrets);
    }

    public String appendQueryParams(String request, HashMap<String, Object> params) {
        String result = "";
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value != null) {
                if (result.length() == 0) {
                    result += "?";
                } else {
                    result += "&";
                }
                String encodeValue;
                try {
                    encodeValue = URLEncoder.encode(value.toString(), JSONConstants.UTF_8);
                } catch (Exception e) {
                    throw new BadRequestException("Unable to encode query params.");
                }
                result += key + "=" + encodeValue;
            }
        }
        return request + result;
    }

    //TODO change way we check for media type

//    private AuthenticateResponse getAuthFromResponse(String entity) {

    private Object unmarshallResponse(String entity, Class<?> objectClass) {
        try {
            if (entity.trim().startsWith("{")) {
                //TODO: HANDLE JAXBElement for user
                JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();
                JSONJAXBContext context = new JSONJAXBContext(jsonConfiguration, "org.openstack.docs.identity.api.v2");
                JSONUnmarshaller jsonUnmarshaller = context.createJSONUnmarshaller();
                StreamSource xml = new StreamSource(new StringReader(entity));
                JAXBElement ob = jsonUnmarshaller.unmarshalJAXBElementFromJSON(new StringReader(entity), objectClass);
                return ob.getValue();
            } else {
                JAXBContext jc = JAXBContext.newInstance(objectClass);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                StreamSource xml = new StreamSource(new StringReader(entity));
                JAXBElement ob = unmarshaller.unmarshal(xml, objectClass);
                return ob.getValue();

            }
        } catch (Exception ex) {
            return null;
        }
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setDummyCloud20Service(DummyCloud20Service dummyCloud20Service) {
        this.dummyCloud20Service = dummyCloud20Service;
    }

    private String getCloudAuthV20Url() {
        return config.getString("cloudAuth20url");
    }

    boolean isUserInGAbyId(String userId) {
        return userService.userExistsById(userId);
    }

    private String marshallObjectToString(Object jaxbObject) throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = JAXBContextResolver.get().createMarshaller();
        try {
            marshaller.marshal(jaxbObject, sw);
        } catch (Exception e) {
            JAXBException k = new JAXBException(e.getMessage());
            throw k;
        }
        return sw.toString();
    }

    private boolean isGASourceOfTruth() {
        return config.getBoolean(GA_SOURCE_OF_TRUTH);
    }

    private boolean isCloudAuthRoutingEnabled() {
        return config.getBoolean(CLOUD_AUTH_ROUTING);
    }

    private Cloud20Service getCloud20Service() {
        if (config.getBoolean("GAKeystoneDisabled")) {
            return dummyCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }

    public void setCloudUserExtractor(CloudUserExtractor cloudUserExtractor) {
        this.cloudUserExtractor = cloudUserExtractor;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setDefaultCloud20Service(DefaultCloud20Service defaultCloud20Service) {
        this.defaultCloud20Service = defaultCloud20Service;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public AuthenticateResponse getXAuthToken(String userName, String apiKey) throws JAXBException, IOException {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername(userName);
        apiKeyCredentials.setApiKey(apiKey);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setCredential(objectFactoryRAXKSKEY.createApiKeyCredentials(apiKeyCredentials));
        String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/xml");
        headers.put("Accept", "application/xml");
        Response authResponse = cloudClient.post(getCloudAuthV20Url() + "tokens", headers, body).build();
        if (authResponse.getStatus() != 200 && authResponse.getStatus() != 203) {
            throw new ApiException(authResponse.getStatus(), "", "");
        }
        return (AuthenticateResponse) unmarshallResponse(authResponse.getEntity().toString(), AuthenticateResponse.class);
    }

    public ApiKeyCredentials getUserApiCredentials(String userId, String xAuthToken) throws IOException {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/xml");
        headers.put("X-Auth-Token", xAuthToken);
        headers.put("Accept", "application/xml");
        Response credsResponse = cloudClient.get(getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials", headers).build();
        if (credsResponse.getStatus() != 200 && credsResponse.getStatus() != 203) {
            throw new ApiException(credsResponse.getStatus(), "", "");
        }
        return (ApiKeyCredentials) unmarshallResponse(credsResponse.getEntity().toString(), ApiKeyCredentials.class);
    }

    public User getCloudUserByName(String userName, String xAuthToken) throws IOException {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/xml");
        headers.put("X-Auth-Token", xAuthToken);
        headers.put("Accept", "application/xml");
        Response userResponse = cloudClient.get(getCloudAuthV20Url() + "users" + "?name=" + userName, headers).build();
        if (userResponse.getStatus() != 200 && userResponse.getStatus() != 203) {
            throw new ApiException(userResponse.getStatus(), "", "");
        }
        return (User) unmarshallResponse(userResponse.getEntity().toString(), User.class);
    }

    public RoleList getGlobalRolesForCloudUser(String userId, String xAuthToken) throws IOException {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/xml");
        headers.put("X-Auth-Token", xAuthToken);
        headers.put("Accept", "application/xml");
        Response userResponse = cloudClient.get(getCloudAuthV20Url() + "users/" + userId + "/roles", headers).build();
        if (userResponse.getStatus() != 200 && userResponse.getStatus() != 203) {
            throw new ApiException(userResponse.getStatus(), "", "");
        }
        return (RoleList) unmarshallResponse(userResponse.getEntity().toString(), RoleList.class);
    }


    public String impersonateUser(String userName, String impersonatorName, String impersonatorKey) throws JAXBException, IOException {
        String impersonatorXAuthToken = getXAuthToken(impersonatorName, impersonatorKey)
                .getToken()
                .getId();
        User user = getCloudUserByName(userName, impersonatorXAuthToken);
        RoleList globalRolesForCloudUser = getGlobalRolesForCloudUser(user.getId(), impersonatorXAuthToken);
        if (!isValidCloudImpersonatee(globalRolesForCloudUser)){
            throw new BadRequestException("User cannot be impersontated; No valid impersonation roles");
        }
        String userApiKey = getUserApiCredentials(user.getId(), impersonatorXAuthToken).getApiKey();
        String userXAuthToken = getXAuthToken(userName, userApiKey)
                .getToken()
                .getId();
        return userXAuthToken;
    }

    public boolean isValidCloudImpersonatee(RoleList userRoles) {
        for (Role role : userRoles.getRole()) {
            String name = role.getName();
            if (name.equals("identity:default") || name.equals("identity:user-admin")) {
                return true;
            }
        }
        return false;
    }
    
    public TenantService getTenantService() {
        return tenantService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
