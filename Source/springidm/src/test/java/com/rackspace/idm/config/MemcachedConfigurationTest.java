package com.rackspace.idm.config;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.entities.BaseUser;
import com.rackspace.idm.test.stub.StubLogger;
import net.spy.memcached.MemcachedClient;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MemcachedConfigurationTest {
    private MemcachedClient mclient;
    private MemcachedConfiguration mconfig;

    public MemcachedConfigurationTest() {
        mconfig = new MemcachedConfiguration(new PropertyFileConfiguration()
                .getConfigFromClasspath(), new StubLogger());
    }

    @Before
    public void setUp() {
        mclient = mconfig.memcacheClient();
    }

    @Test
    public void shouldGetMemcacheClient() {
        Assert.assertNotNull(mclient);
    }

    @Test
    public void shouldPutStuffIntoServer() {
        AccessToken token = new AccessToken("XXXXX", new DateTime()
                .plusSeconds(5), getTestUser(), getTestClient(), IDM_SCOPE.FULL);
        mclient.set("foo", 5, token);
        Assert.assertEquals(token, mclient.get("foo"));
    }

    @Test
    public void shouldExpire() throws InterruptedException {
        AccessToken token = new AccessToken("XXXXX", new DateTime()
                .plusSeconds(5), getTestUser(), getTestClient(), IDM_SCOPE.FULL);
        mclient.set("foo", 1, token);
        Assert.assertEquals(token, mclient.get("foo"));
        Thread.sleep(1000);
        Assert.assertNull(mclient.get("foo"));
    }

    private BaseUser getTestUser() {
        return new BaseUser("username", "customerId");
    }

    private BaseClient getTestClient() {
        return new BaseClient("clientId", "customerId");
    }
}
