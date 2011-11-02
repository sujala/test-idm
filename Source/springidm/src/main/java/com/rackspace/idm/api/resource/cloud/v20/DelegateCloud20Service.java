package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.domain.config.JAXBContextResolver;

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
    private DefaultCloud20Service defaultCloud20Service;
    @Autowired
    private DummyCloud20Service dummyCloud20Service;

    public static void setOBJ_FACTORY(ObjectFactory OBJ_FACTORY) {
        DelegateCloud20Service.OBJ_FACTORY = OBJ_FACTORY;
    }

    private static org.openstack.docs.identity.api.v2.ObjectFactory OBJ_FACTORY = new org.openstack.docs.identity.api.v2.ObjectFactory();
    private static org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory OBJ_FACTORY_OS_ADMIN_EXT = new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory();
    private static org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory OBJ_FACTORY_OS_CATALOG = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();
    private static com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory OBJ_FACTORY_RAX_KSKEY = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();
    private static com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory OBJ_FACOTRY_SECRETQA = new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory();

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException,
        JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .authenticate(httpHeaders, authenticationRequest);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String body = marshallObjectToString(OBJ_FACTORY
                .createAuth(authenticationRequest));
            return cloudClient.post(getCloudAuthV20Url() + "tokens",
                httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders,
        String authToken, String tokenId, String belongsTo) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .validateToken(httpHeaders, authToken, tokenId, belongsTo);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders,
        String authToken, String tokenId, String belongsTo) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .checkToken(httpHeaders, authToken, tokenId, belongsTo);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
        String authToken, String tokenId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listEndpointsForToken(httpHeaders, authToken, tokenId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId
                + "/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException {
        if(config.getBoolean("useCloudAuth")){
             String request = getCloudAuthV20Url() + "extensions";
            return cloudClient.get(request, httpHeaders);
        }
        Response.ResponseBuilder serviceResponse = getCloud20Service().listExtensions(httpHeaders);
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service().getExtension(httpHeaders, alias);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND ||
                clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "extensions/" + alias;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, int limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUsers(httpHeaders, authToken, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            // TODO: Implement routing to DefaultCloud20Service

            String request = getCloudAuthV20Url() + "users";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders,String authToken, String userId)
        throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service().listUserGroups(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId + "/RAX-KSGRP";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders,
        String authToken, String name) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserByName(httpHeaders, authToken, name);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserById(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUserGlobalRoles(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(
        HttpHeaders httpHeaders, String authToken, String userId,
        String serviceId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUserGlobalRoles(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders,
        String authToken, String marker, Integer limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listTenants(httpHeaders, authToken, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders,
        String authToken, String name) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getTenantByName(httpHeaders, authToken, name);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders,
        String authToken, String tenantsId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getTenantById(httpHeaders, authToken, tenantsId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantsId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String body) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addUserCredential(httpHeaders, authToken, userId, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            if (httpHeaders.getMediaType().isCompatible(
                MediaType.APPLICATION_JSON_TYPE)) {
                body = convertCredentialToXML(body);
            }

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials";
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    private String convertCredentialToXML(String body) {
        JAXBElement<? extends CredentialType> jaxbCreds = null;
        String xml = null;

        CredentialType creds = JSONReaderForCredentialType
            .checkAndGetCredentialsFromJSONString(body);

        if (creds instanceof PasswordCredentialsRequiredUsername) {
            PasswordCredentialsRequiredUsername userCreds = (PasswordCredentialsRequiredUsername) creds;
            jaxbCreds = OBJ_FACTORY.createPasswordCredentials(userCreds);
        } else if (creds instanceof ApiKeyCredentials) {
            ApiKeyCredentials userCreds = (ApiKeyCredentials) creds;
            jaxbCreds = OBJ_FACTORY_RAX_KSKEY
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
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String marker, Integer limit)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listCredentials(httpHeaders, authToken, userId, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(
        HttpHeaders httpHeaders, String authToken, String userId,
        String credentialType, PasswordCredentialsRequiredUsername creds)
        throws JAXBException, IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .updateUserPasswordCredentials(httpHeaders, authToken, userId,
                credentialType, creds);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials/" + credentialType;
            String body = marshallObjectToString(OBJ_FACTORY
                .createPasswordCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType,
        ApiKeyCredentials creds) throws JAXBException, IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .updateUserApiKeyCredentials(httpHeaders, authToken, userId,
                credentialType, creds);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials/" + credentialType;
            String body = marshallObjectToString(OBJ_FACTORY_RAX_KSKEY
                .createApiKeyCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserCredential(httpHeaders, authToken, userId, credentialType);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials/" + credentialType;

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType)
        throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteUserCredential(httpHeaders, authToken, userId,
                credentialType);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/credentials/" + credentialType;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String userId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/users/" + userId + "/roles";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, User user) throws IOException, JAXBException {
        Response.ResponseBuilder serviceResponse = getCloud20Service().addUser(
            httpHeaders, uriInfo, authToken, user);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users";
            String body = marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders,
        String authToken, String userId, User user) throws IOException,
        JAXBException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .updateUser(httpHeaders, authToken, userId, user);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId;
            String body = marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteUser(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
        String authToken, String userId, User user) throws IOException,
        JAXBException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .setUserEnabled(httpHeaders, authToken, userId, user);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/enabled";
            String body = marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.put(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
        String authToken, String userId, String serviceId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUserRoles(httpHeaders, authToken, userId, serviceId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/OS-KSADM/roles";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addUserRole(httpHeaders, authToken, userId, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles/OS-KSADM/" + roleId;
            return cloudClient.put(request, httpHeaders, "");
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserRole(httpHeaders, authToken, userId, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles/OS-KSADM/" + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteUserRole(httpHeaders, authToken, userId, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles/OS-KSADM" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, org.openstack.docs.identity.api.v2.Tenant tenant)
        throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addTenant(httpHeaders, uriInfo, authToken, tenant);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants";
            String body = marshallObjectToString(OBJ_FACTORY
                .createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId,
        org.openstack.docs.identity.api.v2.Tenant tenant) throws IOException,
        JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .updateTenant(httpHeaders, authToken, tenantId, tenant);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId;
            String body = marshallObjectToString(OBJ_FACTORY
                .createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteTenant(httpHeaders, authToken, tenantId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String marker, Integer limit)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listRolesForTenant(httpHeaders, authToken, tenantId, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/OS-KSADM/roles/";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String roleId, String marker,
        Integer limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUsersWithRoleForTenant(httpHeaders, authToken, tenantId,
                roleId, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/users";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("roleId", roleId);
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String marker, Integer limit)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUsersForTenant(httpHeaders, authToken, tenantId, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/users";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String userId, String roleId)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addRolesToUserOnTenant(httpHeaders, authToken, tenantId, userId,
                roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.put(request, httpHeaders, "");
        }
        return serviceResponse;
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
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken,
        String serviceId, String marker, Integer limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listRoles(httpHeaders, authToken, serviceId, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/roles";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, Role role) throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service().addRole(
            httpHeaders, uriInfo, authToken, role);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/roles";
            String body = marshallObjectToString(OBJ_FACTORY.createRole(role));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken,
        String roleId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service().getRole(
            httpHeaders, authToken, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders,
        String authToken, String roleId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteRole(httpHeaders, authToken, roleId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders,
        String authToken, String marker, Integer limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listServices(httpHeaders, authToken, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/services";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("marker", marker);
            params.put("limit", limit);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, Service service) throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addService(httpHeaders, uriInfo, authToken, service);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/services";
            String body = marshallObjectToString(OBJ_FACTORY_OS_ADMIN_EXT
                .createService(service));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders,
        String authToken, String serviceId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getService(httpHeaders, authToken, serviceId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

            String request = getCloudAuthV20Url() + "OS-KSADM/services/"
                + serviceId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders,
        String authToken, String serviceId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteService(httpHeaders, authToken, serviceId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services/"
                + serviceId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listEndpointTemplates(httpHeaders, authToken, serviceId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url()
                + "OS-KSCATALOG/endpointTemplates";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders,
        UriInfo uriInfo, String authToken, EndpointTemplate endpoint)
        throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url()
                + "OS-KSCATALOG/endpointTemplates";
            String body = marshallObjectToString(OBJ_FACTORY_OS_CATALOG
                .createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url()
                + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url()
                + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders,
        String authToken, String tenantId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listEndpoints(httpHeaders, authToken, tenantId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/OS-KSCATALOG/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, EndpointTemplate endpoint)
        throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .addEndpoint(httpHeaders, authToken, tenantId, endpoint);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/OS-KSCATALOG/endpoints";
            String body = marshallObjectToString(OBJ_FACTORY_OS_CATALOG
                .createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders,
        String authToken, String endpointId, String tenantId)
        throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getEndpoint(httpHeaders, authToken, endpointId, tenantId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders,
        String authToken, String endpointId, String tenantId)
        throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .deleteEndpoint(httpHeaders, authToken, endpointId, tenantId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantId
                + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.delete(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getSecretQA(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/RAX-KSQA/secretqa/";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders,
        String authToken, String userId, SecretQA secrets) throws IOException,
        JAXBException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .updateSecretQA(httpHeaders, authToken, userId, secrets);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND || clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/RAX-KSQA/secretqa/";
            String body = marshallObjectToString(OBJ_FACOTRY_SECRETQA
                .createSecretQA(secrets));
            return cloudClient.post(request, httpHeaders, body);
        }
        return serviceResponse;
    }

    public String appendQueryParams(String request,
        HashMap<String, Object> params) {
        String result = "";

        for (String key : params.keySet()) {
            Object value = params.get(key);

            if (value != null) {
                if (result.length() == 0) {
                    result += "?";
                } else {
                    result += "&";
                }

                result += key + "=" + value.toString();
            }
        }

        return request + result;
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
        String cloudAuth20url = config.getString("cloudAuth20url");
        return cloudAuth20url;
    }

    private String marshallObjectToString(Object jaxbObject)
        throws JAXBException {

        StringWriter sw = new StringWriter();
        
        Marshaller marshaller = JAXBContextResolver.get().createMarshaller();

        marshaller.marshal(jaxbObject, sw);

        return sw.toString();

    }

    private Cloud20Service getCloud20Service() {
        if (config.getBoolean("GAKeystoneDisabled")) {
            return dummyCloud20Service;
        } else {
            return defaultCloud20Service;
        }
    }

}
