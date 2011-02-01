package com.rackspace.idm.dao;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.config.MemcachedConfiguration;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class WebClientAccessTokenRepositoryTest {
    private static final String QA_TOKEN_STRING = "QAM-xdctesttokenstring";
    private Client c = Client.create();
    private WebClientAccessTokenRepository repo;

    @Before
    public void setUp() {
        WebResource wrqa = c.resource("http://10.127.7.164:8080/v1.0");
        DataCenterClient qaServer = new DataCenterClient("QAM", wrqa);
        DataCenterEndpoints endpoints = new DataCenterEndpoints();
        endpoints.put(qaServer);

        // Credentials for Customer IDM
        AuthCredentials creds = new AuthCredentials();
        creds.setClientId("18e7a7032733486cd32f472d7bd58f709ac0d221");
        creds.setClientSecret("password");
        creds.setGrantType(AuthGrantType.NONE);

        repo = new WebClientAccessTokenRepository(endpoints, creds,
            new StubLogger());
    }

    @Test
    public void shouldLookForTokenAcrossDc() {
        // Use the QA memcached server to simulated XDC token store
        Configuration qaconfig = new PropertiesConfiguration();
        qaconfig.addProperty("memcached.serverList", "10.127.7.165:11211");
        MemcachedClient mclient = new MemcachedConfiguration(qaconfig,
            new StubLogger()).memcacheClient();
        AccessToken token = getNewToken(60);
        // Add a token to a "cross-data-center" location
        mclient.set(QA_TOKEN_STRING, 600, token);
        // TODO The token storage scheme will change
        mclient.set(token.getOwner() + "_" + token.getRequestor(), 600, token);

        // Now attempt a lookup from the local DAO
        AccessToken remoteToken = repo.findByTokenString(QA_TOKEN_STRING);
        Assert.assertNotNull(remoteToken);

        // Try multiple times to see if you get the same result;
        remoteToken = repo.findByTokenString(QA_TOKEN_STRING);
        Assert.assertNotNull(remoteToken);
        remoteToken = repo.findByTokenString(QA_TOKEN_STRING);
        Assert.assertNotNull(remoteToken);
    }

    @Test
    public void shouldGetClientToken() {
        AccessToken idmTk = repo.getMyAccessToken("QAM");
        Assert.assertNotNull(idmTk);
    }

    private AccessToken getNewToken(int expInSeconds) {
        return new AccessToken(QA_TOKEN_STRING,
            new DateTime().plusSeconds(expInSeconds), null, getTestClient(),
            IDM_SCOPE.FULL);
    }

    private BaseClient getTestClient() {
        return new BaseClient("controlpanel", "customerId");
    }
}