package com.rackspace.idm.api.filter;

import javax.ws.rs.core.HttpHeaders;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.sun.jersey.spi.container.ContainerRequest;

public class AuthenticationFilterTests {

    private ScopeAccessService oauthService;
    private AuthenticationFilter authFilter;
    private ContainerRequest request;

    @Before
    public void setUp() {
        oauthService = EasyMock
        .createNiceMock(ScopeAccessService.class);
        authFilter = new AuthenticationFilter(oauthService);
        request = EasyMock.createNiceMock(ContainerRequest.class);
    }
    
    @Test
    public void shouldIgnoreCloudResourceRequest() {
        EasyMock.expect(request.getPath()).andReturn("cloud");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreWadlRequest() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/application.wadl");
        EasyMock.expect(request.getMethod()).andReturn("GET");
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
        EasyMock.expect(request.getPath()).andReturn("v1.0/token");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        replayAndRunFilter();
    }

    @Test()
    public void shouldAcceptAnyOtherRequest() {
        EasyMock.expect(request.getPath()).andReturn(
        "v1.0/customers/RCN-000-000-000/users/foobar/password");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        final String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
        .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                true);
        replayAndRunFilter();
    }

    @Test
    public void shouldPassAuthenticationWhenTokenIsValid() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        final String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
        .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                true);
        replayAndRunFilter();
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsInvalid() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/something/foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        final String header = "OAuth " + tokenString;
        EasyMock.expect(request.getHeaderValue(HttpHeaders.AUTHORIZATION))
        .andReturn(header);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                false);
        replayAndRunFilter();
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldFailAuthenticationWhenTokenIsMissing() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/something/foo");
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
