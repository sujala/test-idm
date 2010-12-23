package controllers;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import library.SecurityHelper;
import models.AuthKey;
import play.data.validation.Required;
import play.mvc.*;

public class TestClient extends Controller {

    public static void index()
    {
        render();
    }

    public static void generateApiKeys()
    {
        List<AuthKey> authkeys = AuthKey.findAll();
        render(authkeys);
    }

    public static void generateApiKeysSubmitted(String user)
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Security.generateKeyPair(user);
        generateApiKeys();
    }

    public static void HashSha1Base64FromBytes(@Required String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String hashedval = SecurityHelper.HashSha1Base64FromBytes(input);
        render(hashedval);
    }
}