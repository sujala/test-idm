package com.rackspace.test;

import com.rackspace.idm.api.resource.cloud.ObjectMarshaller;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBException;

public class TestHelper {

    private ObjectFactory objectFactory = new ObjectFactory();
    private org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory catalogObjectFactory = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();

    public enum Type {
        Json,
        Xml
    }

    public String getAuthenticationRequest(String username, String password, Type type) throws JAXBException {
        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);

        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));

        return marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
    }

    public AuthenticateResponse getAuthenticateResponse(String response, Type type) throws JAXBException {
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
    }

    public String getEndpointTemplate(int id) throws JAXBException {
        ObjectMarshaller<EndpointTemplate> marshaller = new ObjectMarshaller<EndpointTemplate>();

        EndpointTemplate endpoint = new EndpointTemplate();
        endpoint.setId(id);
        endpoint.setType("type");
        endpoint.setPublicURL("http://public.url");

        return marshaller.marshal(catalogObjectFactory.createEndpointTemplate(endpoint), EndpointTemplate.class);
    }
}
