package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.config.MemcachedConfiguration;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.entities.BaseUser;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.RoleStatus;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@Ignore("Rackspace Dev and QA servers are down, testing locally")
public class WebClientAccessTokenRepositoryTest {
    private static final String QA_TOKEN_STRING = "QAM-xdctesttokenstring";
    private Client c = Client.create();
    private WebClientAccessTokenRepository repo;

    @Before
    public void setUp() {
        WebResource wrqa = c.resource("http://172.17.16.85:8080/v1.0");
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
        qaconfig.addProperty("memcached.serverList", "172.17.16.84:11211");
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
        Assert.assertNotNull(remoteToken.getTokenUser());
        Assert.assertNotNull(remoteToken.getTokenClient());
    }

    @Test
    public void shouldGetClientToken() {
        AccessToken idmTk = repo.getMyAccessToken("QAM");
        Assert.assertNotNull(idmTk);
    }

    private AccessToken getNewToken(int expInSeconds) {
        return new AccessToken(QA_TOKEN_STRING,
            new DateTime().plusSeconds(expInSeconds), getTestUser(),
            getTestClient(), IDM_SCOPE.FULL);
    }

    private BaseUser getTestUser() {
        
        ClientGroup group = new ClientGroup("customerId", "clientId", "name");
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(group);

        return new BaseUser("userTested", "customerId", groups);
    }

    private BaseClient getTestClient() {
        Permission perm = new Permission("foo", "bar", "baz", "what");
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(perm);
        return new BaseClient("controlpanel", "customerId", perms);
    }
}