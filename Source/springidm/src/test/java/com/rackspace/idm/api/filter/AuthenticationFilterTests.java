package com.rackspace.idm.api.filter;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.sun.jersey.spi.container.ContainerRequest;

import java.util.ArrayList;

public class AuthenticationFilterTests {

    private ScopeAccessService oauthService;
    private AuthenticationFilter authFilter;
    private ContainerRequest request;
    private ContainerRequest requestMock;
    private HttpServletRequest httpServletRequest;
    ScopeAccessService scopeAccessServiceMock;
    UserService userService;
    Configuration configuration;
    AuthenticationFilter authenticationFilterWithMock;


    @Before
    public void setUp() {
        oauthService = EasyMock
        .createNiceMock(ScopeAccessService.class);
        authFilter = new AuthenticationFilter(oauthService);
        request = EasyMock.createNiceMock(ContainerRequest.class);
        requestMock = mock(ContainerRequest.class);
        scopeAccessServiceMock = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        configuration = mock(Configuration.class);
        authenticationFilterWithMock = new AuthenticationFilter(scopeAccessServiceMock);
        authenticationFilterWithMock.setUserService(userService);
        authenticationFilterWithMock.setConfig(configuration);
    }

    @Test
    public void shouldPublicPaths() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("somet/thing/public");
        authFilter.filter(requestMock);
        verify(requestMock, never()).getHeaderValue("X-Auth-Token");
        assertTrue("no exceptions", true);
    }

    @Test
    public void shouldPublicCloudSplashScreen_withoutAuth() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud");
        authFilter.filter(requestMock);
        verify(requestMock, never()).getHeaderValue("X-Auth-Token");
        assertTrue("no exceptions", true);
    }

    @Test
    public void shouldCloudAuthOrValidationEndpoint_withoutAuth() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/v2.0/tokens");
        authFilter.filter(requestMock);
        verify(requestMock, never()).getHeaderValue("X-Auth-Token");
        assertTrue("no exceptions", true);
    }

    @Test
    public void shouldCloudTokenEndpoints_withoutAuth() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/v2.0/tokens/someToken/endpoints");
        authFilter.filter(requestMock);
        verify(scopeAccessServiceMock, never()).getScopeAccessByAccessToken(anyString());
    }

    @Test
    public void shouldCloudUrls_withNullToken_returnsRequest() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/Any/General/Path");
        when(requestMock.getHeaderValue("X-Auth-Token")).thenReturn(null);
        authFilter.filter(requestMock);
        verify(requestMock, never()).getRequestHeaders();
    }

    @Test
    public void shouldCloudUrls_withNonImpersonationToken_throwsNotAuthenticatedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/Any/General/Path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenString("authToken");
        impersonatedScopeAccess.setImpersonatingToken("impToken");
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusMinutes(10).toDate());
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(new ScopeAccess());
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test(expected = NotAuthorizedException.class)
    public void shouldCloudUrls_withExpiredToken_throwsNotAuthorizedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/Any/General/Path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenString("authToken");
        impersonatedScopeAccess.setImpersonatingToken("impToken");
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().minusMinutes(10).toDate());
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(impersonatedScopeAccess);
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test
    public void shouldCloudUrls_withImpersonationToken_returnsAlteredRequest() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("cloud/Any/General/Path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenString("authToken");
        impersonatedScopeAccess.setImpersonatingToken("impToken");
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusMinutes(10).toDate());
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(impersonatedScopeAccess);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        when(requestMock.getRequestHeaders()).thenReturn(multivaluedMap);
        authenticationFilterWithMock.filter(requestMock);
        assertThat("request headers changed", multivaluedMap.get("x-auth-token").get(0), equalTo("impToken"));
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldMigrateUrl_withNonRackerToken_throwsNotAuthenticatedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("migration/some/path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(new ScopeAccess());
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldMigrateUrl_withNoMigrationAdminRole_throwsNotAuthenticatedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("migration/some/path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(new RackerScopeAccess());
        when(userService.getRackerRoles(anyString())).thenReturn(new ArrayList<String >());
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldMigrateUrl_withNullRoles_throwsNotAuthenticatedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("migration/some/path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(new RackerScopeAccess());
        when(userService.getRackerRoles(anyString())).thenReturn(null);
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test
    public void shouldMigrateUrl_withMigrationAdminRole_returnsRequest() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("migration/some/path");
        when(requestMock.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER)).thenReturn("authToken");
        when(scopeAccessServiceMock.getScopeAccessByAccessToken("authToken")).thenReturn(new RackerScopeAccess());
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("migrationAdminGroup");
        when(userService.getRackerRoles(anyString())).thenReturn(roles);
        when(configuration.getString("migrationAdminGroup")).thenReturn("migrationAdminGroup");
        authenticationFilterWithMock.filter(requestMock);
    }

    @Test
    public void shouldIgnoreCloudResourceRequest() {
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.1/auth");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreApplicationWadlRequest() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/application.wadl");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreIdmWadlRequest() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/idm.wadl");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreXsdPath() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("xsdUsers.xsd");
        authFilter.filter(requestMock);
        verify(requestMock, never()).getHeaderValue("X-Auth-Token");
        assertTrue("no exceptions", true);
    }

    @Test
    public void shouldIgnoreXsltPath() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("xsltUsers.xslt");
        authFilter.filter(requestMock);
        verify(requestMock, never()).getHeaderValue("X-Auth-Token");
        assertTrue("no exceptions", true);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void withInvalidAccessToken_shouldThrowNotAuthenticatedException() throws Exception {
        when(requestMock.getMethod()).thenReturn("POST");
        when(requestMock.getPath()).thenReturn("some/path");
        when(scopeAccessServiceMock.authenticateAccessToken(anyString())).thenReturn(false);
        authFilter.filter(requestMock);
    }

    @Test
    public void shouldIgnoreRootPath() {
        EasyMock.expect(request.getPath()).andReturn("");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        replayAndRunFilter();
    }

    @Test
    public void shouldIgnoreTokenRequest() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/tokens");
        EasyMock.expect(request.getMethod()).andReturn("POST");
        replayAndRunFilter();
    }

    @Test()
    public void shouldAcceptAnyOtherRequest() {
        EasyMock.expect(request.getPath()).andReturn(
        "v1.0/customers/RCN-000-000-000/users/foobar/password");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        EasyMock.expect(request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER))
        .andReturn(tokenString);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                true);
        replayAndRunFilter();
    }

    @Test
    public void shouldPassAuthenticationWhenTokenIsValid() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        EasyMock.expect(request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER))
        .andReturn(tokenString);
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

    @Test
    public void filter_notEqualToNull_returnsRequest() throws Exception {
        httpServletRequest = mock(HttpServletRequest.class);
        EasyMock.expect(request.getPath()).andReturn("cloud/v1.0");
        EasyMock.replay(request, oauthService);
        authFilter.setHttpServletRequest(httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("http://remoteAddress/");
        when(httpServletRequest.getLocalAddr()).thenReturn("http://localhost:9998/");
        ContainerRequest containerRequest = authFilter.filter(request);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_pathStartsWithXsd_ReturnsRequest() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("somePath/xsdUsers.xsd");
        ContainerRequest containerRequest = authFilter.filter(requestMock);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test
    public void filter_pathStartsWithXslt_ReturnsRequest() throws Exception {
        when(requestMock.getMethod()).thenReturn("GET");
        when(requestMock.getPath()).thenReturn("somePath/xsltUsers.xslt");
        ContainerRequest containerRequest = authFilter.filter(requestMock);
        assertThat("request", containerRequest, notNullValue());
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotPassAuthentication_throwNotAuthenticatedException() {
        EasyMock.expect(request.getPath()).andReturn("v1.0/foo");
        EasyMock.expect(request.getMethod()).andReturn("GET");
        final String tokenString = "hiiamatoken";
        EasyMock.expect(request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER))
        .andReturn(tokenString);
        EasyMock.expect(oauthService.authenticateAccessToken(tokenString)).andReturn(
                false);
        replayAndRunFilter();
    }
}
