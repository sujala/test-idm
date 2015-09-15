package com.rackspace.idm.domain.security;

import org.apache.commons.lang.Validate;
import org.keyczar.exceptions.Base64DecodingException;
import org.keyczar.util.Base64Coder;

public class WebSafeTokenCoder {
    public static final String ERROR_CODE_UNMARSHALL_INVALID_BASE64 = "AEU-0001";

    public static final byte[] decodeTokenFromWebSafe(String webSafeToken) {
        Validate.notEmpty(webSafeToken);

        byte[] userTokenBytes;
        try {
            userTokenBytes = Base64Coder.decodeWebSafe(webSafeToken);
        } catch (Base64DecodingException e) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_BASE64, "Error encountered decoding web token", e);
        }

        return userTokenBytes;
    }

    public static final String encodeTokenToWebSafe(byte[] userTokenBytes) {
        return Base64Coder.encodeWebSafe(userTokenBytes);
    }
}
