package com.rackspace.test;

import com.rackspace.idm.api.resource.cloud.ObjectMarshaller;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import javax.xml.bind.JAXBException;

public class IntegrationTestHelper {

    private ObjectFactory objectFactory = new ObjectFactory();

    public String authenticationRequestToXml(String username, String password) throws JAXBException {
        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);

        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));

        return marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
    }

    public AuthenticateResponse xmlToAuthenticateResponse(String response) throws JAXBException {
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
    }
}
