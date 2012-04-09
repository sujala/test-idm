package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.Tenants;
import org.openstack.docs.identity.api.v2.User;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;

public class MigrationClient {

	private final String X_AUTH_TOKEN = "X-AUTH-TOKEN";
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	HttpClientWrapper client;
	
	private String host = "";
	
	public MigrationClient() {
		client = new HttpClientWrapper();
	}
	
	public AuthenticateResponse authenticate(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
		
		ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();
		
        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);
        
        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));
        
        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
		
        String response = client.url(host + "tokens")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .post(requestString);
        
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
	}
	
    public Tenants getTenants(String token) throws URISyntaxException, HttpException, IOException, JAXBException {
    	
        String response = client.url(host + "tenants")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Tenants> unmarshaller = new ObjectMarshaller<Tenants>();
        return unmarshaller.unmarshal(response, Tenants.class);
    }

    public User getUser(String token, String username) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(host + "users?name=" + username)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
        return unmarshaller.unmarshal(response, User.class);
    }

    public Groups getGroups(String token) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(host + "RAX-GRPADM/groups")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public Groups getGroupsForUser(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(host + "users/" + userId + "/RAX-KSGRP")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public CredentialListType getUserCredentials(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(host + "users/" + userId + "/OS-KSADM/credentials")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<CredentialListType> unmarshaller = new ObjectMarshaller<CredentialListType>();
        return unmarshaller.unmarshal(response, CredentialListType.class);
    }

	void setHost(String host) {
		this.host = host;
	}
}
