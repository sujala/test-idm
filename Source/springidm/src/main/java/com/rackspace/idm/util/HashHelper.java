package com.rackspace.idm.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

public class HashHelper {

    private HashHelper() {}

    private static Logger logger = LoggerFactory.getLogger(HashHelper.class);

    public static String getRandomSha1() throws NoSuchAlgorithmException {
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            logger.info("failed to Secure Random based on SHA1PRNG: " + e.getMessage());
        }

        // Salt generation 64 bits long
        byte[] bSalt = new byte[8];
        random.nextBytes(bSalt);
        String sSalt = byteToBase64(bSalt);

        Date now = new Date();
        String timestamp = String.valueOf(now.getTime());

        String randomSha1 = makeSHA1Hash(sSalt + timestamp);

        return randomSha1;
    }

    public static String byteToBase64(byte[] data) {
        return Base64.encodeBase64(data).toString();
    }

    public static String makeSHA1Hash(String input)
        throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        byte[] buffer = input.getBytes();
        md.update(buffer);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16)
                .substring(1);
        }
        return hexStr;
    }
}
