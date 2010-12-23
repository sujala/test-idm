package com.rackspace.idm.oauthAuthentication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class HttpOauthAuthenticationService extends OauthAuthenticationService {

    private static final String TOKEN_REGEX_PATTERN = "^oauth (.+)$";
    private static final Pattern REGEX_PATTERN = Pattern.compile(TOKEN_REGEX_PATTERN, Pattern.CASE_INSENSITIVE);
    private static final int BAD_REQUEST = 404;

    public HttpOauthAuthenticationService(OauthTokenService oauthTokenService) {
        super(oauthTokenService);
    }

    public AuthenticationResult authenticate(HttpServletRequest request) {
        AuthenticationResult result = new AuthenticationResult();

        result.setHttpStatusCode(BAD_REQUEST);
        result.setMessage("Missing Authorization Header");

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {

            String tokenString = parseTokenStringFromHeader(authHeader.trim());

            if (tokenString == null) {
                result.setMessage("Invalid Authorization Header");
                
            } else {
                result = authenticateToken(tokenString);
            }
        }

        return result;
    }

    public String parseTokenStringFromHeader(String authHeader) {
        String tokenString = null;

        Matcher m = REGEX_PATTERN.matcher(authHeader);

        if (m.matches()) {
            tokenString = m.group(1);
            tokenString = tokenString.replace("\"", "");
        }
        return tokenString;
    }
}
