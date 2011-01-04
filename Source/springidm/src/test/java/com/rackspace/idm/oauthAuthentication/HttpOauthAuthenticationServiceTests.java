package com.rackspace.idm.oauthAuthentication;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class HttpOauthAuthenticationServiceTests {
    private HttpOauthAuthenticationService service;
    private OauthTokenService mockOauthTokenService;
    
    private DateTime futureExpiration = new DateTime(3000, 6, 1, 12, 12, 35, 200);
    private DateTime pastExpiration = new DateTime(2000, 6, 1, 12, 12, 35, 200);
    
    private String tokenString = "asdf1234";
    private String authHeader = "OAuth asdf1234";
    private String badAuthHeader = "xOAuth asdf1234";
    private String anotherBadAuthHeader = "sOAuth asdf1234";
    
    private Token token = new Token(tokenString, futureExpiration);
    
    @Before
    public void setUp() throws Exception {

        mockOauthTokenService = EasyMock.createMock(OauthTokenService.class);
        service = new HttpOauthAuthenticationService(mockOauthTokenService);
    }
    
    @Test
    public void ShouldGetTokenFromAuthHeader() {
        String tokenString = service.parseTokenStringFromHeader(authHeader);
        Assert.assertTrue(tokenString.equals("asdf1234"));
    }
    
    @Test
    public void ShouldReturnNullForInvalidAuthHeader() {
        String tokenString = service.parseTokenStringFromHeader(badAuthHeader);     
        Assert.assertNull(tokenString);
        
        tokenString = service.parseTokenStringFromHeader(anotherBadAuthHeader);
        Assert.assertNull(tokenString);
    }
    
    @Test
    public void ShouldReturnOkForGoodToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authHeader);
        
        EasyMock.expect(mockOauthTokenService.getToken(tokenString)).andReturn(token);
        EasyMock.replay(mockOauthTokenService);
        
        AuthenticationResult result = service.authenticate(request);
        
        Assert.assertTrue(result.getHttpStatusCode() == 200);
        
        EasyMock.verify(mockOauthTokenService);
    }
    
    @Test
    public void ShouldReturnBadRequestForMissingAuthHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        AuthenticationResult result = service.authenticate(request);
        
        Assert.assertTrue(result.getHttpStatusCode() == 404);
    }
    
    @Test
    public void ShoulReturnBadRequestForBadHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", badAuthHeader);
        
        AuthenticationResult result = service.authenticate(request);
        
        Assert.assertTrue(result.getHttpStatusCode() == 404);
    }
}
