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
    public void isAccessTokenExpired_accessTokenStringIsBlank_returnsTrue() throws Exception {
        clientScopeAccess.setAccessTokenString("");
        boolean result = clientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        clientScopeAccess.setAccessTokenString("notBlank");
        clientScopeAccess.setAccessTokenExp(null);
        boolean result = clientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        clientScopeAccess.setAccessTokenString("notBlank");
        clientScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = clientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        clientScopeAccess.setAccessTokenString("notBlank");
        clientScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = clientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void getAuditContext_returnsAuditString() throws Exception {
        clientScopeAccess.setClientId("clientId");
        clientScopeAccess.setClientRCN("clientRCN");
        String result = clientScopeAccess.getAuditContext();
        assertThat("audit string", result, equalTo("Client(clientId=clientId,customerId=clientRCN)"));
    }
}
