package com.rackspace.idm.dao;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import net.spy.memcached.MemcachedClient;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.MemcachedConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.entities.BaseUser;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.test.stub.StubLogger;

public class MemcachedAccessTokenRepositoryTest {

    private static final String TOKEN_STRING = "DELETE_ME_GOOD!";
    private static final Set<String> tokenRequestors = new HashSet<String>();
    private MemcachedAccessTokenRepository repo;
    private String owner = "johneo";
    private String requestor = "controlpanel";
    private AccessToken token;

    @BeforeClass
    public static void setUpBeforeClass() {
        MemcachedClient mclient = new MemcachedConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath(),
            new StubLogger()).memcacheClient();
        tokenRequestors.add("rackspace_control_panael");
        MemcachedAccessTokenRepository tempRepo = new MemcachedAccessTokenRepository(mclient);
        tempRepo.delete(TOKEN_STRING);
    }

    @Before
    public void setUp() {
        MemcachedClient mclient = new MemcachedConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath(),
            new StubLogger()).memcacheClient();
        repo = new MemcachedAccessTokenRepository(mclient);
        token = getNewToken(60);
    }

    @Test()
    public void shouldNotAcceptBadParams() {
        try {
            repo.save(null);
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.save(new AccessToken(null, null, getTestUser(), null, null));
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.delete(null);
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findByTokenString(null);
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findTokenForOwner(null, null);
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.deleteAllTokensForOwner(null, null);
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.delete(" ");
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findByTokenString(" ");
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.findTokenForOwner(" ", " ");
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            repo.deleteAllTokensForOwner("  ", new HashSet<String>());
            Assert
                .fail("did not throw an exception when bad param(s) was passed in!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldAddAndGetToken() {
        repo.save(token);
        Assert.assertEquals(token, repo.findByTokenString(token
            .getTokenString()));
        Assert.assertEquals(token, repo.findTokenForOwner(token.getOwner(),
            token.getRequestor()));
    }

    @Test
    public void shouldDeleteToken() {
        repo.save(token);
        repo.delete(token.getTokenString());
        Assert.assertNull(repo.findByTokenString(token.getTokenString()));
        Assert.assertNull(repo.findTokenForOwner(token.getOwner(), token
            .getRequestor()));
    }

    @Test
    public void shoulddeleteAllTokensForOwner() {
        repo.save(token);
        AccessToken anotherToken = getNewToken(60);
        String anotherTokenString = "DELETE_ME_GOOD_TOO!";
        anotherToken.setTokenString(anotherTokenString);
        String anotherRequestor = getTestClient2().getClientId();
        anotherToken.setTokenClient(getTestClient2());
        repo.save(anotherToken);

        Set<String> requestors = new HashSet<String>();
        requestors.add(requestor);
        requestors.add(anotherRequestor);
        repo.deleteAllTokensForOwner(owner, requestors);

        Assert.assertNull(repo.findByTokenString(TOKEN_STRING));
        Assert.assertNull(repo.findByTokenString(anotherTokenString));
        Assert.assertNull(repo.findTokenForOwner(owner, requestor));
        Assert.assertNull(repo.findTokenForOwner(owner, anotherRequestor));
    }

    public void shouldNotOverwriteTokenOnPasswordReset() {
        AccessToken resetToken = new AccessToken("reset me now", new DateTime().plusSeconds(60),
                getTestUser(), getTestClient(), IDM_SCOPE.FULL);
        repo.save(resetToken);
        Assert.assertNotNull(repo.findByTokenString(token.getTokenString()));
    }
    private AccessToken getNewToken(int expInSeconds) {
        return new AccessToken(TOKEN_STRING, new DateTime()
            .plusSeconds(expInSeconds), getTestUser(), getTestClient(), IDM_SCOPE.FULL);
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
}
