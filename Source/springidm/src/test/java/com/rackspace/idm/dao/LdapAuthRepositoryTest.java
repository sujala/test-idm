package com.rackspace.idm.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.config.AuthRepositoryLdapConfiguration;
import com.rackspace.idm.test.stub.StubLogger;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;

@Ignore("Bamboo appears to be unable to connect to Rackspace's LDAP servers.")
public class LdapAuthRepositoryTest {
    private LdapAuthRepository repo;
    private LDAPConnectionPool connPool;
    private StartTLSExtendedRequest startTlsReq;

    @Before
    public void setUp() {
        AuthRepositoryLdapConfiguration config = new AuthRepositoryLdapConfiguration(
            true, new StubLogger());
        connPool = config.connection();
        startTlsReq = config.startTLSExtendedRequest();
        repo = new LdapAuthRepository(connPool, startTlsReq, new StubLogger());
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
}
