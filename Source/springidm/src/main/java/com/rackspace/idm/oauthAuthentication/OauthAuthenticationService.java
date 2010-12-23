package com.rackspace.idm.oauthAuthentication;

import org.joda.time.DateTime;

public class OauthAuthenticationService {

    private static final String ERROR_INVALID_TOKEN = "error=invalid-token";
    private static final String ERROR_TOKEN_EXPIRED = "error=token-expired";
    private static final int HTTP_UNAUTHORIZED_STATUS_CODE = 401;
    private static final int HTTP_OK_STATUS_CODE = 200;

    private OauthTokenService oauthTokenService;

    public OauthAuthenticationService(OauthTokenService oauthTokenService) {
        this.oauthTokenService = oauthTokenService;
    }

    public AuthenticationResult authenticateToken(String tokenString) {
        AuthenticationResult result = new AuthenticationResult();

        Token token = oauthTokenService.getToken(tokenString);

        DateTime current = new DateTime();

        if (token == null) {
            result.setHttpStatusCode(HTTP_UNAUTHORIZED_STATUS_CODE);
            result.setMessage(ERROR_INVALID_TOKEN);
        } else if (token.isExpired(current)) {
            result.setHttpStatusCode(HTTP_UNAUTHORIZED_STATUS_CODE);
            result.setMessage(ERROR_TOKEN_EXPIRED);
        } else {
            result.setHttpStatusCode(HTTP_OK_STATUS_CODE);
        }

        return result;
    }
}
