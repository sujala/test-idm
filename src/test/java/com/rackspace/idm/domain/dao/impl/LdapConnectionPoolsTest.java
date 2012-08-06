package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.LDAPConnectionPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;


/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/24/12
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LDAPConnectionPool.class)  //Dont use power mock unless absolutely necessary it takes FOREVER to set up.
public class LdapConnectionPoolsTest {
    LdapConnectionPools ldapConnectionPools;
    LDAPConnectionPool appConnectionPool;
    LDAPConnectionPool bindConnectionPool;

    @Before
    public void setUp() throws Exception {
        appConnectionPool = mock(LDAPConnectionPool.class);
        bindConnectionPool = mock(LDAPConnectionPool.class);
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
        verify(appConnectionPool).close();
        verify(bindConnectionPool).close();
    }

    @Test
    public void close_avoidsNullPointersOnClosedConnections() throws Exception {
        ldapConnectionPools.setAppConnPool(null);
        ldapConnectionPools.setBindConnPool(null);
        ldapConnectionPools.close();
        assertTrue("no exception thrown", true);
    }
}
