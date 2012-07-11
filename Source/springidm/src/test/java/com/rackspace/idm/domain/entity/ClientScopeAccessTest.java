package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/10/12
 * Time: 5:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientScopeAccessTest {

    ClientScopeAccess clientScopeAccess;

    @Before
    public void setUp() throws Exception {
        clientScopeAccess = new ClientScopeAccess();
    }

    @Test
    public void getUniqueId_ldapEntryNull_returnsNull() throws Exception {
        assertThat("returns  null",clientScopeAccess.getUniqueId(),equalTo(null));
    }

    @Test
    public void getUniqueId_ldapEntryExists_returnsDn() throws Exception {
        clientScopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        assertThat("returns dn",clientScopeAccess.getUniqueId(),equalTo("uniqueId"));
    }

    @Test
    public void setAccessTokenExp_setsDateToExpiredByOneDay() throws Exception {
        clientScopeAccess.setAccessTokenExpired();
        Date date = clientScopeAccess.getAccessTokenExp();
        assertThat("has expired date",date,greaterThan(new DateTime().minusDays(1).minusSeconds(1).toDate()));
        assertThat("has expired date",date,lessThan(new DateTime().minusDays(1).plusSeconds(1).toDate()));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringBlankAndTokenExpNull_returnTrue() throws Exception {
        clientScopeAccess.setAccessTokenString("");
        clientScopeAccess.setAccessTokenExp(null);
        assertThat("is expired",clientScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void testGetAuditContext() throws Exception {

    }
}
