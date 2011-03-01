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

import com.rackspace.idm.domain.config.DataCenterEndpoints;
import com.rackspace.idm.domain.config.MemcachedConfiguration;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.api.client.Client;

public class HttpAccessTokenRepositoryTest {
    private static final String TOKEN_OWNER = "userTested";
    private static final String TOKEN_REQUESTOR = "controlpanel";
    private static final String IDM_CLIENT_ID = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    private static final String QA_TOKEN_STRING = "QA-xdctesttokenstring";
    private Client c = Client.create();
    private MemcachedClient mcdRemote;
    private HttpAccessTokenRepository repo;

    @Before
    public void setUp() {
        // Use the QA memcached server to simulated XDC token store
        Configuration config = new PropertiesConfiguration();
        config.addProperty("memcached.serverList", "10.127.7.165:11211");
        config.addProperty("idm.clientId", IDM_CLIENT_ID);
        config.addProperty("idm.clientSecret", "password");
        String[] dcs = {"QA|http://10.127.7.164:8080/v1.0"};
        config.addProperty("dc", dcs);

        DataCenterEndpoints endpoints = new DataCenterEndpoints(config);
        mcdRemote = new MemcachedConfiguration(config, new StubLogger()).memcacheClient();
        // Delete any old token
        deleteUserTokenInMemcached();
        repo = new HttpAccessTokenRepository(endpoints, config);
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
        int tokenExpInSec = 600;
        AccessToken token = getNewToken(tokenExpInSec);
        // Add a token to a "cross-data-center" location, with expiration set in
        // seconds.
        mcdRemote.set(QA_TOKEN_STRING, tokenExpInSec, token);
        mcdRemote.set(getTokenKeyByClientId(token.getOwner(), token.getRequestor()), tokenExpInSec,
            token.getTokenString());
    }

    @Test
    public void shouldFindForTokenAcrossDc() {
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
        AccessToken idmTk = repo.getMyAccessToken("QA", false);
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
