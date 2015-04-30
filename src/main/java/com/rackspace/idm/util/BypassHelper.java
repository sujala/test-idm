package com.rackspace.idm.util;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

@Component
public class BypassHelper {

    private static final String PRNG = "SHA1PRNG";
    private static final String HASH_ALG = "PBKDF2WithHmacSHA1";
    private static int KEY_LENGTH = 64 * 8;
    private static int SALT_LENGTH = 16;

    @Autowired
    IdentityConfig identityConfig;

    /**
     * Bypass codes utility methods.
     */
    private static final int BYPASS_DIGITS = 8;
    private static final int BYPASS_MASK = (int) Math.pow(10, BYPASS_DIGITS);

    public BypassDeviceCreationResult createBypassDevice(int numberOfCodes, Integer validSecs) {
        final SecureRandom random = getSecureRandom();

        LinkedHashSet<String> plainTextCodes = generateCodes(random, numberOfCodes);

        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        int iterations = identityConfig.getReloadableConfig().getLocalBypassCodeIterationCount();

        final BypassDevice bypassDevice = new BypassDevice();
        bypassDevice.setUniqueId(null);
        bypassDevice.setId(getRandomUUID());
        bypassDevice.setMultiFactorDevicePinExpiration(getValidSecsDate(validSecs));
        bypassDevice.setSalt(encodeToBase64String(salt));
        bypassDevice.setIterations(iterations);

        LinkedHashSet<String> hashedCodes = encodeCodesForDevice(bypassDevice, plainTextCodes);
        bypassDevice.setBypassCodes(hashedCodes);

        return new BypassDeviceCreationResult(bypassDevice, plainTextCodes);
    }

    public String encodeCodeForDevice(BypassDevice device, String code) {
        LinkedHashSet<String> codes = encodeCodesForDevice(device, Collections.singleton(code));
        return codes.iterator().next();
    }

    private SecureRandom getSecureRandom() {
        try {
            return SecureRandom.getInstance(PRNG);
        } catch (NoSuchAlgorithmException e) {
            throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_OTP_MISSING_SECURE_RANDOM_ALGORITHM, "Missing secure random algorithm", e);
        }
    }

    private LinkedHashSet<String> generateCodes(SecureRandom ran, int numberOfCodes) {
        LinkedHashSet<String> plainTextCodes = new LinkedHashSet<String>(numberOfCodes);

        /*
        using size of set here in the very remote chance the ran generator generates the same code. We
        need unique codes per set.
         */
        while (numberOfCodes > 0 && plainTextCodes.size() < numberOfCodes) {
            final int code = Math.abs(ran.nextInt(BYPASS_MASK));
            plainTextCodes.add(format(code, BYPASS_DIGITS));
        }
        return plainTextCodes;
    }

    private LinkedHashSet<String> encodeCodesForDevice(BypassDevice device, Set<String> plainTextCodes) {
        LinkedHashSet<String> hashedCodes = new LinkedHashSet<String>(plainTextCodes.size());

        //little inefficient to decode here when creating new device, but keeps code cleaner
        byte[] salt = decodeFromBase64(device.getSalt());

        for (String plainTextCode : plainTextCodes) {
            hashedCodes.add(encodeToBase64String(hashBypassCode(plainTextCode, salt, device.getIterations())));
        }

        return hashedCodes;
    }

    private Date getValidSecsDate(Integer validSecs) {
        if (validSecs == null || validSecs == 0) {
            return null;
        } else {
            return new DateTime().plusSeconds(validSecs).toDate();
        }
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private static String format(int truncate, int digits) {
        return String.format("%0" + digits + "d", truncate);
    }

    private byte[] hashBypassCode(String code, byte[] salt, int iterations)
    {
        char[] chars = code.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALG);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_OTP_BYPASS_MISSING_HASH_ALGORITHM, "Missing bypass hash algorithm", e);
        } catch (InvalidKeySpecException e) {
            throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_OTP_BYPASS_ERROR_ENCODING, "Could not generate hash of code", e);
        }
    }

    /**
     * Returns base64 encoded UTF8 formatted string
     * @param toEncode
     * @return
     */
    private String encodeToBase64String(byte[] toEncode) {
        return Base64.encodeBase64String(toEncode);
    }

    private byte[] decodeFromBase64(String toDecode) {
        return Base64.decodeBase64(toDecode);
    }

}
