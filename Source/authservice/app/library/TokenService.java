package library;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.security.sasl.AuthenticationException;
import models.AccessToken;
import models.Token;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;

public class TokenService {

    private HttpClientService httpService;

    private final String CONSUMER_NAME = "cloudcp";
    private final String OAUTH_CLIENT_ID = "10eeaa768d39579eca73b2a21fb721b2453d6795";
    private final String OAUTH_CLIENT_SECRET = "dd0725319e2b635a37f895ec6823a9fddda120bf";

    private final String OAUTH_PROVIDER_NAME = "provider_name";
    private final String OAUTH_CONSUMER_KEY_NAME = "oauth_consumer_key";
    private final String OAUTH_TOKEN_NAME = "oauth_token";
    private final String OAUTH_METHOD_NAME = "oauth_signature_method";
    private final String OAUTH_SIGNATURE_NAME = "oauth_signature";
    private final String OAUTH_TIMESTAMP_NAME = "oauth_timestamp";
    private final String OAUTH_NONCE_NAME = "oauth_nonce";
    private final String OAUTH_VERSION_NAME = "oauth_version";

    public TokenService() {}

    public Token GetToken(String hashedval, String username)
    {
        Token currentToken = Token.find(
                            "value =? And LOWER(relatedEntityId) =?", hashedval,
                            username.toLowerCase()).first();
        
        return currentToken;
    }

    public Boolean ValidateToken(String hashval, String username)
    {
        Token token = this.GetToken(hashval, username);

        if (token != null) {
            Date now = new Date();
            long millsecondsdiff = now.getTime() - token.lastModified.getTime();

            // in minutes
            long secondsdiff = millsecondsdiff / 1000;

            if (SecurityHelper.EXPIRE_SECONDS >= secondsdiff) {
                token.lastModified = new Date();
                token.save();
                return true;
            }
        }
        return false;
    }

    public AccessToken GetAccessToken(String username, String password)
            throws Exception
    {
        String url = "http://localhost:8080/idm/access_token";

        NameValuePair[] data = {
          new NameValuePair("type", "username"),
          new NameValuePair("client_id", OAUTH_CLIENT_ID),
          new NameValuePair("client_secret", OAUTH_CLIENT_SECRET),
          new NameValuePair("username", username),
          new NameValuePair("password", password)
        };

        String response = getHttpService().sendPost(url, data);
        AccessToken accessToken = parseAccessTokenResponse(response, username);

        return accessToken;
    }

    public void authenticateAccessToken(String accessToken)
            throws AuthenticationException, HttpException
    {
        try {
            String url = "http://localhost:8080/springidm/access_token/client_cred";

            NameValuePair[] data = {
              new NameValuePair("type", "client_cred"),
              new NameValuePair("client_id", OAUTH_CLIENT_ID),
              new NameValuePair("client_secret", OAUTH_CLIENT_SECRET),
              new NameValuePair("oauth_token", accessToken),
            };

            String response = getHttpService().sendPost(url, data);
        }
        catch(HttpException ex)
        {
            throw new AuthenticationException();
        }
    }

    public AccessToken GetOAuthAccessToken(String username, String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {   
        String requestToken = SecurityHelper.MakeSHA1Hash(username);
        String oauthMethod = "HMAC-SHA1";

        Date now = new Date();
        String timestamp = String.valueOf(now.getTime());
        String oauthNonce = getNonce(requestToken, timestamp);

        String resourceUrl = "http://localhost:8080/springidm/access_token";

        StringBuilder sb = new StringBuilder();
            sb.append(OAUTH_PROVIDER_NAME + "=cloudcp");
            sb.append(getOauthParam(OAUTH_CONSUMER_KEY_NAME, OAUTH_CLIENT_ID));
            sb.append(getOauthParam(OAUTH_TOKEN_NAME, requestToken));
            sb.append(getOauthParam(OAUTH_NONCE_NAME, oauthNonce));
            sb.append(getOauthParam(OAUTH_TIMESTAMP_NAME, timestamp));
            sb.append(getOauthParam(OAUTH_METHOD_NAME, oauthMethod));
            sb.append(getOauthParam(OAUTH_VERSION_NAME, "1.0"));
            sb.append(getOauthParam("username", username));
            sb.append(getOauthParam("password", password));

        String parameterString = sb.toString();
        String signature = getSignature("GET", resourceUrl, parameterString, OAUTH_CLIENT_SECRET);
        String url = String.format("%s?%s%s", 
                resourceUrl,
                parameterString,
                getOauthParam(OAUTH_SIGNATURE_NAME, signature));

        String jsonResponse = getHttpService().sendGet(url);
        Gson gson = new Gson();

        AccessToken accessToken = gson.fromJson(jsonResponse, AccessToken.class);
        accessToken.save();
        
        return accessToken;
    }

    protected HttpClientService getHttpService()
    {
        if(this.httpService == null) {
            this.httpService = new HttpClientService();
        }
        return this.httpService;
    }

    private String getOauthParam(String oauthParamName, String oauthParamValue)
    {
        return String.format("&%s=%s",
                oauthParamName,
                URLEncoder.encode(oauthParamValue));
    }

    private String getSignature(String method, String url, String parameterstring, String key)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();

        byte[] bsalt = digest.digest(key.getBytes("UTF-8"));
        digest.update(bsalt);

        String signatureBase = String.format("%s&%s&%s",
                method,
                URLEncoder.encode(url),
                parameterstring);

        byte[] input = digest.digest(signatureBase.getBytes("UTF-8"));

        String oauthSignature = SecurityHelper.ByteToBase64(input);
        
        return oauthSignature;
    }

    private String getNonce(String token, String timestamp)
            throws NoSuchAlgorithmException
    {
        String nonceBaseStr = String.format("%s:%s", token, timestamp);
        return SecurityHelper.MakeSHA1Hash(nonceBaseStr);
    }

    private AccessToken parseAccessTokenResponse(String response, String username)
    {
        String[] responseElems = response.split("&");
        String token = responseElems[0].split("=")[1].trim();
        String refreshToken = responseElems[1].split("=")[1].trim();
        String expireIn = responseElems[2].split("=")[1].trim();

        AccessToken accessToken = new AccessToken(token, username, "",
                refreshToken, Integer.parseInt(expireIn));

        return accessToken;
    }
}
