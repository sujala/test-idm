package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/18/12
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImpersonatedScopeAccessTest {
    ImpersonatedScopeAccess impersonatedScopeAccess;

    @Before
    public void setUp() throws Exception {
        impersonatedScopeAccess = new ImpersonatedScopeAccess();
    }

    @Test
    public void getClientId_returnsClientId() throws Exception {
        impersonatedScopeAccess.setClientId("clientId");
        String result = impersonatedScopeAccess.getClientId();
        assertThat("client id", result, equalTo("clientId"));
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnsNull() throws Exception {
        impersonatedScopeAccess.setLdapEntry(null);
        String result = impersonatedScopeAccess.getUniqueId();
        assertThat("unique id", result, equalTo(null));
    }
}
