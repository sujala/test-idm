package com.rackspace.idm.domain.entity;

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
    public void getAuditContext_returnsAuditString() throws Exception {
        rackerScopeAccess.setRackerId("rackerId");
        String result = rackerScopeAccess.getAuditContext();
        assertThat("audit string", result, equalTo("Racker(rackerId=rackerId)"));
    }
}
