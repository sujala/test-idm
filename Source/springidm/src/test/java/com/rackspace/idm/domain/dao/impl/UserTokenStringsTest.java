package com.rackspace.idm.domain.dao.impl;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Test;

public class UserTokenStringsTest {

    @Test
    public void shouldPutTokens() {
        UserTokenStrings userTokenStrings = new UserTokenStrings("ownerId");
        userTokenStrings.put("requestorId0", 1, "token_string0");
        userTokenStrings.put("requestorId1", 1, "token_string1");
        userTokenStrings.put("requestorId2", 1, "token_string2");
        Assert.assertEquals(userTokenStrings.get("requestorId1"), "token_string1");
    }

    @Test
    public void shouldRemoveToken() {
        UserTokenStrings userTokenStrings = new UserTokenStrings("ownerId");
        userTokenStrings.put("requestorId0", 1, "token_string0");
        userTokenStrings.put("requestorId1", 1, "token_string1");
        userTokenStrings.put("requestorId2", 1, "token_string2");
        userTokenStrings.remove("requestorId1");
        Assert.assertNull(userTokenStrings.get("requestorId1"));
    }

    @Test
    public void shouldNotGiveNegativeExpirationValue() throws InterruptedException {
        UserTokenStrings userTokenStrings = new UserTokenStrings("ownerId");
        userTokenStrings.put("requestorId", 1, "token_string");
        Thread.sleep(2000);
        Assert.assertEquals(0, userTokenStrings.getExpiration(new DateTime()));
    }
}
