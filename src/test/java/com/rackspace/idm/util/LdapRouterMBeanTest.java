package com.rackspace.idm.util;

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.unboundid.ldap.sdk.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/3/12
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapRouterMBeanTest {

    LdapConnectionPools ldapConnectionPools;
    LdapRouterMBean ldapRouterMBean;
    LDAPConnectionPool ldapConnectionPool;

    @After
    public void tearDown() throws Exception {
        ldapConnectionPool.close();
    }

    @Before
    public void setUp() throws Exception {
        ldapConnectionPools = mock(LdapConnectionPools.class);
        ldapConnectionPool = new LDAPConnectionPool(new LDAPConnection("cert-ldap1.cidm.iad2.corp.rackspace.com", 636), 2, 2);
        when(ldapConnectionPools.getAppConnPool()).thenReturn(ldapConnectionPool);
        when(ldapConnectionPools.getBindConnPool()).thenReturn(ldapConnectionPool);
        ldapRouterMBean = new LdapRouterMBean();
        ldapRouterMBean.setConnPools(ldapConnectionPools);
    }

    @Test
    public void testGetNumApplicationAvailableConnections() throws Exception {
        Integer numConnections = ldapRouterMBean.getNumApplicationAvailableConnections();
        assertThat("Number of connections is 2", numConnections, equalTo(2));
    }

    @Test
    public void testGetNumApplicationFailedCheckouts() throws Exception {
        Long numFailedCheckouts = ldapRouterMBean.getNumApplicationFailedCheckouts();
        assertThat("Number of failed checkouts is 0", numFailedCheckouts, equalTo(0L));
    }

    @Test
    public void testGetNumBindAvailableConnections() throws Exception {
        Integer numConnections = ldapRouterMBean.getNumBindAvailableConnections();
        assertThat("Number of connections is 2", numConnections, equalTo(2));
    }

    @Test
    public void testGetNumBindFailedCheckouts() throws Exception {
        Long numFailedCheckouts = ldapRouterMBean.getNumBindFailedCheckouts();
        assertThat("Number of failed checkouts is 0", numFailedCheckouts, equalTo(0L));
    }

    @Test
    public void testGetBindConfiguration() throws Exception {
        String bindConfiguration = ldapRouterMBean.getBindConfiguration();
        assertThat("bind configuration", bindConfiguration, equalTo("LDAPConnectionPool(serverSet=SingleServerSet(server=cert-ldap1.cidm.iad2.corp.rackspace.com:636), maxConnections=2)"));
    }

    @Test
    public void testGetAppConfiguration() throws Exception {
        String appConfiguration = ldapRouterMBean.getAppConfiguration();
        assertThat("app configuration", appConfiguration, equalTo("LDAPConnectionPool(serverSet=SingleServerSet(server=cert-ldap1.cidm.iad2.corp.rackspace.com:636), maxConnections=2)"));
    }

    @Test
    public void testGetServerConnectionStatus() throws Exception {
        Map<String,String> serverConnectionStatus = ldapRouterMBean.getServerConnectionStatus();
        assertThat("server connection status", serverConnectionStatus.size(), equalTo(1));
        assertThat("server connection status", serverConnectionStatus.values().contains("up"), equalTo(true));
        assertThat("server connection status", serverConnectionStatus.values().contains("down"), equalTo(false));
    }

    @Test
    public void testReset_withConnection() throws Exception {
        ldapRouterMBean.reset();
        assertTrue("no exception thrown", true);
    }

    @Test
    public void testReset_withClosedConnection() throws Exception {
        ldapConnectionPool.close();
        ldapRouterMBean.reset();
        assertTrue("no exception thrown", true);
    }
}
