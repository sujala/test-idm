package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.*;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URI;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 3:58 PM
 */


public class DefaultCloud20ServiceTest {

    private DefaultCloud20Service defaultCloud20Service;
    private DefaultCloud20Service spy;
    private Configuration config;
    private UserService userService;
    private GroupService userGroupService;
    private JAXBObjectFactories jaxbObjectFactories;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private TenantService tenantService;
    private EndpointService endpointService;
    private ApplicationService clientService;
    private UserConverterCloudV20 userConverterCloudV20;
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private TokenConverterCloudV20 tokenConverterCloudV20;
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    private RoleConverterCloudV20 roleConverterCloudV20;
    private String authToken = "token";
    private EndpointTemplate endpointTemplate;
    private String userId = "id";
    private User user;
    private Users users;
    private Tenant tenant;
    private String tenantId = "tenantId";
    private CloudBaseUrl baseUrl;
    private Role role;
    private TenantRole tenantRole;
    private ClientRole clientRole;
    private Service service;
    private org.openstack.docs.identity.api.v2.Tenant tenantOS;
    private UserForCreate userOS;
    private CloudBaseUrl cloudBaseUrl;
    private Application application;
    private String roleId = "roleId";
    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs;
    private Group group;
    private UriInfo uriInfo;
    private CloudKsGroupBuilder cloudKsGroupBuilder;
//    private AtomHopperClient atomHopperClient;
    private HttpHeaders httpHeaders;
    private String jsonBody = "{\"passwordCredentials\":{\"username\":\"test_user\",\"password\":\"resetpass\"}}";
    private AuthConverterCloudV20 authConverterCloudV20;
    private HashMap<String, JAXBElement<Extension>> extensionMap;
    private JAXBElement<Extensions> currentExtensions;
    private ServiceConverterCloudV20 serviceConverterCloudV20;
    private String passwordCredentials = "passwordCredentials";
    private String apiKeyCredentials = "RAX-KSKEY:apiKeyCredentials";

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        //mocks
        userService = mock(UserService.class);
        userGroupService = mock(GroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        tenantConverterCloudV20 = mock(TenantConverterCloudV20.class);
        tokenConverterCloudV20 = mock(TokenConverterCloudV20.class);
        endpointConverterCloudV20 = mock(EndpointConverterCloudV20.class);
        roleConverterCloudV20 = mock(RoleConverterCloudV20.class);
        tenantService = mock(TenantService.class);
        endpointService = mock(EndpointService.class);
        clientService = mock(ApplicationService.class);
        config = mock(Configuration.class);
        cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
//        atomHopperClient = mock(AtomHopperClient.class);
        httpHeaders = mock(HttpHeaders.class);
        authConverterCloudV20 = mock(AuthConverterCloudV20.class);
        serviceConverterCloudV20 = mock(ServiceConverterCloudV20.class);


        //setting mocks
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setOBJ_FACTORIES(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        defaultCloud20Service.setUserConverterCloudV20(userConverterCloudV20);
        defaultCloud20Service.setTenantConverterCloudV20(tenantConverterCloudV20);
        defaultCloud20Service.setEndpointConverterCloudV20(endpointConverterCloudV20);
        defaultCloud20Service.setTenantService(tenantService);
        defaultCloud20Service.setTokenConverterCloudV20(tokenConverterCloudV20);
        defaultCloud20Service.setEndpointService(endpointService);
        defaultCloud20Service.setClientService(clientService);
        defaultCloud20Service.setConfig(config);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
//        defaultCloud20Service.setAtomHopperClient(atomHopperClient);
        defaultCloud20Service.setRoleConverterCloudV20(roleConverterCloudV20);
        defaultCloud20Service.setAuthConverterCloudV20(authConverterCloudV20);
        defaultCloud20Service.setServiceConverterCloudV20(serviceConverterCloudV20);

        //fields
        user = new User();
        user.setUsername(userId);
        user.setId(userId);
        user.setMossoId(123);
        role = new Role();
        role.setId("roleId");
        role.setName("roleName");
        role.setServiceId("role-ServiceId");
        tenantRole = new TenantRole();
        tenantRole.setClientId("clientId");
        tenantRole.setUserId(userId);
        tenantRole.setRoleRsId("tenantRoleId");
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("access");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setBaseUrlIds(new String[]{});
        clientRole = new ClientRole();
        clientRole.setClientId("clientId");
        clientRole.setId("clientRoleId");
        endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(101);
        service = new Service();
        service.setName("serviceName");
        service.setId("serviceId");
        service.setType("serviceType");
        tenantOS = new org.openstack.docs.identity.api.v2.Tenant();
        tenantOS.setId("tenantName");
        tenantOS.setName("tenantName");
        userOS = new UserForCreate();
        userOS.setId("userName");
        userOS.setUsername("username");
        userOS.setEmail("foo@bar.com");
        userOS.setEnabled(true);
        cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(101);
        cloudBaseUrl.setGlobal(false);
        application = new Application();
        application.setClientId("clientId");
        group = new Group();
        group.setName("Group1");
        group.setDescription("Group Description");
        group.setGroupId(1);
        groupKs = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groupKs.setId("1");
        groupKs.setName("Group1");
        groupKs.setDescription("Group Description");
        uriInfo = mock(UriInfo.class);

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        when(endpointService.getBaseUrlById(101)).thenReturn(cloudBaseUrl);
        when(clientService.getById(role.getServiceId())).thenReturn(application);
        when(clientService.getById("clientId")).thenReturn(application);
        when(clientService.getClientRoleById(role.getId())).thenReturn(clientRole);
        when(clientService.getClientRoleById(tenantRole.getRoleRsId())).thenReturn(clientRole);
        when(tenantService.getTenant(tenantId)).thenReturn(tenant);
        when(userService.getUserById(userId)).thenReturn(user);
        when(config.getString("rackspace.customerId")).thenReturn(null);
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(user);


        spy = spy(defaultCloud20Service);
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), any(String.class));
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), eq(tenantId));
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_expireInIsLessThan1_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        impersonationRequest.setUser(user1);
        impersonationRequest.setExpireInSeconds(0);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void validateImpersonationRequest_expireInNull_succeeds() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        impersonationRequest.setUser(user1);
        impersonationRequest.setExpireInSeconds(null);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void validateToken_whenExpiredToken_returns404() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenString("rackerToken");
        rackerScopeAccess.setAccessTokenExpired();
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken("token");
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, authToken, "token", null);
        assertThat("Reponse Code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsUserConverter_toUserForAuthenticateResponse() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        when(userService.getRackerByRackerId(any(String.class))).thenReturn(racker);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(userConverterCloudV20).toUserForAuthenticateResponse(org.mockito.Matchers.any(Racker.class), org.mockito.Matchers.any(List.class));
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsTenantService_getTenantRolesForScopeAccess() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(tenantService).getTenantRolesForScopeAccess(rackerScopeAccess);
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsUserService_getRackerByRackerId() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(userService).getRackerByRackerId("rackerId");
    }

    @Test
    public void validateToken_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(scopeAccessService).getScopeAccessByAccessToken("rackerToken");
    }

    @Test
    public void authenticate_withTenantId_callsTenantService_hasTenantAccess() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setTenantId("tenantId");
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("uuuuuuuuuu");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(tenantService).hasTenantAccess(org.mockito.Matchers.any(UserScopeAccess.class), eq("tenantId"));
    }

    @Test
    public void authenticate_withTenantIdAndNoTenantAccess_returns404() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(false);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void authenticate_withTenantNameAndNoTenantAccess_returns404() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantName"))).thenReturn(false);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void authenticate_endpoints_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void authenticate_notCloudIdentityAdmin_callsStripEndpoints() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(spy).stripEndpoints(any(List.class));
    }

    @Test
    public void authenticate_callsAuthConverterCloud() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.authenticate(null, authenticationRequest);
        verify(authConverterCloudV20).toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class));
    }

    @Test
    public void authenticate_withTenantId_callsVerifyTokenHasAccessForAuthenticate() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        Token token = new Token();
        token.setId("token");
        authenticateResponse.setToken(token);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        spy.authenticate(null, authenticationRequest);
        verify(spy).verifyTokenHasTenantAccessForAuthenticate("token", "tenantId");
    }

    @Test
    public void authenticate_withEmptyTenantId_returns400() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq(""))).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticate_getTenantIdThrowForbiddenException_returns401() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        Token token = new Token();
        token.setId("token");
        authenticateResponse.setToken(token);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(true);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        doThrow(new ForbiddenException()).when(spy).verifyTokenHasTenantAccessForAuthenticate("tokenId", "tenantId");
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        Token token = new Token();
        token.setId("token");
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        RoleList roleList = mock(RoleList.class);
        userForAuthenticateResponse.setRoles(roleList);
        authenticateResponse.setToken(token);
        authenticateResponse.setUser(userForAuthenticateResponse);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(true);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        doNothing().when(spy).verifyTokenHasTenantAccessForAuthenticate(anyString(), anyString());
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_withTenantName_callsTenantService_hasTenantAccess() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setTenantName("tenantId");
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("uuuuuuuuuu");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(tenantService).hasTenantAccess(org.mockito.Matchers.any(UserScopeAccess.class), eq("tenantId"));
    }

    @Test
    public void authenticate_tokenNotNullAndIdNotBlankScopeAccessIsNull_returns401() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void authenticate_tokenNotNullAndIsNotBlankTokenExpired_returns401() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        UserScopeAccess sa = new UserScopeAccess();
        sa.setAccessTokenExp(new Date(1000, 1, 1));
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(sa);
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void authenticate_tokenNotNullAndIsNotBlankScopeAccessNotInstanceOfUserScopeAccess_returns401() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        RackerScopeAccess sa = new RackerScopeAccess();
        sa.setAccessTokenExp(new Date(1000, 1, 1));
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(sa);
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void authenticate_tokenIsNullAndIsPasswordCredentials_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("test_user");
        passwordCredentialsRequiredUsername.setPassword("123");
        JAXBElement<? extends PasswordCredentialsRequiredUsername> credentialType = new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        doReturn(new User()).when(spy).checkAndGetUserByName("test_user");
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword("test_user", "123", null);
    }

    @Test
    public void authenticate_tokenIsNullAndIsAPICredentials_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername("test_user");
        apiKeyCredentials.setApiKey("123");
        JAXBElement<? extends ApiKeyCredentials> credentialType = new JAXBElement(QName.valueOf("foo"), ApiKeyCredentials.class, apiKeyCredentials);
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        doReturn(new User()).when(spy).checkAndGetUserByName("test_user");
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials("test_user", "123", null);
    }

    @Test
    public void verifyRackerOrServiceAdminAccess_callsAuthorizationService_authorizeCloudServiceAdmin() throws Exception {
        when(authorizationService.authorizeRacker(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.verifyRackerOrServiceAdminAccess(authToken);
        verify(authorizationService).authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void verifyRackerOrServiceAdminAccess_withServiceAdmin_succeeds() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.verifyRackerOrServiceAdminAccess(authToken);
        verify(authorizationService).authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void updateUserPasswordCredentials_withNullPassword_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername("username");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withNullUsername_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(null);
        creds.setPassword("foo");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withValidCredentials_callsUserService_updateUserMethod() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(userId);
        creds.setPassword("ABCdef123");
        spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        verify(userService).updateUser(user, false);
    }

    @Test
    public void addUser_callerIsUserAdmin_setsMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        users = mock(Users.class);
        List<User> usersList = new ArrayList();
        User tempUser = new User();
        tempUser.setId("1");
        tempUser.setUsername("tempUser");
        usersList.add(tempUser);
        users.setUsers(usersList);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>any())).thenReturn(users);
        when(config.getInt("numberOfSubUsers")).thenReturn(100);
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), equalTo("nastId"));
        assertThat("mosso id", argument.getValue().getMossoId(), equalTo(123));
    }

    @Test
    public void addUser_callerIsNotUserAdmin_doesNotSetMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), not(equalTo("nastId")));
        assertThat("mosso id", argument.getValue().getMossoId(), not(equalTo(123)));
    }

    @Test
    public void addUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUser_callsAddUserRole_whenCallerHasIdentityAdminRole() throws Exception {
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn(clientRole.getId());
        spy.addUser(null, null, authToken, userOS);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void addUser_callsClientServiceGetClientRoleByClientIdAndName_whenCallerHasIdentityAdminRole() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("roleId");
        spy.addUser(null, null, authToken, userOS);
        verify(clientService).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void addUser_withUserMissingUsername_returns400() throws Exception {
        userOS.setUsername(null);
        Response.ResponseBuilder responseBuilder = spy.addUser(null, null, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addUser_callsUserConverter_toUserDOMethod() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(userConverterCloudV20).toUserDO(userOS);
    }

    @Test
    public void addUser_callsUserService_addUserMethod() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(userService).addUser(user);
    }

    @Test
    public void addUser_callsSetDomainId() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).setDomainId(any(ScopeAccess.class), any(User.class));
    }

    @Test
    public void addUser_userPasswordNotNull_callsValidatePassword() throws Exception {
        userOS.setPassword("password");
        spy.addUser(null, null, authToken, userOS);
        verify(spy).validatePassword("password");
    }

    @Test
    public void addUser_adminRoleUserSizeGreaterThanNumSubUsersThrowsBadRequest_returns400() throws Exception {
        User caller = new User();
        users = mock(Users.class);
        List<User> userList = new ArrayList();
        User tempUser = new User();
        tempUser.setId("1");
        tempUser.setUsername("tempUser");
        userList.add(tempUser);
        users.setUsers(userList);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>any())).thenReturn(users);
        when(config.getInt("numberOfSubUsers")).thenReturn(-1);
        Response.ResponseBuilder responseBuilder = spy.addUser(null, null, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addUser_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        doNothing().when(spy).assignProperRole(eq(httpHeaders), eq(authToken), any(ScopeAccess.class), any(User.class));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        Response.ResponseBuilder responseBuilder = spy.addUser(httpHeaders, uriInfo, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUser_userServiceDuplicateException_returns409() throws Exception {
        doThrow(new DuplicateException()).when(userService).addUser(any(User.class));
        Response.ResponseBuilder responseBuilder = spy.addUser(null, null, authToken, userOS);
        assertThat("respone code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addUser_userServiceDuplicateUserNameException_returns409() throws Exception {
        doThrow(new DuplicateUsernameException()).when(userService).addUser(any(User.class));
        Response.ResponseBuilder responseBuilder = spy.addUser(null, null, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void deleteUser_callsUserService_softDeleteUserMethod() throws Exception {
        User user1 = new User();
        User caller = new User();
        user1.setDomainId("domainId");
        caller.setId("id");
        caller.setDomainId("domainId");
        when(userService.getUserById(userId)).thenReturn(user1);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        spy.deleteUser(null, authToken, userId);
        verify(userService).softDeleteUser(any(User.class));
    }

    @Test
    public void userDisabledExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("userName");
        JAXBElement<UserDisabledFault> jaxbElement = ((JAXBElement<UserDisabledFault>) (responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("userName"));
    }

    @Test
    public void userDisabledExceptionResponse_returns403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void tenantConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        JAXBElement<TenantConflictFault> jaxbElement = ((JAXBElement<TenantConflictFault>) (responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void tenantConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void userConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        JAXBElement<BadRequestFault> jaxbElement = ((JAXBElement<BadRequestFault>) (responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void userConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void exceptionResponse_withForbiddenException_403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new ForbiddenException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void exceptionResponse_withNotAuthenticatedException_401() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new NotAuthenticatedException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void deleteService_callsClientService_deleteMethod() throws Exception {
        spy.deleteService(null, authToken, "clientId");
        verify(clientService).delete("clientId");
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsTenantService_deleteTenantRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteRole_callsClientService_deleteClientRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, role.getId());
        verify(clientService).deleteClientRole(any(ClientRole.class));
    }

    @Test
    public void deleteRole_serviceAdminCallerDoesNotHaveAccess_throwsForbiddenException() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        Response.ResponseBuilder responseBuilder = spy.deleteRole(null, authToken, "roleId");
        assertThat("code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteRole_withNullRole_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.deleteRole(null, authToken, null);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_deleteBaseUrlMethod() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, "101");
        verify(endpointService).deleteBaseUrl(101);
    }

    @Test
    public void deleteEndpoint_callsTenantService_updateTenantMethod() throws Exception {
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void addUserRole_callsTenantService_addTenantRoleToUserMethod() throws Exception {
        spy.addUserRole(null, authToken, userId, tenantRole.getRoleRsId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addTenant_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.addTenant(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addTenant_callsTenantService_addTenant() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addTenant_callsTenantConverterCloudV20_toTenantDO() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
        verify(tenantConverterCloudV20).toTenantDO(tenantOS);
    }

    @Test
    public void addTenant_withNullTenantName_returns400() throws Exception {
        tenantOS.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addTenant(null, null, authToken, tenantOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addTenant_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        Tenant domainTenant = mock(Tenant.class);
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.v2.ObjectFactory.class);
        doReturn(domainTenant).when(tenantConverterCloudV20).toTenantDO(any(org.openstack.docs.identity.api.v2.Tenant.class));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        doReturn(tenantId).when(domainTenant).getTenantId();
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(tenantConverterCloudV20.toTenant(any(Tenant.class))).thenReturn(tenantOS);
        Response.ResponseBuilder responseBuilder = spy.addTenant(httpHeaders, uriInfo, authToken, tenantOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addTenant_tenantServiceDuplicateException_returns409() throws Exception {
        doThrow(new DuplicateException()).when(tenantService).addTenant(any(Tenant.class));
        Response.ResponseBuilder responseBuilder = spy.addTenant(null, null, authToken, tenantOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addService(null, null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addService_callsClientService_add() throws Exception {
        spy.addService(null, null, authToken, service);
        verify(clientService).add(any(Application.class));
    }

    @Test
    public void addService_withNullService_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_withNullServiceType_returns400() throws Exception {
        service.setType(null);
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_withNullName_returns400() throws Exception {
        service.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(objectFactory);
        Response.ResponseBuilder responseBuilder = spy.addService(httpHeaders, uriInfo, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addService_clientServiceDuplicateException_returns409() throws Exception {
        doThrow(new DuplicateException()).when(clientService).add(any(Application.class));
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        Role role1 = new Role();
        role1.setServiceId("id");
        spy.addRole(null, null, authToken, role1);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addRole_callsClientService_addClientRole() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.addRole(null, null, authToken, role);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void addRole_roleWithNullName_returns400() throws Exception {
        Role role1 = new Role();
        role1.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, role1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_nullRole_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_roleWithNullServiceId_returns400() throws Exception {
        Role role1 = new Role();
        role1.setName("roleName");
        role1.setServiceId(null);
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, role1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_responseCreated() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.v2.ObjectFactory.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(roleConverterCloudV20.toRoleFromClientRole(any(ClientRole.class))).thenReturn(role);
        Response.ResponseBuilder responseBuilder = spy.addRole(httpHeaders, uriInfo, authToken, role);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addRole_roleConflictThrowsDuplicateException_returns409() throws Exception {
        doThrow(new DuplicateException()).when(clientService).addClientRole(any(ClientRole.class));
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, role);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addRolesToUserOnTenant_callsTenantService_addTenantRoleToUser() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        spy.addRolesToUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant() throws Exception {
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant_throw400() throws Exception {
        cloudBaseUrl.setGlobal(true);
        Response.ResponseBuilder responseBuilder = spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void listUserGroups_withValidUser_returns200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_withValidUser_returnsNonNullEntity() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), Matchers.<Object>notNullValue());
    }

    @Test
    public void listUserGroups_withValidUser_returnsAJaxbElement() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), instanceOf(javax.xml.bind.JAXBElement.class));
    }

    @Test
    public void listTenants_invalidToken_returns401() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess("bad");
        when(scopeAccessService.getAccessTokenByAuthHeader("bad")).thenReturn(null);
        when(tenantConverterCloudV20.toTenantList(org.mockito.Matchers.<List<Tenant>>any())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listTenants(null, "bad", null, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void listTenants_callsUserLevelAccess() throws Exception {
        spy.listTenants(null, null, null, null);
        verify(spy).verifyUserLevelAccess(null);
    }

    @Test
    public void listTenants_scopeAccessSaNotNullResponseOk_returns200() throws Exception {
        when(scopeAccessService.getAccessTokenByAuthHeader(authToken)).thenReturn(new ScopeAccess());
        Response.ResponseBuilder responseBuilder = spy.listTenants(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpointTemplate(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addEndpointTemplate_callsEndpointService_addBaseUrl() throws Exception {
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(baseUrl);
        spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        verify(endpointService).addBaseUrl(baseUrl);
    }

    @Test
    public void addEndpointTemplate_returnsResponseCreated() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("101");
        org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory.class);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path("101");
        doReturn(uri).when(uriBuilder).build();
        when(endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl)).thenReturn(endpointTemplate);
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(objectFactory);
        Response.ResponseBuilder responseBuilder = spy.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addEndpointTemplate_endPointServiceThrowsBaseUrlConflictException_returns409() throws Exception {
        doThrow(new BaseUrlConflictException()).when(endpointService).addBaseUrl(any(CloudBaseUrl.class));
        Response.ResponseBuilder responseBuilder = spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addEndpointTemplate_endPointServiceThrowsDuplicateException_returns409() throws Exception {
        doThrow(new DuplicateException()).when(endpointService).addBaseUrl(any(CloudBaseUrl.class));
        Response.ResponseBuilder responseBuilder = spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addRoleToUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addRolesToUserOnTenant(null, authToken, null, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addUserCredential_mediaTypeXML_callsGetXMLCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, authToken, null, null);
        verify(spy).getXMLCredentials(null);
    }

    @Test
    public void addUserCredential_mediaTypeJSON_callsGetJSONCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, authToken, null, null);
        verify(spy).getJSONCredentials(null);
    }

    @Test
    public void addUserCredential_passwordCredentials_callsUserServiceUpdateUser() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(any(PasswordCredentialsRequiredUsername.class));
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(anyString());
        spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        verify(userService).updateUser(user, false);
    }

    @Test
    public void addUserCredential_passwordCredentialsUserCredentialNotMatchUserName_returns400() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("wrong_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(any(PasswordCredentialsRequiredUsername.class));
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addUserCredential_passwordCredentialOkResponseCreated_returns201() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(any(PasswordCredentialsRequiredUsername.class));
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.addUserRole(null, authToken, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUserRole_userNotIdentityAdminAndRoleIsIdentityAdmin_returns403() throws Exception {
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(config.getString("cloudAuth.adminRole")).thenReturn("admin");
        ClientRole clientRole1 = new ClientRole();
        clientRole1.setName("admin");
        doReturn(clientRole1).when(spy).checkAndGetClientRole(roleId);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        Response.ResponseBuilder responseBuilder = spy.addUserRole(null, authToken, null, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.checkToken(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void checkToken_belongsToNotBlank_callsTenantServiceGetTenantRolesForScopeAccess() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        spy.checkToken(null, authToken, "tokenId", "belongsTo");
        verify(tenantService).getTenantRolesForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void checkToken_bleongsToNotBlankThrowsNotFoundException_returns404() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(null);
        doReturn(false).when(spy).belongsTo("belongsTo", null);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_responseOk_returns200() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_throwsBadRequestException_returns400() throws Exception {
        doThrow(new BadRequestException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void checkToken_throwsNotAuthorizedException_returns401() throws Exception {
        doThrow(new NotAuthorizedException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_throwsForbiddenException_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_throwsNotFoundException_returns404() throws Exception {
        doThrow(new NotFoundException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_throwsException_returns500() throws Exception {
        doThrow(new NullPointerException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(500));
    }

    @Test
    public void deleteEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpoint(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteRole_callsVerifyIdentityAdminLevelAccess() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, roleId);
        verify(spy).verifyIdentityAdminLevelAccess(anyString());
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteService_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteTenant(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.deleteUser(null, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_userAdmin_differentDomain_throwsForbiddenException() throws Exception {
        when(userService.getUserById("dude")).thenReturn(new User());
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, authToken, "dude");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUserCredential_passwordCredentials_returns501() throws Exception {
        String credentialType = "passwordCredentials";
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, null, credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(501));
    }

    @Test
    public void deleteUserCredential_notPasswordCredentialAndNotAPIKEYCredential_returns400() throws Exception {
        String credentialType = "";
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, null, credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void deleteUserCredential_APIKEYCredential_callsCheckAndGetUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(spy).checkAndGetUser("userId");
    }

    @Test
    public void deleteUserCredential_APIKeyCredential_callsUserServiceUpdateUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        doReturn(new User()).when(spy).checkAndGetUser("userId");
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void deleteUserCredential_APIKeyCredentialResponseNoContent_returns204() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        doReturn(new User()).when(spy).checkAndGetUser("userId");
        doNothing().when(userService).updateUser(any(User.class), eq(false));
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, "userId", credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.deleteUserRole(null, authToken, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUserRole_callsTenantServiceGetGlobalRolesForUser() throws Exception {
        spy.deleteUserRole(null, authToken, userId, null);
        verify(tenantService).getGlobalRolesForUser(any(User.class));
    }

    @Test
    public void deleteUserRole_roleIsNull_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(null, authToken, userId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void deleteUserRole_callsScopeAccessService() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        spy.deleteUserRole(null, authToken, userId, "tenantRoleId");
        verify(scopeAccessService, times(2)).getScopeAccessByAccessToken(authToken);
    }

    @Test
    public void deleteUserRole_notCloudIdentityAdminAndAdminRoleThrowsForbiddenException_returns403() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        tenantRole.setName("identity:admin");
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteUserRole_callsTenantServiceDeleteGlobalRole() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        verify(tenantService).deleteGlobalRole(any(TenantRole.class));
    }

    @Test
    public void deleteUserRole_responseNoContent_returns204() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        doNothing().when(tenantService).deleteGlobalRole(any(TenantRole.class));
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void getEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpoint_throwsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getEndpoint(null, authToken, tenantId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getEndpoint_callsCheckAndGetEndPointTemplate() throws Exception {
        tenant.addBaseUrlId("endpointId");
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        spy.getEndpoint(null, authToken, tenantId, "endpointId");
        verify(spy).checkAndGetEndpointTemplate("endpointId");
    }

    @Test
    public void getEndpoint_responseOk_returns200() throws Exception {
        tenant.addBaseUrlId("endpointId");
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate("endpointId");
        Response.ResponseBuilder responseBuilder = spy.getEndpoint(httpHeaders, authToken, tenantId, "endpointId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpointTemplate_responseOk_returns200() throws Exception {
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate("endpointTemplateId");
        when(endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl)).thenReturn(endpointTemplate);
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getEndpointTemplate(httpHeaders, authToken, "endpointTemplateId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getRole_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getRole(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getRole_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getRole(httpHeaders, authToken, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getSecretQA_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getSecretQA(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getSecretQA_responseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getSecretQA(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getService_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getService_responseOk_returns200() throws Exception {
        doReturn(new Application()).when(spy).checkAndGetApplication("serviceId");
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        when(serviceConverterCloudV20.toService(any(Application.class))).thenReturn(new Service());
        Response.ResponseBuilder responseBuilder = spy.getService(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getTenantById_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantById(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantById_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getTenantById(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getTenantByName_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantByName(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantByName_tenantIsNullThrowsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getTenantByName(null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getTenantByName_responseOk_returns200() throws Exception {
        when(tenantService.getTenantByName(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.getTenantByName(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserById_callerDoesNotHaveDefaultUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        spy.getUserById(null, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void getUserById_callerHasUserAdminRole_callsVerifyDomain() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(this.userService.getUserById(any(String.class))).thenReturn(user);
        spy.getUserById(null, authToken, null);
        verify(spy).verifyDomain(any(User.class), any(User.class));
    }

    @Test
    public void getUserById_isCloudUserAndIdMatchesResponseOk_returns200() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserById_isCloudUserAndIdDoesNotMatchThrowForbiddenException_returns403() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void getUserById_responseOk_returns200() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserByName_callsVerifyUserLevelAccess() throws Exception {
        spy.getUserByName(null, authToken, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void getUserByName_userIsNullThrowsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getUserByName(null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserByName_callsScopeAccessServiceGetScopeAccessByAccessToken() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(scopeAccessService, times(2)).getScopeAccessByAccessToken(authToken);
    }

    @Test
    public void getUserByName_adminUser_callsVerifyDomain() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        doReturn(true).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(spy).verifyDomain(user, user);
    }

    @Test
    public void getUserByName_defaultUser_callsVerifySelf() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        doReturn(false).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        doReturn(true).when(spy).isDefaultUser(any(ScopeAccess.class), any(List.class));
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(spy).verifySelf(authToken, user);
    }

    @Test
    public void getUserByName_responseOk_returns200() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        doReturn(false).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        doReturn(false).when(spy).isDefaultUser(any(ScopeAccess.class), any(List.class));
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getUserByName(httpHeaders, authToken, "userName");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_callsVerifyUserLevelAccess() throws Exception {
        spy.getUserCredential(null, authToken, null, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void getUserCredential_notPasswordCredentialOrAPIKeyCredentialThrowsBadRequest_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(null, authToken, null, "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getUserCredential_cloudUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, apiKeyCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudUserIdNotMatchThrowsForbiddenException_returns403() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(null, authToken, "", apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void getUserCredential_cloudAdminUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, passwordCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudAdminUserIdNotMatchThrowsForbiddenException_returns403() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(null, authToken, "", passwordCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void getUserCredential_userIsNullThrowsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(null, authToken, "", apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserCredential_passwordCredentialUserPasswordIsBlank_returns404() throws Exception {
        when(userService.getUserById(userId)).thenReturn(new User());
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(null, authToken, userId, passwordCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserCredential_passwordCredentialResponseOk_returns200() throws Exception {
        user.setPassword("123");
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, passwordCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        when(userService.getUserById(userId)).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialAPIKeyIsBlank_returns404() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getUserRole(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserRole_roleIsNullThrowsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getUserRole(null, authToken, userId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserRole_responseOk_returns200() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        Response.ResponseBuilder responseBuilder = spy.getUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listCredentials(null, authToken, null, null, 0);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void listCredentials_userAdmin_callsVerifySelf() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        spy.listCredentials(null, authToken, userId, null, null);
        verify(spy).verifySelf(eq(authToken), any(User.class));
    }

    @Test
    public void listCredential_defaultUser_callsVerifySelf() throws Exception {
        doReturn(true).when(spy).isDefaultUser(any(ScopeAccess.class), any(List.class));
        spy.listCredentials(null, authToken, userId, null, null);
        verify(spy).verifySelf(eq(authToken), any(User.class));
    }

    @Test
    public void listCredential_userPasswordIsNotBlankResponseOk_returns200() throws Exception {
        user.setPassword("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userApiKeyIsNotBlankResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userPasswordIsBlankAndApiKeyIsBlankResponseOk_returns200() throws Exception {
        doReturn(user).when(spy).checkAndGetUser(userId);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userPasswordIsNotBlankAndApiKeyIsNotBlank_returns200() throws Exception {
        user.setApiKey("123");
        user.setPassword("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void isUserAdmin_tenantRolesIsNull_callsTenantService() throws Exception {
        spy.isUserAdmin(null, null);
        verify(tenantService).getTenantRolesForScopeAccess(null);
    }

    @Test
    public void isUserAdmin_returnsFalse() throws Exception {
        Boolean hasRole = spy.isUserAdmin(null, null);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void isUserAdmin_returnsTrue() throws Exception {
        tenantRole.setName("identity:user-admin");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isUserAdmin(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(true));
    }

    @Test
    public void isDefaultUser_tenantRolesIsNull_callsTenantService() throws Exception {
        spy.isDefaultUser(null, null);
        verify(tenantService).getTenantRolesForScopeAccess(null);
    }

    @Test
    public void isDefaultUser_returnsFalse() throws Exception {
        Boolean hasRole = spy.isUserAdmin(null, null);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void isDefaultUser_returnsTrue() throws Exception {
        tenantRole.setName("identity:default");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isDefaultUser(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(true));
    }

    @Test
    public void listEndpoints_verifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpoints(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNotNullResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNullResponseOk_returns200() throws Exception {
        tenant.setBaseUrlIds(null);
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointTemplates(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpointTemplates_serviceIdIsBlankResponseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listEndpointTemplates(httpHeaders, authToken, "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_serviceIdNotBlankResponseOk_returns200() throws Exception {
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        doReturn(new Application()).when(spy).checkAndGetApplication("serviceId");
        when(endpointService.getBaseUrlsByServiceId(anyString())).thenReturn(cloudBaseUrlList);
        Response.ResponseBuilder responseBuilder = spy.listEndpointTemplates(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listRoles(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRoles_serviceIdIsBlankResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRoles(httpHeaders, authToken, "", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_serviceIdNotBlankResponseOk_returns200() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(clientService.getClientRolesByClientId("serviceId")).thenReturn(roles);
        Response.ResponseBuilder responseBuilder = spy.listRoles(httpHeaders, authToken, "serviceId", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForTenant(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRolesForTenant(httpHeaders, authToken, tenantId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForUserOnTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForUserOnTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForUserOnTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listServices_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listServices(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listServices_responseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listServices(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_isUserAdmin_callsVerifyDomain() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).getUser(org.mockito.Matchers.any(ScopeAccess.class));
        when(userService.getUserById(anyString())).thenReturn(user);
        when(userService.getUser(anyString())).thenReturn(user);
        when(authorizationService.authorizeCloudIdentityAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifyDomain(org.mockito.Matchers.any(User.class), org.mockito.Matchers.any(User.class));
    }

    @Test
    public void listUserGlobalRoles_isDefaultUser_callsVerifySelf() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).getUser(org.mockito.Matchers.any(ScopeAccess.class));
        when(userService.getUserById(anyString())).thenReturn(user);
        when(userService.getUser(anyString())).thenReturn(user);
        when(authorizationService.authorizeCloudIdentityAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifySelf(anyString(), org.mockito.Matchers.any(User.class));
    }

    @Test
    public void listUserGlobalRoles_callsVerifyUserLevelAccess() throws Exception {
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalRoles_notCloudIdentityAdminAndNotCloudServiceAdminResponseOk_returns200() throws Exception {
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRolesByServiceId_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGlobalRolesByServiceId(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalRolesByServiceId_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGroups(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUsers_callerIsNotDefaultUser_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(false);
        spy.listUsers(null, authToken, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersForTenant_CallsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersForTenant(null, authToken, null, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersWithRoleForTenant(null, authToken, null, null, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void setUserEnabled_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.setUserEnabled(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateSecretQA_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateSecretQA(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUser_callsVerifyUserLevelAccess() throws Exception {
        spy.updateUser(null, authToken, null, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void updateUser_calls_userService_updateUserById() throws Exception {
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).updateUserById(any(User.class), anyBoolean());
    }

    @Test
    public void updateUser_callsAuthorizeCloudUser() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        spy.updateUser(null, authToken, null, new UserForCreate());
        verify(authorizationService).authorizeCloudUser(scopeAccess);
    }

    @Test
    public void updateUser_passwordNotNull_callsValidatePassword() throws Exception {
        userOS.setPassword("123");
        spy.updateUser(null, authToken, null, userOS);
        verify(spy).validatePassword("123");
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrue_callsUserServiceGetUserByAuthToken() throws Exception {
        doReturn(new User()).when(spy).checkAndGetUser(null);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, null, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrueIdNotMatch_returns403() throws Exception {
        doReturn(new User()).when(spy).checkAndGetUser(null);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.updateUser(null, authToken, null, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void updateUser_cloudUserAdminIsTrue_callsUserServiceGetUserByAuthToken() throws Exception {
        doReturn(new User()).when(spy).checkAndGetUser(null);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, null, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_cloudUserAdminIsTrue_callsVerifyDomain() throws Exception {
        doReturn(new User()).when(spy).checkAndGetUser(null);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.updateUser(null, authToken, null, userOS);
        verify(spy).verifyDomain(any(User.class), eq(user));
    }

    @Test
    public void updateUser_userDisabled_callsScopeAccessServiceExpiresAllTokensForUsers() throws Exception {
        User user = mock(User.class);
        doReturn(user).when(spy).checkAndGetUser(null);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user);
        doNothing().when(user).copyChanges(any(User.class));
        when(user.isDisabled()).thenReturn(true);
        spy.updateUser(null, authToken, null, userOS);
        verify(scopeAccessService).expireAllTokensForUser(user.getUsername());
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingUsername_throwsBadRequestException() throws Exception {
        defaultCloud20Service.validateUser(new org.openstack.docs.identity.api.v2.User());
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail2_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail3_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail4_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@bar.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail2_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("racker@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail3_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail4_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.\"elGuapo\".smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail5_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail6_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.net");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail7_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail8_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("R_a_c_K_e_r_4000@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void updateUserApiKeyCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserApiKeyCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUserPasswordCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserPasswordCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void validatePassword_ValidPassword_succeeds() throws Exception {
        defaultCloud20Service.validatePassword("Ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_LessThan8CharactersLong_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("123");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainUpperCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainLowerCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("AB345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainNumericCharacter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("Abcdefghik");
    }

    @Test
    public void setDomainId_callsAuthorizationService_authorizeCloudUserAdmin() throws Exception {
        ScopeAccess scopeAccessByAccessToken = new ScopeAccess();
        User userDO = new User();
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(authorizationService).authorizeCloudUserAdmin(scopeAccessByAccessToken);
    }

    @Test
    public void setDomainId_callerIsUserAdmin_callsUserService() throws Exception {
        User userDO = new User();
        userDO.setUsername("dude");
        Attribute[] attributes = {new Attribute(LdapRepository.ATTR_UID, "dude")};
        ScopeAccess scopeAccessByAccessToken = mock(ScopeAccess.class);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)).thenReturn(true);
        when(userService.getUser("dude")).thenReturn(userDO);
        when(scopeAccessByAccessToken.getLDAPEntry()).thenReturn(new SearchResultEntry("", attributes, new Control[]{}));
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(userService).getUser("dude");
    }

    @Test(expected = NotAuthorizedException.class)
    public void checkXAuthToken_withNullToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess(null);
    }

    @Test(expected = NotAuthorizedException.class)
    public void checkXAuthToken_withBlankToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess("");
    }

    @Test
    public void checkXAuthToken_identityAdminCaller_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test(expected = ForbiddenException.class)
    public void checkXAuthToken_userAdminCaller_throwsForbiddenException() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test
    public void validateToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.validateToken(null, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void listEndpointsForToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointsForToken(null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void listEndpointsForToken_responseOk_returns200() throws Exception {
        List<OpenstackEndpoint> openstackEndpointList = new ArrayList<OpenstackEndpoint>();
        doReturn(new ScopeAccess()).when(spy).checkAndGetToken("tokenId");
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class))).thenReturn(openstackEndpointList);
        when(endpointConverterCloudV20.toEndpointList(openstackEndpointList)).thenReturn(new EndpointList());
        Response.ResponseBuilder responseBuilder = spy.listEndpointsForToken(httpHeaders, authToken, "tokenId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listExtensions_currentExtensionsIsNullResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listExtensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listExtension_currentExtensionsNotNullResponseOk_returns200() throws Exception {
        JAXBContext jaxbContext = JAXBContextResolver.get();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
        StreamSource ss = new StreamSource(is);
        currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
        defaultCloud20Service.setCurrentExtensions(currentExtensions);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listExtensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addGroup_callsCloudGroupBuilder() throws Exception {
        CloudGroupBuilder cloudGrpBuilder = mock(CloudGroupBuilder.class);
        defaultCloud20Service.setCloudGroupBuilder(cloudGrpBuilder);
    }

    @Test
    public void addGroup_validGroup_returns201() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(cloudGroupBuilder.build(null)).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, uriInfo, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addGroup_duplicateGroup_returns409() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        when(cloudGroupBuilder.build((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group) any())).thenReturn(group);
        doThrow(new DuplicateException()).when(userGroupService).addGroup(org.mockito.Matchers.<Group>any());
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addGroup_emptyName_returns400() throws Exception {
        groupKs.setName("");
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void verifyTokenHasTenantAccess_callsTenantService() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        when(authorizationService.authorizeCloudIdentityAdmin((ScopeAccess) anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin((ScopeAccess) anyObject())).thenReturn(false);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, tenant1.getTenantId());
        verify(tenantService).getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyTokenHasTenantAccess_NoTenants_throwsException() throws Exception {
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(new ArrayList<Tenant>());
        when(authorizationService.authorizeCloudIdentityAdmin((ScopeAccess) anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin((ScopeAccess) anyObject())).thenReturn(false);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, null);
    }

    @Test
    public void verifyTokenHasTenantAccess_WithMatchingTenant_succeeds() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, tenant1.getTenantId());
    }

    @Test
    public void validateKsGroup_validGroup() {
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroup_throwsBadRequest() {
        groupKs.setName("");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void validateKsGroup_invalidGroupLength_throwsBadRequestMessage() {
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        try {
            defaultCloud20Service.validateKsGroup(groupKs);
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Group name length cannot exceed 200 characters"));

        }
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroupLength_throwsBadRequest() {
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void validateGroupId_validGroupId() {
        defaultCloud20Service.validateGroupId("1");
    }

    @Test
    public void validateGroupId_validGroupIdwithSpaces() {
        defaultCloud20Service.validateGroupId("  1   ");
    }

    @Test(expected = BadRequestException.class)
    public void validateGroupId_inValidGroupId() {
        defaultCloud20Service.validateGroupId("a");
    }

    @Test
    public void validateGroupId_inValidGroupId_throwBadRequest() {
        try {
            defaultCloud20Service.validateGroupId(" ");
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Invalid group id"));
        }
    }

    @Test
    public void validateGroupId_inValidGroupIdWithSpaces_throwBadRequest() {
        try {
            defaultCloud20Service.validateGroupId(" a ");
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Invalid group id"));
        }
    }

    @Test
    public void listUsers_callerIsUserAdmin_callsGetAllUsers() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        User user1 = new User();
        user1.setDomainId("testDomain");
        doReturn(user1).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        spy.listUsers(null, authToken, null, null);
        verify(userService).getAllUsers(any(FilterParam[].class), any(Integer.class), any(Integer.class));
    }

    @Test
    public void listUsers_callerIsServiceAdmin_callsGetAllUsers() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        spy.listUsers(null, authToken, null, null);
        verify(userService).getAllUsers(null, null, null);
    }

    @Test
    public void listUsers_callerIsDefaultUser_returns200() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUsers(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listGroupWithQueryParam_validName_returns200() throws Exception {
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(userGroupService.getGroupByName(org.mockito.Matchers.<String>anyObject())).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getGroup(null, authToken, "group1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setDomainId_callsAuthorizeCloudUserAdmin() throws Exception {
        defaultCloud20Service.setDomainId(null, null);
        verify(authorizationService).authorizeCloudUserAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudUserAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudUserAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudServiceAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudServiceAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudIdentityAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudIdentityAdmin(null);
    }

    @Test
    public void assignProperRole_cloudUserAdmin_callsAddUserRole() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void assignProperRole_cloudServiceAdmin_callsAddUserRole() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void assignProperRole_cloudIdentityAdmin_callsAddUserRole() throws  Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withEmptyString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withNullString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername(null);
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("first last");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString2_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername(" firstlast");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString3_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("firstlast ");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withTabContainingString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("first   last");
    }

    @Test
    public void validateApiKeyCredentials_validApiKey_noException() throws Exception {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setApiKey("1234568790");
        apiKeyCredentials.setUsername("test");
        defaultCloud20Service.validateApiKeyCredentials(apiKeyCredentials);
    }

    @Test(expected = BadRequestException.class)
    public void validateApiKeyCredentials_validApiKey_BadRequestException() throws Exception {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setApiKey("");
        apiKeyCredentials.setUsername("test");
        defaultCloud20Service.validateApiKeyCredentials(apiKeyCredentials);
    }

    @Test
    public void deleteTenant_validTenantAdminAndServiceAdmin_return204() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        //Current time plus 10 min
        scopeAccess.setAccessTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setRefreshTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setAccessTokenString("token");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        when(tenantService.getTenant(org.mockito.Matchers.<String>any())).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteTenant(null, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenant_validTenantUserAdmin_return403() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        //Current time plus 10 min
        scopeAccess.setAccessTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setRefreshTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setAccessTokenString("token");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteTenant(null, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteTenant_validTenantDefaultUser_return403() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        //Current time plus 10 min
        scopeAccess.setAccessTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setRefreshTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setAccessTokenString("token");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteTenant(null, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void getExtension_badAlias_throwsBadRequestException() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getExtension_ValidAlias_return200() throws Exception {
        org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
        when(jaxbObjectFactories.getOpenStackCommonV1Factory()).thenReturn(objectFactory);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "RAX-KSKEY");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getExtension_invalidAlias_return404() throws Exception {
        org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
        when(jaxbObjectFactories.getOpenStackCommonV1Factory()).thenReturn(objectFactory);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "bad");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getExtension_extensionMapNotNullResponseOk_returns200() throws Exception {
        extensionMap = mock(HashMap.class);
        defaultCloud20Service.setExtensionMap(extensionMap);
        when(extensionMap.containsKey(anyObject())).thenReturn(true);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "RAX-KSKEY");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getExtension_currentExtensionNotNullThrowsException_returns500() throws Exception {
        JAXBContext jaxbContext = JAXBContextResolver.get();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
        StreamSource ss = new StreamSource(is);
        currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
        defaultCloud20Service.setCurrentExtensions(currentExtensions);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "RAX-KSKEY");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(500));
    }

    @Test
    public void listUserGroups_noGroup_ReturnDefaultGroup() throws Exception {
        when(userGroupService.getGroupById(config.getInt(org.mockito.Matchers.<String>any()))).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listUserGroups(null, authToken, userId);
        assertThat("Default Group added", ((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups) ((JAXBElement) responseBuilder.build().getEntity()).getValue()).getGroup().get(0).getName(), equalTo("Group1"));
    }

    @Test
    public void verifySelf_callsUserService_getUserByScopeAccess() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        defaultCloud20Service.verifySelf("token", user1);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifySelf_differentUsername_throwsForbiddenException() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        User user2 = new User();
        user2.setId("foo");
        user2.setUsername("!foo");
        user2.setUniqueId("foo");
        defaultCloud20Service.verifySelf("token", user2);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifySelf_differentUniqueId_throwsForbiddenException() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        User user2 = new User();
        user2.setId("foo");
        user2.setUsername("foo");
        user2.setUniqueId("!foo");
        defaultCloud20Service.verifySelf("token", user2);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void deleteUserFromSoftDeleted_callsCheckXAUTHTOKEN() throws Exception {
        spy.deleteUserFromSoftDeleted(null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteUserFromSoftDeleted_callsCheckAndGetSoftDeletedUser() throws Exception {
        spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        verify(spy).checkAndGetSoftDeletedUser("userId");
    }

    @Test
    public void deleteUserFromSoftDeleted_callsUserServiceDeleteUser() throws Exception {
        doReturn(new User()).when(spy).checkAndGetSoftDeletedUser("userId");
        spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        verify(userService).deleteUser(any(User.class));
    }

    @Test
    public void deleteUserFromSoftDeleted_responseNoContent_returns204() throws Exception {
        doReturn(new User()).when(spy).checkAndGetSoftDeletedUser("userId");
        doNothing().when(userService).deleteUser(any(User.class));
        Response.ResponseBuilder responseBuilder = spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserFromSoftDeleted_throwsExceptionResponseBadRequest_returns400() throws Exception {
        doThrow(new BadRequestException()).when(spy).checkXAUTHTOKEN(authToken, true, null);
        Response.ResponseBuilder responseBuilder = spy.deleteUserFromSoftDeleted(null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }
}
