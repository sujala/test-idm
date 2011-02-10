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
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class WebClientAccessTokenRepositoryTest {
    private static final String TOKEN_OWNER = "userTested";
    private static final String TOKEN_REQUESTOR = "controlpanel";
    private static final String IDM_CLIENT_ID = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    private static final String QA_TOKEN_STRING = "QA-xdctesttokenstring";
    private Client c = Client.create();
    private MemcachedClient mcdRemote;
    private HttpAccessTokenRepository repo;

    @Before
    public void setUp() {
        // Client for the "QA" remote instance of IDM
        WebResource wrqa = c.resource("http://10.127.7.164:8080/v1.0");
        DataCenterClient qaServer = new DataCenterClient("QA", wrqa);
        DataCenterEndpoints endpoints = new DataCenterEndpoints();
        endpoints.put(qaServer);

        // Credentials for Customer IDM
        AuthCredentials creds = new AuthCredentials();
        creds.setClientId(IDM_CLIENT_ID);
        creds.setClientSecret("password");
        creds.setGrantType(AuthGrantType.NONE);

        // Use the QA memcached server to simulated XDC token store
        Configuration qaconfig = new PropertiesConfiguration();
        qaconfig.addProperty("memcached.serverList", "10.127.7.165:11211");
        mcdRemote = new MemcachedConfiguration(qaconfig, new StubLogger()).memcacheClient();

        // Delete any old token
        deleteUserTokenInMemcached();

        repo = new HttpAccessTokenRepository(endpoints, creds, new StubLogger());
    }

    @Test
    @Ignore("Inserts test token on the 'remote' server's memcached. Invoke manually for debugging the client service call.")
    public void deleteUserTokenInMemcached() {
        mcdRemote.delete(QA_TOKEN_STRING);
        mcdRemote.delete(getTokenKeyByClientId(TOKEN_OWNER, TOKEN_REQUESTOR));
    }

    @Test
    @Ignore("Inserts test token on the 'remote' server's memcached. Invoke manually for debugging the client service call.")
    public void putUserTokenInMemcached() {
        AccessToken token = getNewToken(600);
        // Add a token to a "cross-data-center" location, with expiration set in
        // seconds.
        mcdRemote.set(QA_TOKEN_STRING, 600, token);
        mcdRemote.set(getTokenKeyByClientId(token.getOwner(), token.getRequestor()), 600,
            token.getTokenString());
    }

    @Test
    public void shouldLookForTokenAcrossDc() {
        putUserTokenInMemcached();

        // Now attempt a lookup from the local DAO
        AccessToken remoteToken = repo.findByTokenString(QA_TOKEN_STRING);
        Assert.assertNotNull(remoteToken);
        Assert.assertNotNull(remoteToken.getTokenUser());
        Assert.assertNotNull(remoteToken.getTokenClient());
    }

    private String getTokenKeyByClientId(String owner, String requestor) {
        return owner + "_" + requestor;
    }

    @Test
    public void shouldGetMyToken() {
        AccessToken idmTk = repo.getMyAccessToken("QA");
        Assert.assertNotNull(idmTk);
    }

    private AccessToken getNewToken(int expInSeconds) {
        return new AccessToken(QA_TOKEN_STRING, new DateTime().plusSeconds(expInSeconds), getTestUser(),
            getTestClient(), IDM_SCOPE.FULL);
    }

    private BaseUser getTestUser() {

        ClientGroup group = new ClientGroup("customerId", "clientId", "name");
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(group);

        return new BaseUser(TOKEN_OWNER, "customerId", groups);
    }

    private BaseClient getTestClient() {
        Permission perm = new Permission("foo", "bar", "baz", "what");
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(perm);
        return new BaseClient(TOKEN_REQUESTOR, "customerId", perms);
    }
}
