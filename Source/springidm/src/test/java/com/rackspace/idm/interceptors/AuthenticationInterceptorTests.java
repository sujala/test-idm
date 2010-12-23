package com.rackspace.idm.interceptors;

import java.net.URISyntaxException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.oauthAuthentication.AuthenticationResult;
import com.rackspace.idm.oauthAuthentication.HttpOauthAuthenticationService;
import com.rackspace.idm.test.stub.StubLogger;

public class AuthenticationInterceptorTests {

    private HttpOauthAuthenticationService oauthService;
    private Logger logger;
    private AuthenticationInterceptor authInterceptor;

    @Before
    public void setUp() {

        oauthService = EasyMock
            .createMock(HttpOauthAuthenticationService.class);
        logger = new StubLogger();
        authInterceptor = new AuthenticationInterceptor(oauthService, logger);
    }

    @Test
    public void shouldIgnorePasswordChangeRequest() {
        InterceptedCall call = InterceptedCall.SET_USER_PASSWORD;
        boolean accept = authInterceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertFalse(accept);
    }

    @Test
    public void shouldIgnoreRootPath() {
        InterceptedCall call = InterceptedCall.GET_APP_VERSIONS;
        boolean accept = authInterceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertFalse(accept);
    }

    @Test
    public void shouldIgnoreTokenRequest() {
        InterceptedCall call = InterceptedCall.GET_ACCESS_TOKEN;
        boolean accept = authInterceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertFalse(accept);
    }

    @Test
    public void shouldAcceptAnyOtherRequest() {
        InterceptedCall call = InterceptedCall.ADD_FIRST_USER;
        boolean accept = authInterceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertTrue(accept);
    }

    @Test
    public void shouldPassAuthenticationWhenTokenIsValid()
        throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest
            .get("/idm/protected_resource").header(HttpHeaders.AUTHORIZATION,
                "OAuth goodtoken");

        AuthenticationResult authResult = new AuthenticationResult();
        authResult.setHttpStatusCode(Status.OK.getStatusCode());

        EasyMock.expect(oauthService.authenticateToken("goodtoken")).andReturn(
            authResult);
        EasyMock.replay(oauthService);

        ServerResponse response = authInterceptor.preProcess(request, null);
        Assert.assertNull(response);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsInvalid()
        throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest
            .get("/idm/protected_resource").header(HttpHeaders.AUTHORIZATION,
                "OAuth badtoken");

        AuthenticationResult authResult = new AuthenticationResult();
        authResult.setHttpStatusCode(Status.UNAUTHORIZED.getStatusCode());

        EasyMock.expect(oauthService.authenticateToken("badtoken")).andReturn(
            authResult);
        EasyMock.replay(oauthService);

        authInterceptor.preProcess(request, null);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsMissing()
        throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest
            .get("/idm/protected_resource");
        authInterceptor.preProcess(request, null);
    }
}
