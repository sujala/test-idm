package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/19/12
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class RackerScopeAccessTest {
    RackerScopeAccess rackerScopeAccess;

    @Before
    public void setUp() throws Exception {
        rackerScopeAccess = new RackerScopeAccess();
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnsNull() throws Exception {
        rackerScopeAccess.setLdapEntry(null);
        String result = rackerScopeAccess.getUniqueId();
        assertThat("ldap entry", result, equalTo(null));
    }

    @Test
    public void getUniqueId_ldapEntryNotNull_returnsUniqueId() throws Exception {
        rackerScopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        String result = rackerScopeAccess.getUniqueId();
        assertThat("ldap entry", result, equalTo("uniqueId"));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringIsBlank_returnsTrue() throws Exception {
        rackerScopeAccess.setAccessTokenString("");
        boolean result = rackerScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        rackerScopeAccess.setAccessTokenString("notBlank");
        rackerScopeAccess.setAccessTokenExp(null);
        boolean result = rackerScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        rackerScopeAccess.setAccessTokenString("notBlank");
        rackerScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = rackerScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        rackerScopeAccess.setAccessTokenString("notBlank");
        rackerScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = rackerScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }


    @Test
    public void isRefreshTokenExpired_RefreshTokenStringIsBlank_returnsTrue() throws Exception {
        rackerScopeAccess.setRefreshTokenString("");
        boolean result = rackerScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsNull_returnsTrue() throws Exception {
        rackerScopeAccess.setRefreshTokenString("notBlank");
        rackerScopeAccess.setRefreshTokenExp(null);
        boolean result = rackerScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsBeforeTime_returnsTrue() throws Exception {
        rackerScopeAccess.setRefreshTokenString("notBlank");
        rackerScopeAccess.setRefreshTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = rackerScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        rackerScopeAccess.setRefreshTokenString("notBlank");
        rackerScopeAccess.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = rackerScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void getAuditContext_returnsAuditString() throws Exception {
        rackerScopeAccess.setRackerId("rackerId");
        String result = rackerScopeAccess.getAuditContext();
        assertThat("audit string", result, equalTo("Racker(rackerId=rackerId)"));
    }

    @Test
    public void setRefreshTokenExpired_expiresRefreshToken() throws Exception {
        rackerScopeAccess.setRefreshTokenString("refreshToken");
        rackerScopeAccess.setRefreshTokenExpired();
        boolean result = rackerScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }
}
