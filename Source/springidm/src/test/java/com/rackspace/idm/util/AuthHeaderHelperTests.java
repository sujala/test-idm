package com.rackspace.idm.util;

import java.util.Map;

import org.junit.Test;
import org.springframework.util.Assert;

import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.unboundid.util.Base64;

public class AuthHeaderHelperTests {
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private String clientId = "SomeId";
    private String clientSecret = "SomeSecret";
    private String authHeader = "OAuth XXXX";
    private String badAuthHeader = "sOAuth XXXX";
    private String blankTokenHeader = "OAuth";
    
    @Test
    public void shouldReturnAuthParams() {
        String authHeader = "Basic " + Base64.encode(clientId + ":" + clientSecret);
        
        Map<String, String> params = authHeaderHelper.parseBasicParams(authHeader);
        
        Assert.isTrue(params.size() == 2);
        Assert.isTrue(params.containsKey("username"));
        Assert.isTrue(params.containsKey("password"));
        Assert.isTrue(params.get("username").equals(clientId));
        Assert.isTrue(params.get("password").equals(clientSecret));
    }
    
    @Test
    public void shouldReturnTokenParams() {
        
        Map<String, String> params = authHeaderHelper.parseTokenParams(authHeader);
        
        Assert.isTrue(params.size() == 1);
        Assert.isTrue(params.containsKey("token"));
        Assert.isTrue(params.get("token").equals("XXXX"));
    }
    
    @Test
    public void shouldReturnTokenString() {
        
        String tokenString = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        
        Assert.isTrue(tokenString.equals("XXXX"));
    }
    
    @Test(expected = NotAuthorizedException.class)
    public void ShouldThrowErrorForInvalidAuthHeaderWithNullAuthHeader() {
        authHeaderHelper.getTokenFromAuthHeader(null);
    }
    
    @Test(expected = NotAuthorizedException.class)
    public void ShouldThrowErrorForInvalidAuthHeaderWithBadHeader() {
        authHeaderHelper.getTokenFromAuthHeader(badAuthHeader);
    }
    
    @Test(expected = NotAuthorizedException.class)
    public void ShouldThrowErrorForInvalidAuthHeaderWithBlankToken() {
        authHeaderHelper.getTokenFromAuthHeader(blankTokenHeader);
    }
}
