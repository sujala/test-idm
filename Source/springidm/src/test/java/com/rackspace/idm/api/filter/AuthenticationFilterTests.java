package com.rackspace.idm.api.filter;

import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.sun.jersey.spi.container.ContainerRequest;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

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
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1/auth");
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

    @Test
    public void filter_matchesV10Path_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.0/auth");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV10VersionPath_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.0/");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV10VersionPathWithoutSlash_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.0");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV11VersionPath_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1/");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV11VersionPathWithoutSlash_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV11Path_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1/auth");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV20AuthPath_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v2.0/auth");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_matchesV20Path_returnsRequest() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v2.0");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test(expected = CloudAdminAuthorizationException.class)
    public void filter_matchesAdminPath_throwsCloudAdminAuthorizationException() throws Exception {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1/users");
        EasyMock.replay(request, oauthService);
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }
}
