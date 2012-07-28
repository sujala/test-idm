package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.joda.time.DateTime;
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

    @Test
    public void getUserPasswordExpirationDate_returnsUserPasswordExpirationDate() throws Exception {
        DateTime dateTime = new DateTime(1);
        impersonatedScopeAccess.setUserPasswordExpirationDate(dateTime);
        DateTime result = impersonatedScopeAccess.getUserPasswordExpirationDate();
        assertThat("user password expiration date", result, equalTo(dateTime));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringIsBlank_returnsTrue() throws Exception {
        impersonatedScopeAccess.setAccessTokenString("");
        boolean result = impersonatedScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        impersonatedScopeAccess.setAccessTokenString("notBlank");
        impersonatedScopeAccess.setAccessTokenExp(null);
        boolean result = impersonatedScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        impersonatedScopeAccess.setAccessTokenString("notBlank");
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = impersonatedScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        impersonatedScopeAccess.setAccessTokenString("notBlank");
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = impersonatedScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void setAccessTokenExpired_expiresToken() throws Exception {
        impersonatedScopeAccess.setAccessTokenString("tokenString");
        impersonatedScopeAccess.setAccessTokenExpired();
        boolean result = impersonatedScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }
}
