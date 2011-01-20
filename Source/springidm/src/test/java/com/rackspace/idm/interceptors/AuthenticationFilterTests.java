package com.rackspace.idm.interceptors;

import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.test.stub.StubLogger;
import com.sun.jersey.spi.container.ContainerRequest;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.ws.rs.core.HttpHeaders;

public class AuthenticationFilterTests {

    private OAuthService oauthService;
    private AuthenticationFilter authFilter;
    private ContainerRequest request;

    @Before
    public void setUp() {
        oauthService = EasyMock
                .createNiceMock(OAuthService.class);
        Logger logger = new StubLogger();
        authFilter = new AuthenticationFilter(oauthService, logger);
        request = EasyMock.createNiceMock(ContainerRequest.class);
    }

    @Test
    public void shouldIgnoreWadlRequest() {
        EasyMock.expect(request.getPath()).andReturn("application.wadl");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnorePasswordChangeRequest() {
        EasyMock.expect(request.getPath()).andReturn(
                "customers/RCN-000-000-000/users/foobar/password");
        EasyMock.expect(request.getMethod()).andReturn("PUT");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreRootPath() {
        EasyMock.expect(request.getPath()).andReturn("");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreTokenRequest() {
        EasyMock.expect(request.getPath()).andReturn("token");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        replayAndRunFilter();
    }

    @Test()
    public void shouldAcceptAnyOtherRequest() {
        EasyMock.expect(request.getPath()).andReturn(
                "customers/RCN-000-000-000/users/foobar/password");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        String tokenString = "hiiamatoken";
        String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
                .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                true);
        replayAndRunFilter();
    }

    @Test
    public void shouldPassAuthenticationWhenTokenIsValid() {
        EasyMock.expect(request.getPath()).andReturn("foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        String tokenString = "hiiamatoken";
        String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
                .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                true);
        replayAndRunFilter();
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsInvalid() {
        EasyMock.expect(request.getPath()).andReturn("foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        String tokenString = "hiiamatoken";
        String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
                .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                false);
        replayAndRunFilter();
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsMissing() {
        EasyMock.expect(request.getPath()).andReturn("foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
                .andReturn(null);
        replayAndRunFilter();
    }

    private void replayAndRunFilter() {
        EasyMock.replay(request, oauthService);
        authFilter.filter(request);
    }

}
