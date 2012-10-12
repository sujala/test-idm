package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.config.AuthRepositoryLdapConfiguration;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.*;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapAuthRepositoryIntegrationTest {
    @Autowired
    private LdapAuthRepository repo;
    @Autowired
    private LDAPConnectionPool connPool;

    public boolean authenticateRacker() {
        return repo.authenticate("cidm_user1", "lr7dy8qO");
    }

    @Test
    @Ignore("Still waiting on correct credentials")
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
    
    @Test
    @Ignore("Still waiting on trusted connection to eDir")
    public void shouldGetRoles() {
        List<String> roles = repo.getRackerRoles("matt.kovacs");
        Assert.assertTrue(roles.size()>0);
    }
}
