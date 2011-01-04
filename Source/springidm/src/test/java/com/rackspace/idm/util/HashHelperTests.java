package com.rackspace.idm.util;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.springframework.util.Assert;

public class HashHelperTests {

    @Test
    public void getRandomSha1Test() throws NoSuchAlgorithmException {
        String randomSha1Val = HashHelper.getRandomSha1();
        String randomSha1Val2 = HashHelper.getRandomSha1();

        Assert.isTrue(randomSha1Val.length() > 0);
        Assert.isTrue(randomSha1Val2.length() > 0);
        Assert.isTrue(!randomSha1Val.equals(randomSha1Val2));
    }
}
