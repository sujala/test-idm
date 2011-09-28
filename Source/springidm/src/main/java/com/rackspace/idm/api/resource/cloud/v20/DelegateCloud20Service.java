package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.CloudClient;

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

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    private Marshaller marshaller;

    public DelegateCloud20Service() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext
            .newInstance("org.openstack.docs.identity.api.v2");
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .authenticate(httpHeaders, authenticationRequest);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String body = marshallObjectToString(OBJ_FACTORY
                .createAuth(authenticationRequest));
            return cloudClient.post(getCloudAuthV20Url() + "tokens",
                httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken,
        String tokenId, String belongsTo) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .validateToken(httpHeaders, authToken, tokenId, belongsTo);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .checkToken(httpHeaders, authToken, tokenId, belongsTo);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken,
        String tokenId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listEndpointsForToken(httpHeaders, authToken, tokenId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "tokens/" + tokenId
                + "/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listExtensions(httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "extensions";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getExtension(httpHeaders, alias);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "extensions/" + alias;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }
    
    
    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, int limit) throws IOException {
        
        //TODO: Implement routing to DefaultCloud20Service
        
        String request = getCloudAuthV20Url() + "users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserByName(httpHeaders, authToken, name);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "users";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;

    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getUserById(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "users/" + userId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken,
        String userId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listUserGlobalRoles(httpHeaders, authToken, userId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "users/" + userId
                + "/roles";
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker,
        Integer limit) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .listTenants(httpHeaders, authToken, marker, limit);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
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
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name)
        throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getTenantByName(httpHeaders, authToken, name);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "tenants";

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken,
        String tenantsId) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud20Service()
            .getTenantById(httpHeaders, authToken, tenantsId);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
            .clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String request = getCloudAuthV20Url() + "tenants/" + tenantsId;
            return cloudClient.get(request, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/credentials";
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken,
        String userId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/credentials";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String credentialType, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/credentials/" + credentialType;
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String credentialType) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/credentials/" + credentialType;
        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken,
        String userId, String credentialType) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/credentials/" + credentialType;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String userId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/users/" + userId + "/roles";
        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, User user)
        throws IOException {
        String request = getCloudAuthV20Url() + "users";
        String body = marshallObjectToString(OBJ_FACTORY
            .createUser(user));
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId,
        User user) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId;
        String body = marshallObjectToString(OBJ_FACTORY
            .createUser(user));
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId)
        throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken,
        String userId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/OS-KSADM/enabled";
        return cloudClient.put(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder listUserRoles(HttpHeaders httpHeaders, String authToken,
        String userId, String serviceId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/OS-KSADM/roles";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("serviceId", serviceId);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId,
        String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/OS-KSADM/roles/" + roleId;
        return cloudClient.put(request, httpHeaders, "");
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId,
        String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/OS-KSADM/roles/" + roleId;
        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken,
        String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId
            + "/OS-KSADM/roles/" + roleId;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, org.openstack.docs.identity.api.v2.Tenant tenant)
        throws IOException {
        String request = getCloudAuthV20Url() + "tenants";
        String body = marshallObjectToString(OBJ_FACTORY
            .createTenant(tenant));
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, org.openstack.docs.identity.api.v2.Tenant tenant) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId;
        String body = marshallObjectToString(OBJ_FACTORY
            .createTenant(tenant));
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId)
        throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/OS-KSADM/roles";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String roleId, String marker, Integer limit)
        throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("roleId", roleId);
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
        return cloudClient.put(request, httpHeaders, "");
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken,
        String tenantId, String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId
            + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId,
        String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("serviceId", serviceId);
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, String authToken, String body)
        throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles";
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId)
        throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId)
        throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
        return cloudClient.delete(request, httpHeaders);
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker,
        Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, String authToken, String body)
        throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services";
        return cloudClient.post(request, httpHeaders, body);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId)
        throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services/"
            + serviceId;
        return cloudClient.get(request, httpHeaders);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken,
        String serviceId) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services/"
            + serviceId;
        return cloudClient.delete(request, httpHeaders);
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

    private String marshallObjectToString(Object jaxbObject) {

        StringWriter sw = new StringWriter();

        try {
            marshaller.marshal(jaxbObject, sw);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
