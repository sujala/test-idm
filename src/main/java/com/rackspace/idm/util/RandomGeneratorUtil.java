package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Util method that generates secure random digit required to be used for Phone pin generation,
     * which should not be sequential or repeated 3 times consecutively. Method take arguments of
     * 2 numbers used before the number which is expected to be generated in the phone pin number series.
     *
     * When failed to generate non sequential or repeated number, method returns a secure random number.
     *
     * @param phonePinDigit1
     * @param phonePinDigit2
     * @return String
     */
    public static String getNextPhonePinDigit(char phonePinDigit1, char phonePinDigit2) {

        String nextPhonePinDigit;
        try {
            Integer digit1 = Integer.parseInt(phonePinDigit1+"");
            Integer digit2 = Integer.parseInt(phonePinDigit2+"");

            List<String> usableNumberPool = new ArrayList();
            usableNumberPool.add("0");
            usableNumberPool.add("1");
            usableNumberPool.add("2");
            usableNumberPool.add("3");
            usableNumberPool.add("4");
            usableNumberPool.add("5");
            usableNumberPool.add("6");
            usableNumberPool.add("7");
            usableNumberPool.add("8");
            usableNumberPool.add("9");

            // if last 2 digits are repetitive then remove it from usable number pool
            // to avoid the potential of next digit getting the same repetitive number
            // eg. 56944?  => remove number 4 to be repeat itself in next place,
            // once its already repeated twice in last 2 digits.
            if (digit1 == digit2) {
                usableNumberPool.remove(digit2.toString());
            }

            // if last 2 digits are sequential then remove the 3rd number in sequence
            // from usable number pool to avoid the potential of getting 3rd number in series
            // eg. 56945?  => remove number 6 to avoid it being sequential
            if (digit2 - digit1 == 1) {
                digit2++;
                usableNumberPool.remove(digit2.toString());
            }

            // same as above but in descending order ;-)
            // eg. 56398?  => remove number 7 to avoid it being sequential
            if (digit2 - digit1 == -1) {
                digit2--;
                usableNumberPool.remove(digit2.toString());
            }

            Integer index = HashHelper.getSecureRandom().nextInt(usableNumberPool.size());

            nextPhonePinDigit = usableNumberPool.get(index);

        } catch (IdmException ex) {
            logger.info("Failed to generate random number for phone phone digit: " + ex.getMessage());
            return generateSecureRandomNumber(1);
        }
        return nextPhonePinDigit;
    }

}
