package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void getUniqueId_ldapEntryNotNull_returnsLdapEntryGetDn() throws Exception {
        impersonatedScopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        String result = impersonatedScopeAccess.getUniqueId();
        assertThat("unique id", result, equalTo("uniqueId"));
    }
}
