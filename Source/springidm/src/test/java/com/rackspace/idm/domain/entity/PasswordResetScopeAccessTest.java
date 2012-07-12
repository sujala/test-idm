package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordResetScopeAccessTest {

    PasswordResetScopeAccess passwordResetScopeAccess;

    @Before
    public void setUp() throws Exception {
        passwordResetScopeAccess = new PasswordResetScopeAccess();
    }

    @Test
    public void getClientId_() throws Exception {
        passwordResetScopeAccess.setClientId("clientId");
        assertThat("client id",passwordResetScopeAccess.getClientId(),equalTo("clientId"));
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnsNull() throws Exception {
        assertThat("returns null",passwordResetScopeAccess.getUniqueId(),equalTo(null));
    }

    @Test
    public void getUniqueId_ldapEntryIsNotNull_returnsDn() throws Exception {
        passwordResetScopeAccess.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        assertThat("returns dn", passwordResetScopeAccess.getUniqueId(), equalTo("dn"));
    }

    @Test
    public void getUsername_returnsCorrectUsername() throws Exception {
        passwordResetScopeAccess.setUsername("username");
        assertThat("returns username",passwordResetScopeAccess.getUsername(),equalTo("username"));
    }

    @Test
    public void getUserRCN() throws Exception {
        passwordResetScopeAccess.setUserRCN("userRCN");
        assertThat("returns user rcn",passwordResetScopeAccess.getUserRCN(),equalTo("userRCN"));
    }

    @Test
    public void setAccessTokenExpired_setsDateToCurrentTimeMinusOneDay() throws Exception {
        passwordResetScopeAccess.setAccessTokenExpired();
        assertThat("access token is expired",passwordResetScopeAccess.getAccessTokenExp(),lessThan(new DateTime().minusDays(1).plusSeconds(1).toDate()));
        assertThat("access token is expired",passwordResetScopeAccess.getAccessTokenExp(),greaterThan(new DateTime().minusDays(1).minusSeconds(1).toDate()));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringBlankAndAccessTokenExpIsNull_returnsTrue() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("");
        passwordResetScopeAccess.setAccessTokenExp(null);
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringBlankAndAccessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("");
        passwordResetScopeAccess.setAccessTokenExp(new DateTime().minusMinutes(1).toDate());
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringBlankAndAccessTokenExpIsNotBeforeTime_returnsTrue() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("");
        passwordResetScopeAccess.setAccessTokenExp(new DateTime().plusMinutes(1).toDate());
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringNotBlankAndAccessTokenExpIsNull_returnsTrue() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("accessTokenString");
        passwordResetScopeAccess.setAccessTokenExp(null);
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringNotBlankAndAccessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("accessTokenString");
        passwordResetScopeAccess.setAccessTokenExp(new DateTime().minusMinutes(1).toDate());
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringNotBlankAndAccessTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        passwordResetScopeAccess.setAccessTokenString("accessTokenString");
        passwordResetScopeAccess.setAccessTokenExp(new DateTime().plusMinutes(1).toDate());
        assertThat("returns true",passwordResetScopeAccess.isAccessTokenExpired(new DateTime()),equalTo(false));
    }

    @Test
    public void getAuditContext_returnsAuditContext() throws Exception {
        passwordResetScopeAccess.setUsername("jsmith");
        assertThat("audit string",passwordResetScopeAccess.getAuditContext(),equalTo("PasswordReset(username=jsmith)"));
    }

    @Test
    public void getUserRsId() throws Exception {
        passwordResetScopeAccess.setUserRsId("userRsId");
        assertThat("user rs id",passwordResetScopeAccess.getUserRsId(),equalTo("userRsId"));
    }
}
