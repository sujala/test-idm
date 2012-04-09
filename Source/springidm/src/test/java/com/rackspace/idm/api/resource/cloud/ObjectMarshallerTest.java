package com.rackspace.idm.api.resource.cloud;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.TokenForAuthenticationRequest;

import com.rackspace.api.idm.v1.AuthData;
import com.rackspace.api.idm.v1.Token;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ObjectMarshallerTest {

    @Test
    public void marshall_AuthenticationRequest() throws JAXBException {
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = new org.openstack.docs.identity.api.v2.ObjectFactory();

        AuthenticationRequest request = new AuthenticationRequest();
        TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
        token.setId("XXXX");
        request.setToken(token);

        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();
        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);

        AuthenticationRequest unmarshalledRequest = marshaller.unmarshal(requestString, AuthenticationRequest.class);

        assertThat("AuthenticationRequest", request.getToken().getId(), equalTo(unmarshalledRequest.getToken().getId()));
    }

    @Test
    public void marshall_AuthData() throws JAXBException {
        com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();

        AuthData request = new AuthData();
        Token token = new Token();
        token.setId("XXXX");
        request.setAccessToken(token);

        ObjectMarshaller<AuthData> marshaller = new ObjectMarshaller<AuthData>();
        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthData.class);

        AuthData unmarshalledRequest = marshaller.unmarshal(requestString, AuthData.class);

        assertThat("AuthData", request.getAccessToken().getId(), equalTo(unmarshalledRequest.getAccessToken().getId()));
    }
}
