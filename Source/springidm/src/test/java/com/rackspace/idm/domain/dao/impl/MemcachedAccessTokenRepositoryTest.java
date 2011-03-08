package com.rackspace.idm.domain.dao.impl;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.MemcachedConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.test.stub.StubLogger;

public class MemcachedAccessTokenRepositoryTest {

    private static final String ANOTHER_TOKEN_STRING = "DELETE_ME_GOOD_TOO!";
    private static final String TOKEN_STRING = "DELETE_ME_GOOD!";
    private static final String TRUSTED_TOKEN_STRING = "DELETE_THIS_TRUSTED";
    private static final Set<String> tokenRequestors = new HashSet<String>();
    private MemcachedAccessTokenRepository repo;
    private String owner = "johneo";
    private String requestor = "controlpanel";
    private AccessToken token;

    @BeforeClass
    public static void setUpBeforeClass() {
        Configuration config = new PropertyFileConfiguration().getConfigFromClasspath();
        MemcachedClient mclient = new MemcachedConfiguration(config, new StubLogger()).memcacheClient();
        tokenRequestors.add("rackspace_control_panael");
        MemcachedAccessTokenRepository tempRepo = new MemcachedAccessTokenRepository(mclient, config);
        tempRepo.delete(TOKEN_STRING);
        tempRepo.delete(ANOTHER_TOKEN_STRING);
        tempRepo.delete(TRUSTED_TOKEN_STRING);
        tempRepo.deleteAllTokensForOwner("johneo");
        mclient.delete("johneo"); // Make sure to get rid of any stray trusted
                                  // tokens
    }

    @Before
    public void setUp() {
        Configuration config = new PropertyFileConfiguration().getConfigFromClasspath();
        MemcachedClient mclient = new MemcachedConfiguration(config, new StubLogger()).memcacheClient();
        repo = new MemcachedAccessTokenRepository(mclient, config);
        token = getNewToken(60);
    }

    @Test()
    public void shouldNotAcceptBadParams() {
        try {
            repo.save(null);
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.save(new AccessToken(null, null, getTestUser(), null, null));
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.delete(null);
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findByTokenString(null);
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findTokenForOwner(null, null);
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.deleteAllTokensForOwner(null);
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.delete(" ");
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findByTokenString(" ");
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findTokenForOwner(" ", " ");
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.deleteAllTokensForOwner("  ");
            Assert.fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldAddAndGetToken() {
        repo.save(token);
        Assert.assertEquals(token, repo.findByTokenString(token.getTokenString()));
        Assert.assertEquals(token, repo.findTokenForOwner(token.getOwner(), token.getRequestor()));
    }

    @Test
    public void shouldDeleteToken() {
        repo.save(token);
        repo.delete(token.getTokenString());
        Assert.assertNull(repo.findByTokenString(token.getTokenString()));
        Assert.assertNull(repo.findTokenForOwner(token.getOwner(), token.getRequestor()));
    }

    @Test
    public void shouldDeleteAllTokensForOwner() {
        repo.save(token);
        AccessToken anotherToken = getNewToken(60);
        anotherToken.setTokenString(ANOTHER_TOKEN_STRING);
        String anotherRequestor = getTestClient2().getClientId();
        anotherToken.setTokenClient(getTestClient2());
        repo.save(anotherToken);

        repo.deleteAllTokensForOwner(owner);

        Assert.assertNull(repo.findByTokenString(TOKEN_STRING));
        Assert.assertNull(repo.findByTokenString(ANOTHER_TOKEN_STRING));
        Assert.assertNull(repo.findTokenForOwner(owner, requestor));
        Assert.assertNull(repo.findTokenForOwner(owner, anotherRequestor));
    }

    @Test
    public void shouldNotOverwriteTokenOnPasswordReset() {
        repo.save(token);
        AccessToken resetToken = new AccessToken("reset_me_now", new DateTime().plusSeconds(60),
            getTestUser(), getTestClient(), IDM_SCOPE.SET_PASSWORD);
        repo.save(resetToken);
        Assert.assertNotNull(repo.findByTokenString(token.getTokenString()));
    }

    @Test
    public void shouldSaveAndFindClientToken() {
        AccessToken clientToken = getNewToken(60);
        clientToken.setTokenClient(getTestClient2());
        clientToken.setTokenUser(null);
        Assert.assertTrue(clientToken.isClientToken());
        repo.save(clientToken);
        AccessToken foundByTokenStr = repo.findByTokenString(clientToken.getTokenString());
        Assert.assertEquals(clientToken, foundByTokenStr);
        AccessToken foundByOwner = repo.findTokenForOwner(clientToken.getOwner(), clientToken.getRequestor());
        Assert.assertEquals(clientToken, foundByOwner);
    }

    @Test
    public void shouldSaveAndRevokeClientToken() {
        AccessToken clientToken = getNewToken(60);
        clientToken.setTokenClient(getTestClient2());
        clientToken.setTokenUser(null);
        Assert.assertTrue(clientToken.isClientToken());
        repo.save(clientToken);
        repo.delete(clientToken.getTokenString());
        Assert.assertNull(repo.findByTokenString(clientToken.getTokenString()));
        repo.save(clientToken);
        repo.deleteAllTokensForOwner(clientToken.getOwner());
        Assert.assertNull(repo.findByTokenString(clientToken.getTokenString()));
    }

    @Test
    public void shouldSaveAndFindTrustedToken() {
        AccessToken trustedToken = getNewTrustedToken(600);
        Assert.assertTrue(trustedToken.isTrusted());
        repo.save(trustedToken);
        AccessToken foundByTokenStr = repo.findByTokenString(trustedToken.getTokenString());
        Assert.assertEquals(trustedToken, foundByTokenStr);
        AccessToken foundByOwner = repo.findTokenForOwner(trustedToken.getOwner(),
            trustedToken.getRequestor());
        Assert.assertEquals(trustedToken, foundByOwner);
    }

    @Test
    public void shouldSaveAndRevokeTrustedToken() {
        AccessToken trustedToken = getNewTrustedToken(60);
        Assert.assertTrue(trustedToken.isTrusted());
        repo.save(trustedToken);
        repo.delete(trustedToken.getTokenString());
        Assert.assertNull(repo.findByTokenString(trustedToken.getTokenString()));
        repo.save(trustedToken);
        repo.deleteAllTokensForOwner(trustedToken.getOwner());
        Assert.assertNull(repo.findByTokenString(trustedToken.getTokenString()));
    }

    @Test
    public void shouldNotHaveCollisionBetweenRegularAndTrustedTokens() {
        AccessToken regular = getNewToken(60);
        repo.save(regular);
        AccessToken trusted = getNewTrustedToken(60);
        repo.save(trusted);
        Assert.assertEquals(regular, repo.findByTokenString(regular.getTokenString()));
        Assert.assertEquals(trusted, repo.findByTokenString(trusted.getTokenString()));
    }

    private AccessToken getNewToken(int expInSeconds) {
        return new AccessToken(TOKEN_STRING, new DateTime().plusSeconds(expInSeconds), getTestUser(),
            getTestClient(), IDM_SCOPE.FULL);
    }

    private AccessToken getNewTrustedToken(int expInSeconds) {
        return new AccessToken(TRUSTED_TOKEN_STRING, new DateTime().plusSeconds(expInSeconds), getTestUser(),
            getRackspaceTestClient(), IDM_SCOPE.FULL, true);
    }

    private BaseUser getTestUser() {
        return new BaseUser("johneo", "customerId");
    }

    private BaseClient getTestClient() {
        return new BaseClient("controlpanel", "customerId");
    }

    private BaseClient getTestClient2() {
        return new BaseClient("clientId2", "customerId");
    }

    private BaseClient getRackspaceTestClient() {
        return new BaseClient("RACKER", "RACKSPACE");
    }
}
