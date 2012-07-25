package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import org.apache.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MigrationClientTest {

    private MigrationClient client;
    private String username;
    private String password;
    private String password1;
    private String username2;
    private String username3;
    private String userId3;

    @Before
    public void setup() {
        username = "auth";
        password = "auth123";
        username2 = "cmarin2";
        password1 = "Password1";
        username3 = "carlostest3";
        userId3 = "175017";

        client = new MigrationClient();
        client.setCloud11Host("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        client.setCloud20Host("https://auth.staging.us.ccp.rackspace.net/v2.0/");
    }
    
    @Test
    public void getTenants_validUser_returnsTenants() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        Tenants tenants = client.getTenants(token);
        
        assertThat("tenants", tenants, notNullValue());
    }
    
    @Test
    public void getUser_validUserName_returnsUser() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        User user = client.getUser(token, username);
        
        assertThat("user", user, notNullValue());
    }
    
    @Test
    public void getUsers_validUserName_returnsUsers() throws URISyntaxException, HttpException, IOException, JAXBException {
    	String token = getToken(username2, password1);
        UserList users = client.getUsers(token);
        
        assertThat("users", users, notNullValue());
    }

    @Test
    public void getRoles_validUserName_returnsRoles() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        User user = client.getUser(token, "cmarin2");
        RoleList roles = client.getRolesForUser(token, user.getId());

        assertThat("roles", roles, notNullValue());
    }
    
    @Test
    public void getSecretQA_validUserName_returnsSecretQA() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        SecretQA secretQA = client.getSecretQA(token, userId3);

        assertThat("secretQA", secretQA, notNullValue());
    }

    @Test
    public void getUserGroup_validUser_returnsGroups() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        User user = client.getUser(token, "cmarin2");
        Groups groups = client.getGroupsForUser(token, user.getId());
        
        assertThat("groups", groups, notNullValue());
    }
    
    @Test
    public void getGroups_validToken_returnsGroups() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        Groups groups = client.getGroups(token);
        
        assertThat("groups", groups, notNullValue());
    }
    
    @Test
    public void getUserCredentials_validToken_returnsCredentials() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        User user = client.getUser(token, username2);
        CredentialListType credentials = client.getUserCredentials(token, user.getId());
        
        assertThat("credentials", credentials, notNullValue());
    }

    @Test
    public void getUserTenantsBaseUrls_validToken_returnsUser() throws URISyntaxException, HttpException, IOException, JAXBException {
        com.rackspacecloud.docs.auth.api.v1.User user = client.getUserTenantsBaseUrls(username, password, "cmarin2");
        assertThat("User", user, notNullValue());
    }


    @Test
    public void getBaseUrls_validToken_returnsBaseUrls() throws URISyntaxException, HttpException, IOException, JAXBException {
        BaseURLList baseURLList = client.getBaseUrls(username, password);
        assertThat("baseURLList", baseURLList, notNullValue());
    }
   
    @Test
    public void getRoles_validToken_returnsRoles() throws URISyntaxException, HttpException, IOException, JAXBException {
        String token = getAdminToken();
        RoleList roles = client.getRoles(token);
        assertThat("roles", roles, notNullValue());
    }

    @Test
    public void getEndpoints() throws URISyntaxException, HttpException, IOException, JAXBException {
        EndpointList endPoints = client.getEndpointsByToken(getAdminToken(), getToken("cmarin2", "Password1"));
        assertThat("endPoints", endPoints, notNullValue());
    }

    private String getAdminToken() throws URISyntaxException, HttpException, IOException, JAXBException {
        AuthenticateResponse  authenticateResponse = client.authenticateWithPassword(username, password);
        return authenticateResponse.getToken().getId();
    }

    private String getToken(String username, String password) throws URISyntaxException, HttpException, IOException, JAXBException {
        AuthenticateResponse authenticateResponse = client.authenticateWithPassword(username, password);
        return authenticateResponse.getToken().getId();
    }

    @Test
    public void authenticateWithApiKey_returnsAuthenticateResponse() throws Exception {
        AuthenticateResponse authenticateResponse = client.authenticateWithApiKey("auth", "aaaaa-bbbbb-ccccc-12345678");
        assertThat("token", authenticateResponse.getToken(), notNullValue());
    }
}
