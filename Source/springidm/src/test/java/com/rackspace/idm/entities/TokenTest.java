package com.rackspace.idm.entities;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;

public class TokenTest {

    private String tokenString = "token";
    private DateTime tokenExpiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
    private String owner = "Owner";
    private String requestor = "Requestor";
    private boolean isTrusted = false;

    private AccessToken getTestToken() {
        return new AccessToken(tokenString, tokenExpiration, owner, requestor,
            IDM_SCOPE.FULL);
    }

    @Test
    public void shouldDetermineExpiration() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            "fake_owner", "requestor", IDM_SCOPE.FULL);

        DateTime notExpireTime = new DateTime(2010, 6, 1, 11, 0, 0, 0);
        Assert.assertFalse(token.isExpired(notExpireTime));

        DateTime expiredTime = new DateTime(2010, 6, 1, 13, 0, 0, 0);
        Assert.assertTrue(token.isExpired(expiredTime));
    }

    @Test
    public void shouldThrowErrorForExpirationIfTimeIsNull() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            "fake_owner", "requestor", IDM_SCOPE.FULL);
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
            "fake_owner", "requestor", IDM_SCOPE.FULL);

        DateTime goodCurrentDateTime = new DateTime(2010, 6, 1, 12, 12, 15, 200);
        Assert.assertTrue(token.secondsToExpiration(goodCurrentDateTime) == 20);

        DateTime badCurrentDateTime = new DateTime(2010, 6, 1, 12, 12, 55, 200);
        Assert.assertTrue(token.secondsToExpiration(badCurrentDateTime) == 0);
    }

    @Test
    public void shouldThrowErrorIfCurrentTimeIsNull() {
        DateTime expiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
        AccessToken token = new AccessToken("fake_token", expiration,
            "fake_owner", "requestor", IDM_SCOPE.FULL);
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
        String expectedString = String
            .format(
                "Token [token=%s, tokenExpiration=%s, owner=%s, requestor=%s, idmScope=%s, isTrusted=%s]",
                tokenString, tokenExpiration, owner, requestor, IDM_SCOPE.FULL, isTrusted);

        Assert.assertEquals(expectedString, token.toString());

    }

    @Test
    public void shouldReturnEquals() {
        AccessToken token1 = getTestToken();
        AccessToken token2 = getTestToken();

        Assert.assertTrue(token1.equals(token1));
        Assert.assertTrue(token1.equals(token2));

        token1.setOwner(null);
        token1.setRequestor(null);
        token1.setExpirationTime(null);
        token1.setTokenString(null);

        token2.setOwner(null);
        token2.setRequestor(null);
        token2.setExpirationTime(null);
        token2.setTokenString(null);

        Assert.assertTrue(token1.equals(token2));
    }

    @Test
    public void shouldReturnNotEquals() {
        AccessToken token1 = getTestToken();
        AccessToken token2 = getTestToken();

        Assert.assertFalse(token1.equals(null));
        Assert.assertFalse(token1.equals(1));

        token2.setOwner("SomeOtherValue");
        Assert.assertFalse(token1.equals(token2));
        token2.setOwner(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setOwner(token1.getOwner());

        token2.setRequestor("SomeOtherValue");
        Assert.assertFalse(token1.equals(token2));
        token2.setRequestor(null);
        Assert.assertFalse(token2.equals(token1));
        token2.setRequestor(token1.getRequestor());

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

    @Test
    public void shouldDetectRackerToken() {
        // TODO
//        AccessToken tk = getTestToken();
//        tk.setOwner("foo@RacKsPace.CoM");
//        Assert.assertTrue(tk.isRackerToken());
    }

    @Test
    public void shouldDetectNonRackerToken() {
        AccessToken tk = getTestToken();
        tk.setOwner("bar.foo@rackspace.com.morestuff.eu");
        Assert.assertFalse(tk.getIsTrusted());
    }
}
