package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.CloudUserExtractor;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.Token;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:25 PM
 */
public class DelegateCloud20ServiceTest {

    DummyCloud20Service dummyCloud20Service;
    DelegateCloud20Service delegateCloud20Service;
    DefaultCloud20Service defaultCloud20Service = mock(DefaultCloud20Service.class);
    UserService userService = mock(UserService.class);
    TokenService tokenService = mock(TokenService.class);
    CloudClient cloudClient = mock(CloudClient.class);
    ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
    TenantService tenantService = mock(TenantService.class);
    CloudUserExtractor cloudUserExtractor = mock(CloudUserExtractor.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private final Configuration config = mock(Configuration.class);
    AuthenticationRequest authenticationRequest = mock(AuthenticationRequest.class);
    String url = "http://url.com/";
    Boolean disabled = true;
    private String roleId = "roleId";
    private String userId = "userId";
    private String tenantId = "tenantId";
    private String serviceId = "serviceId";
    private String username = "username";
    DelegateCloud20Service spy;

    @Before
    public void setUp() throws IOException, JAXBException {
        dummyCloud20Service = new DummyCloud20Service();
        delegateCloud20Service = new DelegateCloud20Service();
        delegateCloud20Service.setDummyCloud20Service(dummyCloud20Service);
        delegateCloud20Service.setCloudClient(cloudClient);
        delegateCloud20Service.setDefaultCloud20Service(defaultCloud20Service);
        delegateCloud20Service.setUserService(userService);
        delegateCloud20Service.setTokenService(tokenService);
        delegateCloud20Service.setCloudUserExtractor(cloudUserExtractor);
        delegateCloud20Service.setScopeAccessService(scopeAccessService);
        delegateCloud20Service.setTenantService(tenantService);
        when(config.getString("cloudAuth20url")).thenReturn(url);
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(disabled);
        delegateCloud20Service.setConfig(config);
        when(cloudClient.post(anyString(), Matchers.<HttpHeaders>any(), anyString())).thenReturn(Response.ok());
        spy = spy(delegateCloud20Service);
    }

    @Test
    public void listTenants_tokenDoesNotExistInGA_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken("token")).thenReturn(null);
        spy.listTenants(null,"token",null,null);
        verify(cloudClient).get(anyString(),any(HttpHeaders.class));
    }

    @Test
    public void listTenants_tokenExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken("token")).thenReturn(new ScopeAccess());
        spy.listTenants(null,"token",null,null);
        verify(scopeAccessService).getScopeAccessByAccessToken("token");
    }

    @Test
    public void listEndpointsForToken_callsScopeAccessService() throws Exception {
        spy.listEndpointsForToken(null,null,"tokenId");
        verify(scopeAccessService).getScopeAccessByAccessToken("tokenId");
    }

    @Test
    public void listEndpointsForToken_tokenDoesNotExistInGA_andRoutingTrue_callsCloudClient() throws Exception {
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(null);
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        spy.listEndpointsForToken(null,null,"tokenId");
        verify(cloudClient).get(anyString(),any(HttpHeaders.class));
    }

    @Test
    public void listEndpointsForToken_tokenExistInGA_callsDefaultService() throws Exception {
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(new ScopeAccess());
        spy.listEndpointsForToken(null,null,"tokenId");
        verify(defaultCloud20Service).listEndpointsForToken(null,null,"tokenId");
    }
    // Created Today
    @Test
    public void authenticate_userIsMigrated_returnsDefaultServiceResponse() throws Exception {
        when(userService.isMigratedUser(any(User.class))).thenReturn(true);
        when(defaultCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.noContent());
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders, authenticationRequest), instanceOf(Response.ResponseBuilder.class));
    }
    // Created Today
    @Test
    public void authenticate_userNotNull_returnsDefaultServiceResponse() throws Exception {
        User user = new User();
        when(cloudUserExtractor.getUserByV20CredentialType(authenticationRequest)).thenReturn(user);
        when(cloudClient.post(anyString(), any(HttpHeaders.class), anyString())).thenReturn(Response.status(123));
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders, authenticationRequest), instanceOf(Response.ResponseBuilder.class));
    }

    @Test
    public void authenticate_returnsResponse() throws Exception {
        when(defaultCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.noContent());
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders, authenticationRequest), instanceOf(Response.ResponseBuilder.class));
    }

    @Test
    public void appendQueryParams_withNoParams_remainsSame() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", request, equalTo(updatedRequest));
    }

    @Test
    public void appendQueryParams_withOneParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }

    @Test
    public void appendQueryParams_withTwoParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", "value2");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, anyOf(
                equalTo("http://localhost/echo?param=value&param2=value2"),
                equalTo("http://localhost/echo?param2=value2&param=value")
        ));
    }

    @Test
    public void appendQueryParams_withNullParam_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", null);
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }

    @Test
    public void listExtensions_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listExtensions(mockHeaders);
        verify(cloudClient).get(url + "extensions", mockHeaders);
    }

    @Test
    public void listExtensions_callsConfigUseCloudAuth() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void listExtensions_useCloudAuthIsFalse_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient, times(0)).get(url, new HashMap<String, String>());
    }

    @Test
    public void listTenants_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(defaultCloud20Service).listTenants(null, null, null, null);
    }

    @Test
    public void listTenants_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(defaultCloud20Service).listTenants(null, null, null, null);
    }

    @Test
    public void listTenants_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listTenants(mockHeaders, null, null, null);
        verify(cloudClient).get(url + "tenants", mockHeaders);
    }

    @Test
    public void getTenantByName_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantByName_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantByName_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getTenantByName(mockHeaders, null, "myName");
        verify(cloudClient).get(url + "tenants?name=myName", mockHeaders);
    }

    @Test
    public void getTenantByName_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantById_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantById(null, null, null);
        verify(defaultCloud20Service).getTenantById(null, null, null);
    }

    @Test
    public void getTenantById_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantById(null, null, null);
        verify(defaultCloud20Service).getTenantById(null, null, null);
    }

    @Test
    public void getTenantById_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getTenantById(mockHeaders, null, "myId");
        verify(cloudClient).get(url + "tenants/myId", mockHeaders);
    }

    @Test
    public void validateToken_RoutingFalseAndTokenDoesNotExist_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(tokenService.doesTokenHaveAccessToApplication(null, null)).thenReturn(false);
        delegateCloud20Service.validateToken(null, "token", "token", null);
        verify(defaultCloud20Service).validateToken(null, "token", "token", null);
    }

    @Test
    public void validateToken_RoutingFalseAndTokenDoesExist_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(tokenService.doesTokenHaveAccessToApplication(null, null)).thenReturn(true);
        delegateCloud20Service.validateToken(null, "token", "token", null);
        verify(defaultCloud20Service).validateToken(null, "token", "token", null);
    }
    // Edited Today
    @Test
    public void validateToken_RoutingFalseAndTokenIsImpersonatedUserWithNoAccess_callsValidateImpersonatedTokenFromCloud() throws Exception {

        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken("token")).thenReturn(impersonatedScopeAccess);
        doReturn(new ResponseBuilderImpl()).when(spy).validateImpersonatedTokenFromCloud(null, null, null, impersonatedScopeAccess);
        spy.validateToken(null, "token", "token", null);
        verify(spy).validateImpersonatedTokenFromCloud(null, null, null, impersonatedScopeAccess);
    }
     // Created Today
    @Test
    public void validateToken_RoutingFalseAndTokenIsImpersonatedUserWithAccess_callsDefaultCloud20ServiceValidateToken() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken("token")).thenReturn(new ImpersonatedScopeAccess());
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(new ScopeAccess());
        when(defaultCloud20Service.validateToken(null, "token", "token", null)).thenReturn(new ResponseBuilderImpl());
        defaultCloud20Service.validateToken(null, "token", "token", null);
        verify(defaultCloud20Service).validateToken(null, "token", "token", null);
    }

    @Test(expected = BadRequestException.class)
    public void validateToken_nullToken_throwsBadRequestException() throws Exception {
        delegateCloud20Service.validateToken(null,null,null,null);
    }
    // Edited Today
    @Test(expected = BadRequestException.class)
    public void validateToken_emptyToken_throwsBadRequestException() throws Exception {
        delegateCloud20Service.validateToken(null,null,"  ",null);
    }

    @Test
    public void validateToken_RoutingTrueAndTokenDoesNotExist_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(tokenService.doesTokenHaveAccessToApplication(null, null)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.validateToken(mockHeaders, null, "1", null);
        verify(cloudClient).get(url + "tokens/1", mockHeaders);
    }

    @Test
    public void validateToken_RoutingTrueAndTokenDoesExist_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        ScopeAccess mockSA = mock(ScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken(Matchers.<String>any())).thenReturn(mockSA);
        delegateCloud20Service.validateToken(null, null, "test", null);
        verify(defaultCloud20Service).validateToken(null, null, "test", null);
    }

    @Test
    public void validateImpersonatedTokenFromCloud_SCOkFalseAndSCNonAuthoritativeInformationFalse() throws Exception{
       AuthenticateResponse authenticateResponse = new AuthenticateResponse();
       authenticateResponse.setToken(new Token());
       ArrayList<String> dummy = new ArrayList<String>();
       dummy.add("foo");
       MultivaluedMap<String,String> requestHeaders = mock(MultivaluedMap.class);
       doReturn(authenticateResponse).when(spy).getXAuthToken_byPassword(anyString(), anyString());
       when(httpHeaders.getRequestHeaders()).thenReturn(requestHeaders);
       when(requestHeaders.get("x-auth-token")).thenReturn(dummy);
       when(requestHeaders.get("accept")).thenReturn(dummy);
       doReturn(Response.status(123)).when(spy).checkToken(eq(httpHeaders), anyString(), eq("token"), eq("foo"));
       assertThat("response", spy.validateImpersonatedTokenFromCloud(httpHeaders, "token", "foo", null).build().getStatus(), equalTo(123));
    }

    @Ignore
    @Test
    public void validateImpersonatedTokenFromCloud_SCOkTrueAndSCNonAuthoritativeInformationFalse() throws Exception{
        AuthenticateResponse authenticateResponse = mock(AuthenticateResponse.class);
        Response.ResponseBuilder responseBuilder = mock(Response.ResponseBuilder.class);
        Response response = mock(Response.class);
        ArrayList<String> dummy = new ArrayList<String>();
        dummy.add("foo");
        MultivaluedMap<String,String> requestHeaders = mock(MultivaluedMap.class);

        doReturn(authenticateResponse).when(spy).getXAuthToken_byPassword(anyString(), anyString());
        when(httpHeaders.getRequestHeaders()).thenReturn(requestHeaders);
        when(requestHeaders.get("x-auth-token")).thenReturn(dummy);
        when(requestHeaders.get("accept")).thenReturn(dummy);
        when(authenticateResponse.getToken()).thenReturn(new Token());
        doReturn(responseBuilder).when(spy).checkToken(eq(httpHeaders), anyString(), eq("token"), eq("foo"));
        when(responseBuilder.build()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity()).thenReturn(new Object());
        doReturn(authenticateResponse).when(spy).unmarshallResponse(anyString(),eq(AuthenticateResponse.class));
        assertThat("response", spy.validateImpersonatedTokenFromCloud(httpHeaders, "token", "foo", null).build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointsForToken(null, null, null);
        verify(defaultCloud20Service).listEndpointsForToken(null, null, null);
    }

    @Test
    public void listEndpointsForToken_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointsForToken(null, null, null);
        verify(defaultCloud20Service).listEndpointsForToken(null, null, null);
    }

    @Test
    public void listEndpointsForToken_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listEndpointsForToken(mockHeaders, null, "1");
        verify(cloudClient).get(url + "tokens/1/endpoints", mockHeaders);
    }

    @Test
    public void listExtensions_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listExtensions_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listExtensions_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listExtensions(mockHeaders);
        verify(cloudClient).get(url + "extensions", mockHeaders);
    }

    @Test
    public void listExtensions_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listRoles_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void listRoles_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void listRoles_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listRoles(mockHeaders, null, null, null, null);
        verify(cloudClient).get(url + "OS-KSADM/roles", mockHeaders);
    }

    @Test
    public void listRoles_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void addRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void addRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void addRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSADM/roles"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void getRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void getRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void getRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getRole(mockHeaders, null, "1");
        verify(cloudClient).get(url + "OS-KSADM/roles/1", mockHeaders);
    }

    @Test
    public void getRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void deleteRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void deleteRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void deleteRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSADM/roles/1", null);
    }

    @Test
    public void deleteRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void addTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void addTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void addTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(cloudClient).post(eq(url + "tenants"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void updateTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void updateTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void updateTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(cloudClient).post(eq(url + "tenants/1"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void listUsersForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUsersForTenant(mockHeaders, null, "1", null, null);
        verify(cloudClient).get(url + "tenants/1/users", mockHeaders);
    }

    @Test
    public void listUsersForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUsersWithRoleForTenant(mockHeaders, null, "1", "2", null, null);
        verify(cloudClient).get(url + "tenants/1/users?roleId=2", mockHeaders);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void deleteTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void deleteTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void deleteTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(cloudClient).delete(url + "tenants/1", null);
    }

    @Test
    public void deleteTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void listRolesForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listRolesForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listRolesForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listRolesForTenant(mockHeaders, null, "1", null, null);
        verify(cloudClient).get(url + "tenants/1/OS-KSADM/roles", mockHeaders);
    }

    @Test
    public void listRolesForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listServices_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void listServices_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void listServices_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listServices(mockHeaders, null, null, null);
        verify(cloudClient).get(url + "OS-KSADM/services", mockHeaders);
    }

    @Test
    public void listServices_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void addService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void addService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void addService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addService(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSADM/services"), Matchers.<HttpHeaders>any(), Matchers.anyString());
    }

    @Test
    public void addService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void getService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void getService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void getService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getService(mockHeaders, null, "1");
        verify(cloudClient).get(url + "OS-KSADM/services/1", mockHeaders);
    }

    @Test
    public void getService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void deleteService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");
    }

    @Test
    public void deleteService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");
        ;
    }

    @Test
    public void deleteService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSADM/services/1", null);
    }

    @Test
    public void deleteService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getEndpointTemplate(mockHeaders, null, "1");
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates/1", mockHeaders);
    }

    @Test
    public void getEndpointTemplate_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSCATALOG/endpointTemplates/1", null);
    }

    @Test
    public void deleteEndpointTemplate_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listEndpoints(mockHeaders, null, "1");
        verify(cloudClient).get(url + "tenants/1/OS-KSCATALOG/endpoints", mockHeaders);
    }

    @Test
    public void listEndpoints_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void checkToken_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.checkToken(null, null, "1", null);
        verify(defaultCloud20Service).checkToken(null, null, "1", null);
    }

    @Test
    public void checkToken_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.checkToken(null, null, "1", null);
        verify(defaultCloud20Service).checkToken(null, null, "1", null);
    }

    @Test
    public void checkToken_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.checkToken(mockHeaders, null, "1", null);
        verify(cloudClient).get(url + "tokens/1", mockHeaders);
    }

    @Test
    public void checkToken_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.checkToken(null, null, "1", null);
        verify(defaultCloud20Service).checkToken(null, null, "1", null);
    }

    @Test
    public void getExtension_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getExtension_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getExtension_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getExtension(mockHeaders, "RAX-KSKEY");
        verify(cloudClient).get(url + "extensions/RAX-KSKEY", mockHeaders);
    }

    @Test
    public void getExtension_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void getEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void getEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getEndpoint(mockHeaders, null, "1", "2");
        verify(cloudClient).get(url + "tenants/2/OS-KSCATALOG/endpoints/1", mockHeaders);
    }

    @Test
    public void getEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(cloudClient).delete(url + "tenants/2/OS-KSCATALOG/endpoints/1", null);
    }

    @Test
    public void deleteEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void addEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void addEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void addEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(cloudClient).post(eq(url + "tenants/1/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), Matchers.anyString());
    }

    @Test
    public void addEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void listTenants_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listTenants(mockHeaders, "token", null, null);
        verify(cloudClient).get(url + "tenants", mockHeaders);
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_doesntCallCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listTenants(null, "token", null, null);
        verify(cloudClient, times(0)).get(url + "tenants", new HashMap<String, String>());
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_returns200() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(defaultCloud20Service.listTenants(null, "token", null, null)).thenReturn(Response.ok());
        Response.ResponseBuilder responseBuilder = delegateCloud20Service.listTenants(null, "token", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void deleteRoleFromUserOnTenant_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).delete(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void deleteRoleFromUserOnTenant_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).delete(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void addEndpoint_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addEndpoint_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(cloudClient).post(eq(url + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addEndpoint_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(cloudClient, times(0)).post(eq(url + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void listExtensions_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud20Service.listExtensions(null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void getExtensions_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listExtensions(mockHeaders);
        verify(cloudClient).get(url + "extensions", mockHeaders);
    }

    @Test
    public void getExtensions_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listExtensions(mockHeaders);
        verify(cloudClient, times(0)).get(url + "extensions", mockHeaders);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaTrue_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getUserById(mockHeaders, null, userId);
        verify(cloudClient).get(url + "users/" + userId, mockHeaders);
    }

    @Test
    public void getUserById_RoutingTrue_UserExistsInGaTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GASourceOFTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GASourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GANotSourceOFTruth_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listEndpointTemplates(mockHeaders, null, serviceId);
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates?serviceId=serviceId", mockHeaders);
    }

    @Test
    public void listEndpointTemplates_RoutingTrue_GANotSourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GASourceOFTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GASourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GANotSourceOFTruth_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSCATALOG/endpointTemplates"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addEndpointTemplate_RoutingTrue_GANotSourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void listUserGroups_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void listUserGroups_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void listUserGroups_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUserGroups(mockHeaders, null, userId);
        verify(cloudClient).get(url + "users/" + userId + "/RAX-KSGRP", mockHeaders);
    }

    @Test
    public void listUserGroups_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void getUserByName_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsByUsername(username)).thenReturn(false);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void getUserByName_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsByUsername(userId)).thenReturn(true);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void getUserByName_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsByUsername(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getUserByName(mockHeaders, null, username);
        verify(cloudClient).get(url + "users?name=" + username, mockHeaders);
    }

    @Test
    public void getUserByName_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsByUsername(username)).thenReturn(true);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void listUserGlobalRoles_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRoles_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRoles_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUserGlobalRoles(mockHeaders, null, userId);
        verify(cloudClient).get(url + "users/" + userId + "/roles", mockHeaders);
    }

    @Test
    public void listUserGlobalRoles_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUserGlobalRolesByServiceId(mockHeaders, null, userId, serviceId);
        verify(cloudClient).get(url + "users/" + userId + "/roles?serviceId=" + serviceId, mockHeaders);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void addUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addUserCredential(null, null, userId, null);
        verify(defaultCloud20Service).addUserCredential(null, null, userId, null);
    }

    @Test
    public void addUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserCredential(null, null, userId, null);
        verify(defaultCloud20Service).addUserCredential(null, null, userId, null);
    }

    @Test
    public void addUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        delegateCloud20Service.addUserCredential(httpHeaders, null, userId, null);
        verify(cloudClient).post(url + "users/" + userId + "/OS-KSADM/credentials", httpHeaders, null);
    }

    @Test
    public void addUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserCredential(null, null, userId, null);
        verify(defaultCloud20Service).addUserCredential(null, null, userId, null);
    }


    @Test
    public void listCredentials_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).listCredentials(null, null, userId, null, null);
    }

    @Test
    public void listCredentials_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).listCredentials(null, null, userId, null, null);
    }

    @Test
    public void listCredentials_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listCredentials(mockHeaders, null, userId, null, null);
        verify(cloudClient).get(url + "users/" + userId + "/OS-KSADM/credentials", mockHeaders);
    }

    @Test
    public void listCredentials_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).listCredentials(null, null, userId, null, null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserPasswordCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null, null, userId, null, null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserPasswordCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null, null, userId, null, null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserPasswordCredentials(null, null, userId, "type", null);
        verify(cloudClient).post(eq(url + "users/" + userId + "/OS-KSADM/credentials/type"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateUserPasswordCredentials_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserPasswordCredentials(null, null, userId, null, null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null, null, userId, null, null);
    }

    @Test
    public void listUsers_RoutingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsers(null, null, null, null);
        verify(defaultCloud20Service).listUsers(null, null, null, null);
    }

    @Test
    public void listUsers_RoutingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsers(null, null, null, null);
        verify(defaultCloud20Service).listUsers(null, null, null, null);
    }

    @Test
    public void listUsers_RoutingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listUsers(mockHeaders, null, null, null);
        verify(cloudClient).get(url + "users", mockHeaders);
    }

    @Test
    public void listUsers_RoutingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsers(null, null, null, null);
        verify(defaultCloud20Service).listUsers(null, null, null, null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserApiKeyCredentials(null, null, userId, "type", null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null, null, userId, "type", null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserApiKeyCredentials(null, null, userId, "type", null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null, null, userId, "type", null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserApiKeyCredentials(null, null, userId, "type", null);
        verify(cloudClient).post(eq(url + "users/" + userId + "/OS-KSADM/credentials/type"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserApiKeyCredentials(null, null, userId, "type", null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null, null, userId, "type", null);
    }

    @Test
    public void getUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).getUserCredential(null, null, userId, "type");
    }

    @Test
    public void getUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).getUserCredential(null, null, userId, "type");
    }

    @Test
    public void getUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.getUserCredential(mockHeaders, null, userId, "type");
        verify(cloudClient).get(url + "users/" + userId + "/OS-KSADM/credentials/type", mockHeaders);
    }

    @Test
    public void getUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).getUserCredential(null, null, userId, "type");
    }

    @Test
    public void deleteUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void deleteUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void deleteUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(cloudClient).delete(url + "users/" + userId + "/OS-KSADM/credentials/type", null);
    }

    @Test
    public void deleteUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void listRolesForUserOnTenant_RoutingFalse_UserExistsFalse_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listRolesForUserOnTenant(null, null, tenantId, userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null, null, tenantId, userId);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingFalse_UserExistsTrue_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listRolesForUserOnTenant(null, null, tenantId, userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null, null, tenantId, userId);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingTrue_UserExistsFalse_callClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        delegateCloud20Service.listRolesForUserOnTenant(mockHeaders, null, tenantId, userId);
        verify(cloudClient).get(url + "tenants/" + tenantId + "/users/" + userId + "/roles", mockHeaders);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingTrue_UserExistsTrue_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listRolesForUserOnTenant(null, null, tenantId, userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null, null, tenantId, userId);
    }

    @Test
    public void addUser_RoutingFalse_GASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addUser(null, null, null, null);
        verify(defaultCloud20Service).addUser(null, null, null, null);
    }

    @Test
    public void addUser_RoutingFalse_GASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addUser(null, null, null, null);
        verify(defaultCloud20Service).addUser(null, null, null, null);
    }

    @Test
    public void addUser_RoutingTrue_GASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addUser(null, null, null, null);
        verify(cloudClient).post(eq(url + "users"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addUser_RoutingTrue_GASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addUser(null, null, null, null);
        verify(defaultCloud20Service).addUser(null, null, null, null);
    }

    @Test
    public void updateUser_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUser(null, null, userId, null);
        verify(defaultCloud20Service).updateUser(null, null, userId, null);
    }

    @Test
    public void updateUser_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUser(null, null, userId, null);
        verify(defaultCloud20Service).updateUser(null, null, userId, null);
    }

    @Test
    public void updateUser_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUser(null, null, userId, null);
        verify(cloudClient).post(eq(url + "users/" + userId), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateUser_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUser(null, null, userId, null);
        verify(defaultCloud20Service).updateUser(null, null, userId, null);
    }

    @Test
    public void deleteUser_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void deleteUser_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void deleteUserUser_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(cloudClient).delete(eq(url + "users/" + userId), Matchers.<HttpHeaders>any());
    }

    @Test
    public void deleteUserUser_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void setUserEnabled_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.setUserEnabled(null, null, userId, null);
        verify(defaultCloud20Service).setUserEnabled(null, null, userId, null);
    }

    @Test
    public void setUserEnabled_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.setUserEnabled(null, null, userId, null);
        verify(defaultCloud20Service).setUserEnabled(null, null, userId, null);
    }

    @Test
    public void setUserEnabled_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.setUserEnabled(null, null, userId, null);
        verify(cloudClient).put(eq(url + "users/" + userId + "/OS-KSADM/enabled"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void setUserEnabled_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.setUserEnabled(null, null, userId, null);
        verify(defaultCloud20Service).setUserEnabled(null, null, userId, null);
    }

    @Test
    public void addUserRole_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).addUserRole(null, null, userId, roleId);
    }

    @Test
    public void addUserRole_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).addUserRole(null, null, userId, roleId);
    }

    @Test
    public void addUserRole_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(cloudClient).put(eq(url + "users/" + userId + "/roles/OS-KSADM/" + roleId), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addUserRole_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).addUserRole(null, null, userId, roleId);
    }

    @Test
    public void getUserRole_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).getUserRole(null, null, userId, roleId);
    }

    @Test
    public void getUserRole_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).getUserRole(null, null, userId, roleId);
    }

    @Test
    public void getUserRole_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(cloudClient).get(eq(url + "users/" + userId + "/roles/OS-KSADM/" + roleId), Matchers.<HttpHeaders>any());
    }

    @Test
    public void getUserRole_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).getUserRole(null, null, userId, roleId);
    }

    @Test
    public void deleteUserRole_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).deleteUserRole(null, null, userId, roleId);
    }

    @Test
    public void deleteUserRole_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).deleteUserRole(null, null, userId, roleId);
    }

    @Test
    public void deleteUserRole_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(cloudClient).delete(eq(url + "users/" + userId + "/roles/OS-KSADM/" + roleId), Matchers.<HttpHeaders>any());
    }

    @Test
    public void deleteUserRole_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(defaultCloud20Service).deleteUserRole(null, null, userId, roleId);
    }

    @Test
    public void getSecretQA_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getSecretQA(null, null, userId);
        verify(defaultCloud20Service).getSecretQA(null, null, userId);
    }

    @Test
    public void getSecretQA_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getSecretQA(null, null, userId);
        verify(defaultCloud20Service).getSecretQA(null, null, userId);
    }

    @Test
    public void getSecretQA_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getSecretQA(null, null, userId);
        verify(cloudClient).get(eq(url + "users/" + userId + "/RAX-KSQA/secretqa"), Matchers.<HttpHeaders>any());
    }

    @Test
    public void getSecretQA_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getSecretQA(null, null, userId);
        verify(defaultCloud20Service).getSecretQA(null, null, userId);
    }

    @Test
    public void updateSecretQA_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateSecretQA(null, null, userId, null);
        verify(defaultCloud20Service).updateSecretQA(null, null, userId, null);
    }

    @Test
    public void updateSecretQA_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateSecretQA(null, null, userId, null);
        verify(defaultCloud20Service).updateSecretQA(null, null, userId, null);
    }

    @Test
    public void updateSecretQA_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateSecretQA(null, null, userId, null);
        verify(cloudClient).put(eq(url + "users/" + userId + "/RAX-KSQA/secretqa"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateSecretQA_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateSecretQA(null, null, userId, null);
        verify(defaultCloud20Service).updateSecretQA(null, null, userId, null);
    }

    @Test
    public void addRolesToUserOnTenant_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(defaultCloud20Service).addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
    }

    @Test
    public void addRolesToUserOnTenant_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(defaultCloud20Service).addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
    }

    @Test
    public void addRolesToUserOnTenant_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).put(eq(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addRolesToUserOnTenant_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(defaultCloud20Service).addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
    }

    @Test
    public void getTenantById_callsTenantService_getTenantById() throws Exception {
        delegateCloud20Service.getTenantById(null, null, "tenantId");
        verify(tenantService).getTenant(tenantId);
    }

    @Test
    public void getTenantById_TenantService_getTenantByIdReturnsTenant_callsDefaultService() throws Exception {
        when(tenantService.getTenant("tenantId")).thenReturn(new Tenant());
        delegateCloud20Service.getTenantById(null, null, "tenantId");
        verify(defaultCloud20Service).getTenantById(null,null,"tenantId");
    }

    @Test
    public void getTenantById_TenantService_getTenantByIdReturnsNull_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(tenantService.getTenant("tenantId")).thenReturn(null);
        delegateCloud20Service.getTenantById(null, null, "tenantId");
        verify(cloudClient).get(anyString(), any(HttpHeaders.class));
    }

    @Test
    public void getGroupById_routingEnabledAndNotSourceOfTruth_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getGroupById(null, null, "groupId");
        verify(cloudClient).get(eq(url + "RAX-GRPADM/groups/groupId"), any(HttpHeaders.class));
    }

    @Test
    public void getGroupById_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getGroupById(null, null, "groupId");
        verify(defaultCloud20Service).getGroupById(null, null, "groupId");
    }

    @Test
    public void getGroupById_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getGroupById(null, null, "groupId");
        verify(defaultCloud20Service).getGroupById(null, null, "groupId");
    }

    @Test
    public void getGroupById_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getGroupById(null, null, "groupId");
        verify(defaultCloud20Service).getGroupById(null, null, "groupId");
    }

    @Test
    public void addGroup_routingEnabledAndNotSourceOfTruth_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addGroup(null, null, null, null);
        verify(cloudClient).post(eq(url + "RAX-GRPADM/groups"), any(HttpHeaders.class), anyString());
    }

    @Test
    public void addGroup_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addGroup(null, null, null, null);
        verify(defaultCloud20Service).addGroup(null, null, null, null);
    }

    @Test
    public void addGroup_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addGroup(null, null, null, null);
        verify(defaultCloud20Service).addGroup(null, null, null, null);
    }

    @Test
    public void addGroup_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addGroup(null, null, null, null);
        verify(defaultCloud20Service).addGroup(null, null, null, null);
    }

    @Test
    public void updateGroup_routingEnabledAndNotSourceOfTruthEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateGroup(null, null, "groupId", null);
        verify(cloudClient).put(eq(url + "RAX-GRPADM/groups/groupId"), any(HttpHeaders.class), anyString());
    }

    @Test
    public void updateGroup_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateGroup(null, null, "groupId", null);
        verify(defaultCloud20Service).updateGroup(null, null, "groupId", null);
    }

    @Test
    public void updateGroup_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateGroup(null, null, "groupId", null);
        verify(defaultCloud20Service).updateGroup(null, null, "groupId", null);
    }

    @Test
    public void updateGroup_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateGroup(null, null, "groupId", null);
        verify(defaultCloud20Service).updateGroup(null, null, "groupId", null);
    }

    @Test
    public void deleteGroup_routingEnabledAndNotSourceOfTruthEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteGroup(null, null, "groupId");
        verify(cloudClient).delete(eq(url + "RAX-GRPADM/groups/groupId"), any(HttpHeaders.class));
    }

    @Test
    public void deleteGroup_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteGroup(null, null, "groupId");
        verify(defaultCloud20Service).deleteGroup(null, null, "groupId");
    }

    @Test
    public void deleteGroup_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteGroup(null, null, "groupId");
        verify(defaultCloud20Service).deleteGroup(null, null, "groupId");
    }

    @Test
    public void deleteGroup_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteGroup(null, null, "groupId");
        verify(defaultCloud20Service).deleteGroup(null, null, "groupId");
    }

    @Test
    public void addUserToGroup_routingEnabledAndNotUserInGAbyIdEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        doReturn(false).when(spy).isUserInGAbyId("userId");
        spy.addUserToGroup(null, null, "groupId", "userId");
        verify(cloudClient).put(eq(url + "RAX-GRPADM/groups/groupId/users/userId"), any(HttpHeaders.class), anyString());
    }

    @Test
    public void addUserToGroup_routingNotEnabledAndNotUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(false).when(spy).isUserInGAbyId("userId");
        spy.addUserToGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).addUserToGroup(null, null, "groupId", "userId");
    }

    @Test
    public void addUserToGroup_routingEnabledAndUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        doReturn(true).when(spy).isUserInGAbyId("userId");
        spy.addUserToGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).addUserToGroup(null, null, "groupId", "userId");
    }

    @Test
    public void addUserToGroup_routingNotEnabledAndUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(true).when(spy).isUserInGAbyId("userId");
        spy.addUserToGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).addUserToGroup(null, null, "groupId", "userId");
    }

    @Test
    public void removeUserFromGroup_routingEnabledAndNotUserInGAbyIdEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        doReturn(false).when(spy).isUserInGAbyId("userId");
        spy.removeUserFromGroup(null, null, "groupId", "userId");
        verify(cloudClient).delete(eq(url + "RAX-GRPADM/groups/groupId/users/userId"), any(HttpHeaders.class));
    }

    @Test
    public void removeUserFromGroup_routingNotEnabledAndNotUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(false).when(spy).isUserInGAbyId("userId");
        spy.removeUserFromGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).removeUserFromGroup(null, null, "groupId", "userId");
    }

    @Test
    public void removeUserFromGroup_routingEnabledAndUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        doReturn(true).when(spy).isUserInGAbyId("userId");
        spy.removeUserFromGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).removeUserFromGroup(null, null, "groupId", "userId");
    }

    @Test
    public void removeUserFromGroup_routingNotEnabledAndUserInGAbyIdEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(true).when(spy).isUserInGAbyId("userId");
        spy.removeUserFromGroup(null, null, "groupId", "userId");
        verify(defaultCloud20Service).removeUserFromGroup(null, null, "groupId", "userId");
    }

    @Test
    public void getUsersForGroup_routingEnabledAndNotSourceOfTruthEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getUsersForGroup(null, null, "groupId", "marker", 0);
        verify(cloudClient).get(eq(url + "RAX-GRPADM/groups/groupId/users?limit=0&marker=marker"), any(HttpHeaders.class));
    }

    @Test
    public void getUsersForGroup_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getUsersForGroup(null, null, "groupId", "marker", 0);
        verify(defaultCloud20Service).getUsersForGroup(null, null, "groupId", "marker", 0);
    }

    @Test
    public void getUsersForGroup_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getUsersForGroup(null, null, "groupId", "marker", 0);
        verify(defaultCloud20Service).getUsersForGroup(null, null, "groupId", "marker", 0);
    }

    @Test
    public void getUsersForGroup_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getUsersForGroup(null, null, "groupId", "marker", 0);
        verify(defaultCloud20Service).getUsersForGroup(null, null, "groupId", "marker", 0);
    }

    @Test
    public void getGroup_routingEnabledAndNotSourceOfTruthEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getGroup(null, null, "groupName");
        verify(cloudClient).get(eq(url + "RAX-GRPADM/groups?name=groupName"), any(HttpHeaders.class));
    }

    @Test
    public void getGroup_routingNotEnabledAndNotSourceOfTruth_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getGroup(null, null, "groupName");
        verify(defaultCloud20Service).listGroups(null, null, "groupName", null, null);
    }

    @Test
    public void getGroup_routingEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getGroup(null, null, "groupName");
        verify(defaultCloud20Service).listGroups(null, null, "groupName", null, null);
    }

    @Test
    public void getGroup_routingNotEnabledAndSourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getGroup(null, null, "groupName");
        verify(defaultCloud20Service).listGroups(null, null, "groupName", null, null);
    }
}

