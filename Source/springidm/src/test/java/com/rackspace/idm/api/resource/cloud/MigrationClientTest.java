package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.Tenants;
import org.openstack.docs.identity.api.v2.User;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


public class MigrationClientTest {

	private MigrationClient client;
	private String username;
	private String password;
       
	@Before
	public void setup() {
		username = "auth";
		password = "auth123";
				
		client = new MigrationClient();
		client.setHost("https://auth.staging.us.ccp.rackspace.net/v2.0/");
	}
	
	@Test
	public void getTenants_validUser_returnsTenants() throws URISyntaxException, HttpException, IOException, JAXBException {
		String token = getToken();
		Tenants tenants = client.getTenants(token);
		
		assertThat("tenants", tenants, notNullValue());
	}
	
	@Test
	public void getUser_validUserName_returnsUser() throws URISyntaxException, HttpException, IOException, JAXBException {
		String token = getToken();
		User user = client.getUser(token, username);
		
		assertThat("user", user, notNullValue());
	}
	
	@Test
	public void getUserGroup_validUser_returnsGroups() throws URISyntaxException, HttpException, IOException, JAXBException {
		String token = getToken();
		User user = client.getUser(token, "cmarin1");
		Groups groups = client.getGroupsForUser(token, user.getId());
		
		assertThat("groups", groups, notNullValue());
	}
	
	@Test
	public void getGroups_validToken_returnsGroups() throws URISyntaxException, HttpException, IOException, JAXBException {
		String token = getToken();
		Groups groups = client.getGroups(token);
		
		assertThat("groups", groups, notNullValue());
	}
	
	@Test
	public void getUserCredentials_validToken_returnsCredentials() throws URISyntaxException, HttpException, IOException, JAXBException {
		String token = getToken();
		User user = client.getUser(token, "cmarin1");
		CredentialListType credentials = client.getUserCredentials(token, user.getId());
		
		assertThat("credentials", credentials, notNullValue());
	}

	private String getToken() throws URISyntaxException, HttpException, IOException, JAXBException {
		AuthenticateResponse  authenticateResponse = client.authenticate(username, password);
		return authenticateResponse.getToken().getId();
	}
}
