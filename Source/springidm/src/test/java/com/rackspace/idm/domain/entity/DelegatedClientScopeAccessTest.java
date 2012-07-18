package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/18/12
 * Time: 12:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelegatedClientScopeAccessTest {
    DelegatedClientScopeAccess delegatedClientScopeAccess;

    @Before
    public void setUp() throws Exception {
        delegatedClientScopeAccess = new DelegatedClientScopeAccess();
    }

    @Test
    public void setRefreshTokenExpired_setsRefreshTokenExpire() throws Exception {
        delegatedClientScopeAccess.setRefreshTokenExpired();
        Date result = delegatedClientScopeAccess.getRefreshTokenExp();
        assertThat("date", result.before(new DateTime().toDate()), equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenStringIsBlank_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setAccessTokenString("");
        boolean result = delegatedClientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNull_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setAccessTokenString("notBlank");
        delegatedClientScopeAccess.setAccessTokenExp(null);
        boolean result = delegatedClientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsBeforeTime_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setAccessTokenString("notBlank");
        delegatedClientScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = delegatedClientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAccessTokenExpired_accessTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        delegatedClientScopeAccess.setAccessTokenString("notBlank");
        delegatedClientScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = delegatedClientScopeAccess.isAccessTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }


    @Test
    public void isRefreshTokenExpired_RefreshTokenStringIsBlank_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setRefreshTokenString("");
        boolean result = delegatedClientScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsNull_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setRefreshTokenString("notBlank");
        delegatedClientScopeAccess.setRefreshTokenExp(null);
        boolean result = delegatedClientScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsBeforeTime_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setRefreshTokenString("notBlank");
        delegatedClientScopeAccess.setRefreshTokenExp(new DateTime().minusDays(1).toDate());
        boolean result = delegatedClientScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isRefreshTokenExpired_RefreshTokenExpIsNotBeforeTime_returnsFalse() throws Exception {
        delegatedClientScopeAccess.setRefreshTokenString("notBlank");
        delegatedClientScopeAccess.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        boolean result = delegatedClientScopeAccess.isRefreshTokenExpired(new DateTime());
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void isAuthorizationCodeExpired_authCodeIsBlank_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setAuthCode("");
        boolean result = delegatedClientScopeAccess.isAuthorizationCodeExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isAuthorizationCodeExpired_authCodeExpIsNull_returnsTrue() throws Exception {
        delegatedClientScopeAccess.setAuthCode("notBlank");
        delegatedClientScopeAccess.setAuthCodeExp(null);
        boolean result = delegatedClientScopeAccess.isAuthorizationCodeExpired(new DateTime());
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void getUserRsId_returnsUserRsId() throws Exception {
        delegatedClientScopeAccess.setUserRsId("userRsId");
        String result = delegatedClientScopeAccess.getUserRsId();
        assertThat("user rs id", result, equalTo("userRsId"));
    }
}
