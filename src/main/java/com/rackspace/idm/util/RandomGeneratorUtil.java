package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class to generate secure random digits.
 *
 */
public final class RandomGeneratorUtil {

    private RandomGeneratorUtil() {}

    private static Logger logger = LoggerFactory.getLogger(RandomGeneratorUtil.class);

    /**
     * Util method that generates secure random number for the length specified.
     * Instance of the secure random is based on SHA1PRNG.
     *
     * When failed to generate secure random, method returns null.
     *
     * @param length
     * @return
     */
    public static String generateSecureRandomNumber(int length) {
        String random = null;
        try {
            random = RandomStringUtils.random(length, 0, 0, false,
                    true, null, HashHelper.getSecureRandom());
        } catch (IdmException ex) {
            logger.info("Failed to generate random number: " + ex.getMessage());
            return random;
        }
        return random;
    }
}
