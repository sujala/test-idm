package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;

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
        String body = marshallObjectToString(OBJ_FACTORY.createAuth(authenticationRequest));
        return cloudClient.post(getCloudAuthV20Url() + "tokens", httpHeaders, body);
    }


	@Override
	public ResponseBuilder validateToken(HttpHeaders httpHeaders,
			String tokenId, String belongsTo) throws IOException {
        String request = getCloudAuthV20Url() + "tokens/" + tokenId;
        if (belongsTo != null) {
            request += "?belongsTo=" + belongsTo;
        }
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
		// TODO Auto-generated method stub
        String request = getCloudAuthV20Url() + "tenants";
        if(marker != null && limit != null) {
            request += "?limit=" + limit + "&marker=" + marker;
        } else if (marker != null ) {
            request += "?marker=" + marker;
        } else if (limit != null) {
            request += "?limit=" + limit;
        }
        return cloudClient.get(request, httpHeaders);
	}


	@Override
	public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name)
			throws IOException {
        String request = getCloudAuthV20Url() + "tenants";
        if(name != null) {
            request += "?name=" + name;
        }
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
        if(marker != null && limit != null) {
            request += "?limit=" + limit + "&marker=" + marker;
        } else if (marker != null ) {
            request += "?marker=" + marker;
        } else if (limit != null) {
            request += "?limit=" + limit;
        }
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

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public void setConfig(Configuration config) {
        this.config = config;
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
}
