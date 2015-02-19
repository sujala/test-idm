package com.rackspace.idm.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.unboundid.util.Base32;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

@Component
public class OTPHelper {

    private static final String PRNG = "SHA1PRNG";
    private static final int KEY_SIZE = 32;

    private static final int IMAGE_SIZE = 640;
    private static final String IMAGE_FORMAT = "png";

    private static final String ISSUER = "Rackspace";

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
            throw new RuntimeException("Error creating QR code", e);
        }
    }

    public String fromKeyToURI(byte[] key, String name) {
        try {
            final String keyUri = Base32.encode(key).replaceAll("=", "");
            final URI uri = new URI("otpauth", "//totp/" + ISSUER + ":" + name + "?secret=" + keyUri + "&issuer=" + ISSUER, null);
            return uri.toASCIIString();
        } catch (Exception e) {
            throw new RuntimeException("Error encoding URI", e);
        }
    }

    private byte[] getRandomKey() {
        try {
            final SecureRandom random = SecureRandom.getInstance(PRNG);
            final byte[] key = new byte[KEY_SIZE];
            random.nextBytes(key);
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing secure random algorithm", e);
        }
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
