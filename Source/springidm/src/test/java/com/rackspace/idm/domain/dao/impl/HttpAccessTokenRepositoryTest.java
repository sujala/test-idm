package com.rackspace.idm.domain.dao.impl;

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
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.test.stub.StubLogger;

public class HttpAccessTokenRepositoryTest {
    private static final String TOKEN_OWNER = "userTested";
    private static final String TOKEN_REQUESTOR = "controlpanel";
    private static final String IDM_CLIENT_ID = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    private static final String QA_TOKEN_STRING = "QA-xdctesttokenstring";
    private MemcachedAccessTokenRepository tokenRepo;
    private HttpAccessTokenRepository repo;
    private DataCenterEndpoints endpoints;
    private Configuration config;

    public HttpAccessTokenRepositoryTest() {
        // Config for memcached client
        config = new PropertiesConfiguration();
        config.addProperty("memcached.serverList", "10.127.7.165:11211");
        config.addProperty("idm.clientId", IDM_CLIENT_ID);
        config.addProperty("idm.clientSecret", "password");
        String[] dcs = {"QA|http://10.127.7.164:8080/v1.0"};
        config.addProperty("dc", dcs);

        // Config for memcached repo
        config.addProperty("racker.client_id", "RACKER");

        endpoints = new DataCenterEndpoints(config);
        MemcachedClient mcdRemote = new MemcachedConfiguration(config, new StubLogger()).memcacheClient();
        tokenRepo = new MemcachedAccessTokenRepository(mcdRemote, config);
    }

    @Before
    public void setUp() {

        // Delete any old token
        deleteUserTokenInMemcached();
        repo = new HttpAccessTokenRepository(endpoints, config);
    }

    @Test
    @Ignore("Inserts test token on the 'remote' server's memcached. Invoke manually for debugging the client service call.")
    public void deleteUserTokenInMemcached() {
        tokenRepo.delete(QA_TOKEN_STRING);
    }

    @Test
    @Ignore("Inserts test token on the 'remote' server's memcached. Invoke manually for debugging the client service call.")
    public void putUserTokenInMemcached() {
        int tokenExpInSec = 600;
        AccessToken token = getNewToken(tokenExpInSec);
        // Add a token to a "cross-data-center" location, with expiration set in
        // seconds.
        tokenRepo.save(token);
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

    private UserTokenStrings getUserTokenStrings(String owner, String requestor, int exp, String tokenString) {
        UserTokenStrings uts = new UserTokenStrings(owner);
        uts.put(requestor, exp, tokenString);
        return uts;
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
