package com.rackspace.idm.util;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Component
public class BypassHelper {

    private static final String PRNG = "SHA1PRNG";
    private static int TIME_MILLIS = 1000;

    /**
     * Bypass codes utility methods.
     */

    private static final int BYPASS_DIGITS = 8;
    private static final int BYPASS_MASK = (int) Math.pow(10, BYPASS_DIGITS);

    public BypassDevice createBypassDevice(int numberOfCodes, Integer validSecs) {
        final BypassDevice bypassDevice = createBypassDevice(validSecs);
        try {
            final SecureRandom random = SecureRandom.getInstance(PRNG);
            while (numberOfCodes > 0 && bypassDevice.getBypassCodes().size() < numberOfCodes) {
                final int code = Math.abs(random.nextInt(BYPASS_MASK));
                bypassDevice.getBypassCodes().add(format(code, BYPASS_DIGITS));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new ErrorCodeIdmException(ErrorCodes.ERROR_CODE_OTP_MISSING_SECURE_RANDOM_ALGORITHM, "Missing secure random algorithm", e);
        }
        return bypassDevice;
    }

    private BypassDevice createBypassDevice(Integer validSecs) {
        final BypassDevice bypassDevice = new BypassDevice();
        bypassDevice.setUniqueId(null);
        bypassDevice.setId(getRandomUUID());
        bypassDevice.setBypassCodes(new HashSet<String>());
        bypassDevice.setMultiFactorDevicePinExpiration(getValidSecsDate(validSecs));
        return bypassDevice;
    }

    public List<String> calculateBypassCodes(BypassDevice bypassDevice) {
        return calculateBypassCodes(Collections.singleton(bypassDevice));
    }

    public List<String> calculateBypassCodes(Iterable<BypassDevice> bypassDevices) {
        final Set<String> bypassCodes = new HashSet<String>();
        for (BypassDevice bypassDevice : bypassDevices) {
            for (String bypassCode : bypassDevice.getBypassCodes()) {
                bypassCodes.add(bypassCode);
            }
        }
        return new ArrayList<String>(bypassCodes);
    }

    private Date getValidSecsDate(Integer validSecs) {
        if (validSecs == null || validSecs == 0) {
            return null;
        } else {
            return new Date(System.currentTimeMillis() + (validSecs * TIME_MILLIS));
        }
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private static String format(int truncate, int digits) {
        return String.format("%0" + digits + "d", truncate);
    }

}
