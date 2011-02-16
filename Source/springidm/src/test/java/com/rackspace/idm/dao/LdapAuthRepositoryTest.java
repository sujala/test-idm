package com.rackspace.idm.dao;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.config.AuthRepositoryLdapConfiguration;
import com.rackspace.idm.test.stub.StubLogger;

@Ignore
public class LdapAuthRepositoryTest {
    private LdapAuthRepository repo;
    private LdapConnectionPools connPools;

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }
    
    private static LdapAuthRepository getRepo(LdapConnectionPools connPools) {
        return new LdapAuthRepository(connPools,  new StubLogger());
    }

    private static LdapConnectionPools getConnPools() {
        return new AuthRepositoryLdapConfiguration(true, new StubLogger()).connectionPools();
    }

    public boolean authenticateRacker() {
        return repo.authenticate("cloud_controlpanel", "Sj!wI2As");
    }

    @Test
    public void shouldNotAuthBadCred() {
        Assert.assertFalse(repo.authenticate("john.eo", "badpassword"));
    }

    /**
     * Making sure that the load-balancing by the LDAP directories don't explode
     * the StartTLS handshake.
     */
    @Test
    public void shouldAuthenticateRackerManyTimes() throws InterruptedException {
        // Just making sure that the connection pool isn't being depleted when
        // the connections are being closed after the authentication bind.
        for (int i = 0; i < 20; i++) {
            Assert.assertTrue(authenticateRacker());
            System.out.println("auth count: " + (i + 1));
        }
    }
    
    @After
    public void tearDown() {
        connPools.close();
    }
}
