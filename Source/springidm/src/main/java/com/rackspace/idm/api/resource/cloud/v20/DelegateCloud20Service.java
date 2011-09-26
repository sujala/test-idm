package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
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

    @Autowired private Configuration config;
    @Autowired private DefaultCloud20Service defaultCloud20Service;
    @Autowired private DummyCloud20Service dummyCloud20Service;

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
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud20Service().authenticate(httpHeaders, authenticationRequest);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        if (clonedServiceResponse.build().getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            String body = marshallObjectToString(OBJ_FACTORY.createAuth(authenticationRequest));
            return cloudClient.post(getCloudAuthV20Url() + "tokens", httpHeaders, body);
        }
        return serviceResponse;
    }


	@Override
	public ResponseBuilder validateToken(HttpHeaders httpHeaders,
			String tokenId, String belongsTo) throws IOException {
        String request = getCloudAuthV20Url() + "tokens/" + tokenId;

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("belongsTo", belongsTo);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
			String tokenId) throws IOException {
        String request = getCloudAuthV20Url() + "tokens/" + tokenId + "/endpoints";
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
			throws IOException {
        String request = getCloudAuthV20Url() + "extensions";
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
			throws IOException {
        String request = getCloudAuthV20Url() + "extensions/" + alias;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String name)
			throws IOException {
        String request = getCloudAuthV20Url() + "users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("name", name);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getUserById(HttpHeaders httpHeaders, String userId)
			throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders,
			String userId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/roles";
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker,
			Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name)
			throws IOException {
        String request = getCloudAuthV20Url() + "tenants";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("name", name);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getTenantById(HttpHeaders httpHeaders,
			String tenantsId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantsId;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String userId,
			String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/credentials";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String userId,
			String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/credentials";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders, String userId,
			String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/credentials/RAX-KSKEY:apikeyCredentials";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String userId)
			throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/credentials/RAX-KSKEY:apikeyCredentials";
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String userId)
			throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/credentials/RAX-KSKEY:apikeyCredentials";
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders,
			String tenantId, String userId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users/" + userId + "/roles";
        return cloudClient.get(request, httpHeaders);
	}

	@Override
	public ResponseBuilder listUsers(HttpHeaders httpHeaders) throws IOException {
		String request = getCloudAuthV20Url() + "users";
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addUser(HttpHeaders httpHeaders, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder updateUser(HttpHeaders httpHeaders, String userId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId;
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String userId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String userId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/enabled";
        return cloudClient.put(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
			String userId, String serviceId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/roles";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("serviceId", serviceId);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String userId,
			String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/roles/" + roleId;
        return cloudClient.put(request, httpHeaders, "");
	}


	@Override
	public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String userId,
			String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/roles/" + roleId;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
			String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/roles/" + roleId;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder OS_KSADM_addUserCredential(HttpHeaders httpHeaders,
			String userId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder OS_KSADM_listCredentials(HttpHeaders httpHeaders,
			String userId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);
        
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder OS_KSADM_updateUserCredential(
			HttpHeaders httpHeaders, String userId, String credentialType,
			String body) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder OS_KSADM_getUserCredential(HttpHeaders httpHeaders,
			String userId, String credentialType) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder OS_KSADM_deleteUserCredential(
			HttpHeaders httpHeaders, String userId, String credentialType) throws IOException {
        String request = getCloudAuthV20Url() + "users/" + userId + "/OS-KSADM/credentials/" + credentialType;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addTenant(HttpHeaders httpHeaders, String body) throws IOException {
        String request = getCloudAuthV20Url() + "tenants";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String tenantId, String body) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId;
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String tenantId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
			String tenantId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/OS-KSADM/roles";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
			String tenantId, String roleId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("roleId", roleId);
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
			String tenantId, String marker, Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
			String tenantId, String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
        return cloudClient.put(request, httpHeaders, "");
	}


	@Override
	public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
			String tenantId, String userId, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listRoles(HttpHeaders httpHeaders, String serviceId,
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
	public ResponseBuilder addRole(HttpHeaders httpHeaders, String body) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder getRole(HttpHeaders httpHeaders, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String roleId) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
        return cloudClient.delete(request, httpHeaders);
	}


	@Override
	public ResponseBuilder listServices(HttpHeaders httpHeaders, String marker,
			Integer limit) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("marker", marker);
        params.put("limit", limit);
        request = appendQueryParams(request, params);

        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder addService(HttpHeaders httpHeaders, String body) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services";
        return cloudClient.post(request, httpHeaders, body);
	}


	@Override
	public ResponseBuilder getService(HttpHeaders httpHeaders, String serviceId) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder deleteService(HttpHeaders httpHeaders,
			String serviceId) throws IOException {
        String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
        return cloudClient.delete(request, httpHeaders);
	}

    public String appendQueryParams(String request, HashMap<String, Object> params) {
        String result = "";

        for(String key : params.keySet()) {
            Object value = params.get(key);

            if(value != null) {
                if(result.length() == 0) {
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
