package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

public final class HashHelper {

    public static final int SALT_SIZE = 8;
    public static final int MASK = 0xff;
    public static final int BIT256 = 0x100;
    public static final int BASE = 16;

    private HashHelper() {}

    private static Logger logger = LoggerFactory.getLogger(HashHelper.class);

    public static String getRandomSha1() {
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            logger.info("failed to Secure Random based on SHA1PRNG: " + e.getMessage());
            throw new IdmException("failed to create Secure Random based on SHA1PRNG", e);
        }

        // Salt generation 64 bits long
        byte[] bSalt = new byte[SALT_SIZE];
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

    public static String makeSHA1Hash(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            logger.info("failed to create SHA1 message digest: " + e.getMessage());
            throw new IdmException("failed to create SHA1 message digest", e);
        }
        md.reset();
        byte[] buffer = input.getBytes();
        md.update(buffer);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr += Integer.toString((digest[i] & MASK) + BIT256, BASE)
                .substring(1);
        }
        return hexStr;
    }
}
