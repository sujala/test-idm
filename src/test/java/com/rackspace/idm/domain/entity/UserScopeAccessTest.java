package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: yung5027
 * Date: 7/27/12
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserScopeAccessTest {
    UserScopeAccess userScopeAccess;

    @Before
    public void setUp() throws Exception {
        userScopeAccess = new UserScopeAccess();
    }

    @Test
    public void getLDAPEntry_returnsLdapEntry() throws Exception {
        ReadOnlyEntry result = userScopeAccess.getLDAPEntry();
        assertThat("ldap entry", result, equalTo(null));
    }

    @Test
    public void getUniqueId_ldapEntryNotNull_returnsUniqueId() throws Exception {
        userScopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        String result = userScopeAccess.getUniqueId();
        assertThat("unique id", result, equalTo("uniqueId"));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        userScopeAccess.setAccessTokenString("expired");
        userScopeAccess.setAccessTokenExp(null);
        boolean result = userScopeAccess.isAccessTokenExpired(new DateTime(1));
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpNotNull_returnsFalse() throws Exception {
        userScopeAccess.setAccessTokenString("notExpired");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        boolean result = userScopeAccess.isAccessTokenExpired(new DateTime(1));
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void isRefreshTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        userScopeAccess.setRefreshTokenString("expired");
        userScopeAccess.setRefreshTokenExp(null);
        boolean result = userScopeAccess.isRefreshTokenExpired(new DateTime(1));
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_accessTokenExpNotNull_returnsFalse() throws Exception {
        userScopeAccess.setRefreshTokenString("notExpired");
        userScopeAccess.setRefreshTokenExp(new Date(3000, 1, 1));
        boolean result = userScopeAccess.isRefreshTokenExpired(new DateTime(1));
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void getAuditContext_returnsStringFormatContext() throws Exception {
        userScopeAccess.setUsername("username");
        userScopeAccess.setUserRCN("RCN");
        String result = userScopeAccess.getAuditContext();
        assertThat("audit context", result, equalTo("User(username=username,customerId=RCN)"));
    }
}
