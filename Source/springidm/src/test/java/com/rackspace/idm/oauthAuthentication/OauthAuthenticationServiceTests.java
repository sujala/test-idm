package com.rackspace.idm.oauthAuthentication;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

public class OauthAuthenticationServiceTests {
    private OauthAuthenticationService service;
    private OauthTokenService mockOauthTokenService;

    private DateTime futureExpiration = new DateTime(3000, 6, 1, 12, 12, 35,
            200);
    private DateTime pastExpiration = new DateTime(2000, 6, 1, 12, 12, 35, 200);

    private String tokenString = "asdf1234";
    private String authHeader = "Token token=\"asdf1234\"";
    private String badAuthHeader = "SToken token=\"asdf1234\"";
    private String anotherBadAuthHeader = "Token token=\"asdf1234\" x";

    private Token token = new Token(tokenString, futureExpiration);

    @Before
    public void setUp() {

        mockOauthTokenService = EasyMock.createMock(OauthTokenService.class);
        service = new OauthAuthenticationService(mockOauthTokenService);
    }

    @Test
    public void ShouldReturnOkForGoodToken() {
        EasyMock.expect(mockOauthTokenService.getToken(tokenString)).andReturn(
                token);
        EasyMock.replay(mockOauthTokenService);

        AuthenticationResult result = service.authenticateToken(tokenString);

        Assert.assertTrue(result.getHttpStatusCode() == 200);

        EasyMock.verify(mockOauthTokenService);
    }

    @Test
    public void ShouldReturnUnAuthorizedWithExpiredToken() {

        token.setExpirationTime(pastExpiration);

        EasyMock.expect(mockOauthTokenService.getToken(tokenString)).andReturn(
                token);
        EasyMock.replay(mockOauthTokenService);

        AuthenticationResult result = service.authenticateToken(tokenString);

        Assert.assertTrue(result.getHttpStatusCode() == 401);
        Assert.assertTrue(result.getMessage().equals("error=token-expired"));

        EasyMock.verify(mockOauthTokenService);
    }

    @Test
    public void ShouldReturnUnAuthorizedWithTokenNotFound() {

        EasyMock.expect(mockOauthTokenService.getToken(tokenString)).andReturn(
                null);
        EasyMock.replay(mockOauthTokenService);

        AuthenticationResult result = service.authenticateToken(tokenString);

        Assert.assertTrue(result.getHttpStatusCode() == 401);
        Assert.assertTrue(result.getMessage().equals("error=invalid-token"));

        EasyMock.verify(mockOauthTokenService);
    }
}
