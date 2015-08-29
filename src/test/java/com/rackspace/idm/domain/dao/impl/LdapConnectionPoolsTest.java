package com.rackspace.idm.domain.dao.impl;


import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import testHelpers.junit.java.ConditionalIgnoreRule;
import testHelpers.junit.java.IgnoreByRepositoryProfile;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/24/12
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
@IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
public class LdapConnectionPoolsTest {

    @Autowired(required = false)
    Configuration config;

    LdapConnectionPools ldapConnectionPools;

    LDAPConnectionPool appConnectionPool;
    LDAPConnectionPool bindConnectionPool;

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule();

    @Before
    public void setUp() throws Exception {
        LdapConfiguration ldapConfiguration = new LdapConfiguration(config);
        LdapConnectionPools connectionPools  = ldapConfiguration.connectionPools();
        appConnectionPool = connectionPools.getAppConnPool();
        bindConnectionPool = connectionPools.getBindConnPool();
        ldapConnectionPools  = new LdapConnectionPools(appConnectionPool, bindConnectionPool);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newLdapConnectionPools_withNullAppPool_throwsIllegalArgumentException() throws Exception {
        new LdapConnectionPools(null, bindConnectionPool);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newLdapConnectionPools_withNullBindPool_throwsIllegalArgumentException() throws Exception {
        new LdapConnectionPools(appConnectionPool, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newLdapConnectionPools_withSameBindAndAppPool_throwsIllegalArgumentException() throws Exception {
        new LdapConnectionPools(appConnectionPool, appConnectionPool);
    }

    @Test
    public void getAppConnPool_returnsCorrectPool() throws Exception {
        assertThat("app conn pool", ldapConnectionPools.getAppConnPool(), equalTo(appConnectionPool));
    }

    @Test
    public void getBindConnPool_returnsCorrectPool() throws Exception {
        assertThat("bind conn pool", ldapConnectionPools.getBindConnPool(), equalTo(bindConnectionPool));
    }

    @Test
    public void close_callsCloseOnConnectionPools() throws Exception {
        ldapConnectionPools.close();

        boolean appConnectionClosed = false;
        try {
            appConnectionPool.getConnection();
        } catch (LDAPException e) {
            appConnectionClosed = true;
        }

        boolean bindConnectionClosed = false;
        try {
            bindConnectionPool.getConnection();
        } catch (LDAPException e) {
            bindConnectionClosed = true;
        }

        assertThat("App conection closed", appConnectionClosed, equalTo(true));
        assertThat("Bind connection closed", bindConnectionClosed, equalTo(true));
    }

    @Test
    public void close_avoidsNullPointersOnClosedConnections() throws Exception {
        ldapConnectionPools.setAppConnPool(null);
        ldapConnectionPools.setBindConnPool(null);
        ldapConnectionPools.close();
        assertTrue("no exception thrown", true);
    }
}
