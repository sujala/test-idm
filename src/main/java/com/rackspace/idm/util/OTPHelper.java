package com.rackspace.idm.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import com.unboundid.util.Base32;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class OTPHelper {

    private static final String PRNG = "SHA1PRNG";
    private static final int KEY_SIZE = 20;

    private static final int IMAGE_SIZE = 640;
    private static final String IMAGE_FORMAT = "png";

    @Autowired
    IdentityConfig config;

    public OTPDevice createOTPDevice(String name) {
        final OTPDevice otpDevice = new OTPDevice();
        otpDevice.setUniqueId(null);
        otpDevice.setKey(getRandomKey());
        otpDevice.setId(getRandomUUID());
        otpDevice.setMultiFactorDeviceVerified(false);
        otpDevice.setName(name);
        return otpDevice;
    }

    public String fromStringToQRCode(String data) {
        try {
            final BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE);
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, IMAGE_FORMAT, bytes);

            final URI uri = new URI("data", "image/png;base64," + Base64.encodeBase64String(bytes.toByteArray()), null);
            return uri.toASCIIString();
        } catch (Exception e) {
            throw new ErrorCodeIdmException("OTP-001", "Error creating QR code", e);
        }
    }

    public String fromKeyToURI(byte[] key, String name) {
        try {
            final String keyUri = Base32.encode(key);
            final String issuer = config.getStaticConfig().getOTPIssuer();
            final URI uri = new URI("otpauth", "//totp/" + issuer + ":" + name + "?secret=" + keyUri + "&issuer=" + issuer, null);
            return uri.toASCIIString();
        } catch (Exception e) {
            throw new ErrorCodeIdmException("OTP-002", "Error encoding URI", e);
        }
    }

    private byte[] getRandomKey() {
        try {
            final SecureRandom random = SecureRandom.getInstance(PRNG);
            final byte[] key = new byte[KEY_SIZE];
            random.nextBytes(key);
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new ErrorCodeIdmException("OTP-003", "Missing secure random algorithm", e);
        }
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * OATH HOTP + TOTP Implementation in Java.
     * Based on http://tools.ietf.org/html/rfc4226
     *
     * Parameter and function names kept inline with the RFC,
     * but K renamed to 'key' and C renamed to 'counter'.
     * (e.g. HOTP, Truncate, etc)
     */

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static int MIN_DIGITS = 6;
    private static int DATA_LENGTH = 8;
    private static int DATA_CONVERT_LOOP = DATA_LENGTH - 1;
    private static int TIME_MILLIS = 1000;
    private static int DEFAULT_WINDOW = 30;

    private static int[] POSSIBLE_DIGITS = new int[] {
            (int) Math.pow(10, MIN_DIGITS),
            (int) Math.pow(10, MIN_DIGITS + 1),
            (int) Math.pow(10, MIN_DIGITS + 2)
    };

    private static int truncate(final byte[] hmac_result) {
        final int offset = hmac_result[19] & 0xf;
        return (hmac_result[offset] & 0x7f) << 24
                | (hmac_result[offset + 1] & 0xff) << 16
                | (hmac_result[offset + 2] & 0xff) << 8
                | (hmac_result[offset + 3] & 0xff);
    }

    private static String OTP(final byte[] hmac_result, final int digits) {
        final int idx = digits - MIN_DIGITS;
        if (idx < 0 || idx > POSSIBLE_DIGITS.length) {
            throw new ErrorCodeIdmException("OTP-004", "Invalid number of digits: " + digits);
        }
        return String.format("%0" + digits + "d", truncate(hmac_result) % POSSIBLE_DIGITS[idx]);
    }

    private static byte[] fromCounterToByte(final long counter) {
        long data = counter;
        byte[] bytes = new byte[DATA_LENGTH];
        for (int i = DATA_CONVERT_LOOP; i > -1; i--) {
            bytes[i] = (byte) (data & 0xff);
            data >>= 8;
        }
        return bytes;
    }

    public static String HOTP(final byte[] key, final int counter) {
        return HOTP(key, counter, MIN_DIGITS);
    }

    public static String HOTP(final byte[] key, final long counter, final int digits) {
        try {
            final SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
            final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            return OTP(mac.doFinal(fromCounterToByte(counter)), digits);
        } catch (Exception e) {
            throw new ErrorCodeIdmException("OTP-005", "Encryption error", e);
        }
    }

    public static String TOTP(final byte[] key) {
        return TOTP(key, MIN_DIGITS);
    }

    public static String TOTP(final byte[] key, final int digits) {
        return TOTP(key, digits, DEFAULT_WINDOW);
    }

    public static String TOTP(final byte[] key, final int digits, final int window) {
        return TOTP(key, digits, window, 0).iterator().next();
    }

    public static Set<String> TOTP(final byte[] key, final int digits, final int window, final int entropy) {
        final Set<Integer> counters = new HashSet<Integer>();
        final long seconds = (System.currentTimeMillis() / TIME_MILLIS);
        for (long sec=(seconds-entropy); sec<=(seconds+entropy); sec+=window) {
            counters.add((int) (sec / window) - 1);
        }
        final Set<String> codes = new HashSet<String>();
        for (int counter : counters) {
            codes.add(HOTP(key, counter, digits));
        }
        return codes;
    }

    public boolean checkTOTP(final byte[] key, final int digits, final int window, final int entropy, final String passcode) {
        return TOTP(key, digits, window, entropy).contains(passcode);
    }

    public boolean checkTOTP(final byte[] key, String passcode) {
        return checkTOTP(key, MIN_DIGITS, DEFAULT_WINDOW, config.getReloadableConfig().getOTPEntropy(), passcode);
    }

}
