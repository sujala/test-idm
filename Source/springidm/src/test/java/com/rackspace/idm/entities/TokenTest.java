package com.rackspace.idm.entities;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;

public class TokenTest {

    private String tokenString = "token";
    private DateTime tokenExpiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);

    private AccessToken getTestToken() {
        return new AccessToken(tokenString, tokenExpiration, getTestUser(), getTestClient(),
            IDM_SCOPE.FULL);
    }
    
    private BaseUser getTestUser() {
        BaseUser user = new BaseUser();
        user.setCustomerId("customerId");
        user.setUsername("username");
        return user;
    }
    
    private BaseClient getTestClient() {
        BaseClient client = new BaseClient();
        client.setClientId("clientId");
        client.setCustomerId("customerId");
        return client;
    }
    
    private BaseUser getTestUser2() {
        BaseUser user = new BaseUser();
        user.setCustomerId("customerId");
        user.setUsername("username2");
        return user;
    }
    
    private BaseClient getTestClient2() {
        BaseClient client = new BaseClient();
        client.setClientId("clientId2");
        client.setCustomerId("customerId");
        return client;
    }

    @Test
    public void shouldDetermineExpiration() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            getTestUser(), getTestClient(), IDM_SCOPE.FULL);

        DateTime notExpireTime = new DateTime(2010, 6, 1, 11, 0, 0, 0);
        Assert.assertFalse(token.isExpired(notExpireTime));

        DateTime expiredTime = new DateTime(2010, 6, 1, 13, 0, 0, 0);
        Assert.assertTrue(token.isExpired(expiredTime));
    }

    @Test
    public void shouldThrowErrorForExpirationIfTimeIsNull() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            getTestUser(), getTestClient(), IDM_SCOPE.FULL);
        try {
            token.isExpired(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldDetermineRemainingSeconds() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            getTestUser(), getTestClient(), IDM_SCOPE.FULL);

        DateTime goodCurrentDateTime = new DateTime(2010, 6, 1, 12, 12, 15, 200);
        Assert.assertTrue(token.secondsToExpiration(goodCurrentDateTime) == 20);

        DateTime badCurrentDateTime = new DateTime(2010, 6, 1, 12, 12, 55, 200);
        Assert.assertTrue(token.secondsToExpiration(badCurrentDateTime) == 0);
    }

    @Test
    public void shouldThrowErrorIfCurrentTimeIsNull() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            getTestUser(), getTestClient(), IDM_SCOPE.FULL);
        try {
            token.secondsToExpiration(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldReturnToString() {
        AccessToken token = getTestToken();

        Assert.assertNotNull(token.toString());
    }

    @Test
    public void shouldReturnEquals() {
        AccessToken token1 = getTestToken();
        AccessToken token2 = getTestToken();

        Assert.assertTrue(token1.equals(token1));
        Assert.assertTrue(token1.equals(token2));

        token1.setTokenUser(null);
        token1.setTokenClient(null);
        token1.setExpirationTime(null);
        token1.setTokenString(null);

        token2.setTokenUser(null);
        token2.setTokenClient(null);
        token2.setExpirationTime(null);
        token2.setTokenString(null);

        Assert.assertTrue(token1.equals(token2));
    }

    @Test
    public void shouldReturnNotEquals() {
        AccessToken token1 = getTestToken();
        AccessToken token2 = getTestToken();

        Assert.assertFalse(token1.equals(null));
        Assert.assertFalse(token2.equals(null));

        token2.setTokenUser(getTestUser2());
        Assert.assertFalse(token1.equals(token2));
        token2.setTokenUser(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setTokenUser(token1.getTokenUser());

        token2.setTokenClient(getTestClient2());
        Assert.assertFalse(token1.equals(token2));
        token2.setTokenClient(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setTokenClient(token1.getTokenClient());

        token2.setExpirationTime(new DateTime(2010, 6, 1, 11, 0, 0, 0));
        Assert.assertFalse(token1.equals(token2));
        token2.setExpirationTime(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setExpiration(token1.getExpiration());

        token2.setTokenString("SomeOtherValue");
        Assert.assertFalse(token1.equals(token2));
        token2.setTokenString(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setTokenString(token1.getTokenString());
    }

    @Test
    public void shouldDetectRestrictedScope() {
        AccessToken token = new AccessToken();
        Assert.assertFalse(token.isRestrictedToSetPassword());
        token.setRestrictedToSetPassword();
        Assert.assertTrue(token.isRestrictedToSetPassword());
    }
}
