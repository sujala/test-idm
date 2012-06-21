package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.domain.entity.EndPoints;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.ws.commons.util.Base64;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;

public class MigrationClient {

	private final String X_AUTH_TOKEN = "X-AUTH-TOKEN";
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	HttpClientWrapper client;
	
    private String cloud11Host = "";
	private String cloud20Host = "";
	
	public MigrationClient() {
		client = new HttpClientWrapper();
	}
	
	public AuthenticateResponse authenticateWithPassword(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
		
		ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();
		
        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);
        
        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));
        
        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
		
        String response = client.url(cloud20Host + "tokens")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .post(requestString);
        
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
	}

    public AuthenticateResponse authenticateWithApiKey(String username, String apiKey) throws URISyntaxException, HttpException, IOException, JAXBException {

		ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername(username);
        apiKeyCredentials.setApiKey(apiKey);

        request.setCredential(objectFactory.createCredential(apiKeyCredentials));

        String requestString = marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);

        String response = client.url(cloud20Host + "tokens")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .post(requestString);

        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
	}
	
    public Tenants getTenants(String token) throws URISyntaxException, HttpException, IOException, JAXBException {
    	
        String response = client.url(cloud20Host + "tenants")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Tenants> unmarshaller = new ObjectMarshaller<Tenants>();
        return unmarshaller.unmarshal(response, Tenants.class);
    }

    public EndpointList getEndpointsByToken(String adminToken, String token) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response = client.url(cloud20Host + "tokens/" + token + "/endpoints")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, adminToken)
            .get();

        ObjectMarshaller<EndpointList> unmarshaller = new ObjectMarshaller<EndpointList>();
        return unmarshaller.unmarshal(response, EndPoints.class);
    }

    public User getUser(String token, String username) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud20Host + "users?name=" + username)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
        return unmarshaller.unmarshal(response, User.class);
    }

    public RoleList getRolesForUser(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud20Host + "users/" + userId + "/roles")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

    	ObjectMarshaller<RoleList> unmarshaller = new ObjectMarshaller<RoleList>();
        return unmarshaller.unmarshal(response, RoleList.class);
    }
    
    public UserList getUsers(String token) throws URISyntaxException, HttpException, IOException, JAXBException {
        UserList userList = new UserList();
    	String response = client.url(cloud20Host + "users")
    	    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
    	    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
    	    .header(X_AUTH_TOKEN, token)
    	    .get();

    	try {
            ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
            User user = unmarshaller.unmarshal(response, User.class);
            userList.getUser().add(user);
        }catch(Exception ex1){
            try {
                ObjectMarshaller<UserList> unmarshaller = new ObjectMarshaller<UserList>();
                userList = unmarshaller.unmarshal(response, UserList.class);
            }catch (Exception ex2){

            }
        }

    	return userList;
    }

    public SecretQA getSecretQA(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response = client.url(cloud20Host + "users/"+ userId +"/RAX-KSQA/secretqa")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<SecretQA> unmarshaller = new ObjectMarshaller<SecretQA>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public Groups getGroups(String token) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud20Host + "RAX-GRPADM/groups")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public Groups getGroupsForUser(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud20Host + "users/" + userId + "/RAX-KSGRP")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        ObjectMarshaller<Groups> unmarshaller = new ObjectMarshaller<Groups>();
        return unmarshaller.unmarshal(response, Groups.class);
    }

    public CredentialListType getUserCredentials(String token, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud20Host + "users/" + userId + "/OS-KSADM/credentials")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, token)
            .get();

        CredentialListType credentialListType = new CredentialListType();
        ObjectMarshaller<CredentialListType> unmarshaller = new ObjectMarshaller<CredentialListType>();
        credentialListType = unmarshaller.unmarshal(response, CredentialListType.class);
        return credentialListType;
    }

    public com.rackspacecloud.docs.auth.api.v1.User getUserTenantsBaseUrls(String username, String password, String userId) throws URISyntaxException, HttpException, IOException, JAXBException {

        String response = client.url(cloud11Host + "users/" + userId + ".xml")
            .header(HttpHeaders.AUTHORIZATION, getBasicAuth(username, password))
            .get();

        ObjectMarshaller<com.rackspacecloud.docs.auth.api.v1.User> unmarshaller = new ObjectMarshaller<com.rackspacecloud.docs.auth.api.v1.User>();
        return unmarshaller.unmarshal(response, com.rackspacecloud.docs.auth.api.v1.User.class);
    }

    public BaseURLList getBaseUrls(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response = client.url(cloud11Host + "baseURLs.xml")
            .header(HttpHeaders.AUTHORIZATION, getBasicAuth(username, password))
            .get();

        ObjectMarshaller<BaseURLList> unmarshaller = new ObjectMarshaller<BaseURLList>();
        return unmarshaller.unmarshal(response, BaseURLList.class);
    }

    public EndpointTemplateList getEndpointTemplates(String adminToken) throws URISyntaxException, HttpException, IOException, JAXBException {
        String response = client.url(cloud20Host + "OS-KSCATALOG/endpointTemplates")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(X_AUTH_TOKEN, adminToken)
            .get();

        ObjectMarshaller<EndpointTemplateList> unmarshaller = new ObjectMarshaller<EndpointTemplateList>();
        return unmarshaller.unmarshal(response, EndPoints.class);
    }

    private String getBasicAuth(String username, String password) {
        String usernamePassword = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = usernamePassword.getBytes();
        return (new StringBuffer("Basic ").append(Base64.encode(base))).toString();
    }

	public void setCloud20Host(String host) {
		this.cloud20Host = host;
	}

	public void setCloud11Host(String host) {
		this.cloud11Host = host;
	}
}
