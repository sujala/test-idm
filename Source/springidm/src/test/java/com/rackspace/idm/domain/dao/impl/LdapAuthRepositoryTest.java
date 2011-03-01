package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.domain.config.AuthRepositoryLdapConfiguration;
import com.rackspace.idm.domain.dao.impl.LdapAuthRepository;
import com.rackspace.idm.test.stub.StubLogger;
import com.unboundid.ldap.sdk.LDAPConnectionPool;

public class LdapAuthRepositoryTest {
    private LdapAuthRepository repo;
    private LDAPConnectionPool connPool;

    @Before
    public void setUp() {
        connPool = getConnPool();
        repo = getRepo(connPool);
    }

    private static LdapAuthRepository getRepo(LDAPConnectionPool connPool) {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration("auth.repository.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapAuthRepository(connPool, config);
    }

    private static LDAPConnectionPool getConnPool() {
        return new AuthRepositoryLdapConfiguration(true, new StubLogger())
            .connection();
    }

    public boolean authenticateRacker() {
        return repo.authenticate("cidm_user1", "lr7dy8qO");
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
    @Ignore("Still waiting on correct credentials")
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
        connPool.close();
    }
}
