package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.util.Assert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HashHelper.class)
public class HashHelperTests {

    @Test
    public void getRandomSha1Test() throws Exception {
        String randomSha1Val = HashHelper.getRandomSha1();
        String randomSha1Val2 = HashHelper.getRandomSha1();

        Assert.isTrue(randomSha1Val.length() > 0);
        Assert.isTrue(randomSha1Val2.length() > 0);
        Assert.isTrue(!randomSha1Val.equals(randomSha1Val2));
    }

    @Test(expected = IdmException.class)
    public void getRandomSha1_secureRandom_getInstance_ofSHA1PRNG_throwsIdmException() throws Exception {
        PowerMockito.mockStatic(SecureRandom.class);
        when(SecureRandom.getInstance("SHA1PRNG")).thenThrow(new NoSuchAlgorithmException());
        HashHelper.getRandomSha1();
    }

    @Test(expected = IdmException.class)
    public void makeSHA1Hash_MessageDigest_getInstance_ofSHA1_throwsIdmException() throws Exception {
        PowerMockito.mockStatic(MessageDigest.class);
        when(MessageDigest.getInstance("SHA1")).thenThrow(new NoSuchAlgorithmException());
        HashHelper.makeSHA1Hash("blah");
    }
}
