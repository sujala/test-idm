package com.rackspace.idm.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class WebClientAccessTokenRepositoryTest {
    private Client c = Client.create();
    private WebClientAccessTokenRepository repo;

    @Before
    public void setUp() {
        WebResource wr = c.resource("http://10.127.7.166:8080/v1.0");
        DataCenterClient devServer = new DataCenterClient("DEV", wr);
        DataCenterEndpoints endpoints = new DataCenterEndpoints();
        endpoints.put(devServer);

        // Credentials for Customer IDM
        AuthCredentials creds = new AuthCredentials();
        creds.setClientId("18e7a7032733486cd32f472d7bd58f709ac0d221");
        creds.setClientSecret("password");
        creds.setGrantType(AuthGrantType.NONE);

        repo = new WebClientAccessTokenRepository(endpoints, creds,
            new StubLogger());
    }

    @Test
    public void shouldGetClientToken() {
        AccessToken idmTk = repo.getMyAccessToken("DEV");
        Assert.assertNotNull(idmTk);
    }

    @Test
    public void shouldLookForTokenAccrossDc() {
        AccessToken idmTk = repo.getMyAccessToken("DEV");
        AccessToken foundTk = repo.findByTokenString(idmTk.getTokenString());
        Assert.assertNotNull(foundTk);
    }
}