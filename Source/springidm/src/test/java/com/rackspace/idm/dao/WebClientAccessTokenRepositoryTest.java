package com.rackspace.idm.dao;

import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.entities.AccessToken;
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
        repo = new WebClientAccessTokenRepository(endpoints, new StubLogger());
    }

    @Test
    public void shouldGetClientToken() {
        AccessToken tk = repo.getMyAccessToken("DEV");
    }
}