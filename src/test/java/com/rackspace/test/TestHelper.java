package com.rackspace.test;

import com.rackspace.idm.api.resource.cloud.ObjectMarshaller;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBException;

public class TestHelper {


    private ObjectFactory objectFactory = new ObjectFactory();
    private org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory catalogObjectFactory = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();
    private org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = new org.openstack.docs.identity.api.v2.ObjectFactory();

    public String getAuthenticationRequest(String username, String password) throws JAXBException {
        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);

        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));

        return marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
    }

    public AuthenticateResponse getAuthenticateResponse(String response) throws JAXBException {
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
    }

    public String getEndpointTemplateString(String endpointTemplateId) throws JAXBException {
        ObjectMarshaller<EndpointTemplate> marshaller = new ObjectMarshaller<EndpointTemplate>();

        EndpointTemplate endpoint = new EndpointTemplate();
        endpoint.setId(Integer.valueOf(endpointTemplateId));
        endpoint.setType("type");
        endpoint.setPublicURL("http://public.url");

        return marshaller.marshal(catalogObjectFactory.createEndpointTemplate(endpoint), EndpointTemplate.class);
    }

    public String createServiceAdmin(String token, String username, String password, String email) throws JAXBException {
        ObjectMarshaller<UserForCreate> marshaller = new ObjectMarshaller<UserForCreate>();

        UserForCreate user = new UserForCreate();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        return marshaller.marshal(openStackIdentityV2Factory.createUser(user), User.class);
    }

    public EndpointTemplate getEndpointTemplateObject(String endpointTemplate) throws JAXBException {
        ObjectMarshaller<EndpointTemplate> unmarshaller = new ObjectMarshaller<EndpointTemplate>();
        return unmarshaller.unmarshal(endpointTemplate, EndpointTemplate.class);
    }
}
