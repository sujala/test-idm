package com.rackspace.idm.util;

import com.rackspace.idm.exception.NotAuthorizedException;
import com.unboundid.util.Base64;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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

    @Test(expected = NotAuthorizedException.class)
    public void getBase64EncodedString_withEmptyHeader_throwsNotAuthorizedException() throws Exception {
        authHeaderHelper.getBase64EncodedString("");
    }

    @Test(expected = NotAuthorizedException.class)
    public void getBase64EncodedString_withNullHeader_throwsNotAuthorizedException() throws Exception {
        authHeaderHelper.getBase64EncodedString(null);
    }

    @Test
    public void getBase64EncodedString_withValidHeader_returnsKeyValue() throws Exception {
        String basicAuthKey = authHeaderHelper.getBase64EncodedString("Basic abc");
        assertThat("key", basicAuthKey, equalTo("abc"));
    }

    @Test(expected = NotAuthorizedException.class)
    public void getBase64EncodedString_withInvalidHeader_returnsKeyValue() throws Exception {
        authHeaderHelper.getBase64EncodedString("Basic ");
    }
}
