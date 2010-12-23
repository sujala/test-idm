package controllers;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import library.SecurityHelper;
import play.mvc.*;
import models.AuthKey;

public class Security extends Controller {

    public static final int API_KEY_SIZE = 15;
    public static final int SECRET_KEY_SIZE = 30;
    public static final String HEADER_KEY_USERAGENT = "x-user-agent";
    public static final String HEADER_KEY_SIGNATURE = "x-api-signature";

    @Before(unless = {"generateKeyPair", "HashSha1Base64FromBytes", "Accounts.list", "Users.list"})
    static void headerAuthentification()
        throws NoSuchAlgorithmException, UnsupportedEncodingException {

        Boolean authenticated = false;

        // get headers
        Map<String, Http.Header> headers = request.headers;
        if (!ValidateHeaders(headers)) {
            error(401, "Missing Authentication Headers");
        }

        String userAgent = headers.get(HEADER_KEY_USERAGENT).value();
        String signatureValues = headers.get(HEADER_KEY_SIGNATURE).value();

        if (signatureValues.length() > 0) {

            String[] values = signatureValues.split(":");
            if (values.length != 3) {
                error(401, "Malformed API Key");
            }

            String apiId = values[0];
            String timestamp = values[1];
            String messageSignature = values[2];

            AuthKey authKey = AuthKey.find("apiKey = ?", apiId).first();
            if (authKey == null) {
                error(401, "Invalid API Key");
            }

            String generatedSignature = SecurityHelper.GenerateSignature(apiId, userAgent, timestamp, authKey.secretKey);

            if (messageSignature.compareTo(generatedSignature) == 0) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            error(401, "Invalid API Key or Signagure");
        }
    }

    public static HashMap generateKeyPair(String user)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        // generate keys
        byte[] bApiKey = new byte[API_KEY_SIZE];
        random.nextBytes(bApiKey);
        String apiKey = SecurityHelper.ByteToBase64(bApiKey);

        byte[] bSecretKey = new byte[SECRET_KEY_SIZE];
        random.nextBytes(bSecretKey);
        String secretKey = SecurityHelper.ByteToBase64(bSecretKey);

        // save keys
        AuthKey keys = new AuthKey(user, apiKey, secretKey);
        keys.save();

        // return keys
        HashMap retval = new HashMap();
        retval.put("apiKey", apiKey);
        retval.put("secretKey", secretKey);

        return retval;
    }

    public static Boolean ValidateHeaders(Map headers)
    {
        if (headers.containsKey(HEADER_KEY_SIGNATURE) &&
                headers.containsKey(HEADER_KEY_USERAGENT)) {
            return true;
        }
        return false;
    }
}