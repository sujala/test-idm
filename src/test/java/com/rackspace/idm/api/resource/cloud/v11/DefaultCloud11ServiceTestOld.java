package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.uri.UriBuilderImpl;
import com.sun.jersey.core.util.Base64;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mortbay.jetty.HttpHeaders;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 6:19 PM
 */
public class DefaultCloud11ServiceTestOld {

    AuthorizationService authorizationService;
    DefaultCloud11Service defaultCloud11Service;
    CredentialUnmarshaller credentialUnmarshaller;
    UserConverterCloudV11 userConverterCloudV11;
    UserValidator userValidator;
    NastFacade nastFacade;
    UserService userService;
    EndpointService endpointService;
    EndpointConverterCloudV11 endpointConverterCloudV11;
    Configuration config;
    UriInfo uriInfo;
    TenantService tenantService;
    ApplicationService clientService;
    AuthHeaderHelper authHeaderHelper;
    User user = new User();
    com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User();
    HttpServletRequest request;
    private ScopeAccessService scopeAccessService;
    UserScopeAccess userScopeAccess;
    ImpersonatedScopeAccess impersonatedScopeAccess;
    javax.ws.rs.core.HttpHeaders httpHeaders;
    CloudExceptionResponse cloudExceptionResponse;
    Application application = new Application("id",null,"myApp", null);
    AtomHopperClient atomHopperClient;
    GroupService userGroupService, cloudGroupService;
    DomainService domainService;
    AuthConverterCloudV11 authConverterCloudv11;
    CredentialValidator credentialValidator;
    private CloudContractDescriptionBuilder cloudContratDescriptionBuilder;
    Validator validator;
    Tenant tenant;

    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        authConverterCloudv11 = mock(AuthConverterCloudV11.class);
        authHeaderHelper = mock(AuthHeaderHelper.class);
        credentialUnmarshaller = mock(CredentialUnmarshaller.class);
        cloudExceptionResponse = new CloudExceptionResponse();
        userService = mock(UserService.class);
        httpHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        scopeAccessService = mock(ScopeAccessService.class);
        domainService = mock(DomainService.class);
        userScopeAccess = mock(UserScopeAccess.class);
        impersonatedScopeAccess = mock(ImpersonatedScopeAccess.class);
        endpointService = mock(EndpointService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        uriInfo = mock(UriInfo.class);
        tenantService = mock(TenantService.class);
        clientService = mock(ApplicationService.class);
        config = mock(Configuration.class);
        request = mock(HttpServletRequest.class);
        userValidator = mock(UserValidator.class);
        authorizationService = mock(AuthorizationService.class);
        atomHopperClient = mock(AtomHopperClient.class);
        userGroupService = mock(GroupService.class);
        cloudGroupService = mock(GroupService.class);
        credentialValidator = mock(CredentialValidator.class);
        cloudContratDescriptionBuilder = mock(CloudContractDescriptionBuilder.class);
        validator = mock(Validator.class);
        tenant = new Tenant();
        tenant.setName("tenant");
        tenant.setEnabled(true);
        tenant.setTenantId("1");
        tenant.getBaseUrlIds().clear();

        userDO.setId("1");
        userDO.setMossoId(1);
        userDO.setNastId("nastId");

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.fromUser(user)).thenReturn(user1);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(config.getString("serviceName.cloudServers")).thenReturn("cloudServers");
        when(config.getString("serviceName.cloudFiles")).thenReturn("cloudFiles");
        application.setOpenStackType("foo");
        Application testService = new Application(null, null, "testService", null);
        testService.setOpenStackType("foo");
        when(clientService.getByName(any(String.class))).thenReturn(testService);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultCloud11Service = new DefaultCloud11Service();
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
        defaultCloud11Service.setValidator(validator);
        defaultCloud11Service.setAuthorizationService(authorizationService);
        defaultCloud11Service.setAtomHopperClient(atomHopperClient);
        defaultCloud11Service.setCloudGroupService(cloudGroupService);
        defaultCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
        defaultCloud11Service.setCredentialValidator(credentialValidator);
        defaultCloud11Service.setCloudContractDescriptionBuilder(cloudContratDescriptionBuilder);
        defaultCloud11Service.setAuthHeaderHelper(authHeaderHelper);
        defaultCloud11Service.setConfig(config);
        defaultCloud11Service.setScopeAccessService(scopeAccessService);
        defaultCloud11Service.setEndpointService(endpointService);
        defaultCloud11Service.setUserService(userService);
        defaultCloud11Service.setAuthConverterCloudv11(authConverterCloudv11);
        defaultCloud11Service.setUserConverterCloudV11(userConverterCloudV11);
        defaultCloud11Service.setEndpointConverterCloudV11(endpointConverterCloudV11);
        defaultCloud11Service.setCloudExceptionResponse(cloudExceptionResponse);
        defaultCloud11Service.setApplicationService(clientService);
        defaultCloud11Service.setTenantService(tenantService);
        defaultCloud11Service.setDomainService(domainService);
    }

    @Test
    public void getVersion_callsCloudContractDescriptionBuilder() throws Exception {
        doReturn("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<version>1.0</version>").when(cloudContratDescriptionBuilder).buildVersion11Page();
        try {
            defaultCloud11Service.getVersion(null);
        } catch (Exception e){ }
        verify(cloudContratDescriptionBuilder).buildVersion11Page();
    }

    @Test
    public void authenticateResponse_withNastCredential_redirects() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials)).build();
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    @Test
    public void adminAuthenticateResponse_callsCredentialValidator_validateCredential() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(userService.getUserByTenantId(anyString())).thenReturn(user);
        defaultCloud11Service.adminAuthenticateResponse(null,new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials));
        verify(credentialValidator).validateCredential(nastCredentials, userService);
    }

    @Test
    public void validateMossoId_calls_UserService_getUsersByMossoId() throws Exception {
        defaultCloud11Service.validateMossoId(123);
        verify(userService).getUserByTenantId("123");
    }

    @Test
    public void validateMossoId_noUserExists_succeeds() throws Exception {
        when(userService.getUserByTenantId("123")).thenReturn(null);
        defaultCloud11Service.validateMossoId(123);
    }

    @Test(expected = BadRequestException.class)
    public void validateMossoId_UserExists_succeeds() throws Exception {
        when(userService.getUserByTenantId("123")).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.validateMossoId(123);
    }

    @Test
    public void authenticateResponse_withMossoCredentials_redirects() throws Exception {
        JAXBElement<MossoCredentials> credentials = new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,credentials).build();
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    @Test
    public void authenticateResponse_withUserCredentials_callsUserService_getUser() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("username");
        userCredentials.setKey("key");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        defaultCloud11Service.authenticateResponse(uriInfo ,credentials);
        verify(userService).getUser("username");
    }

    @Test
    public void authenticateResponse_withUserCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndApiCredentials() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("foo");
        userCredentials.setKey("key");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(uriInfo ,credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_redirects() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("pass");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,credentials).build();
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    @Test
    public void authenticateResponse_withUnknownCredentials_returns404Status() throws Exception {
        Credentials passwordCredentials = new Credentials() {
        };
        JAXBElement<Credentials> credentials =
                new JAXBElement<Credentials>(QName.valueOf("foo"), Credentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(uriInfo ,credentials);
        assertThat("Response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void adminAuthenticateResponse_withPasswordCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndPassword() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        userDO.setEnabled(true);
        when(userService.getUser("username")).thenReturn(userDO);
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(eq("username"), eq("password"), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentials_callsUserService_getUserByNastId() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(credentials.getValue()).thenReturn(new NastCredentials());
        when(userService.getUserByTenantId(anyString())).thenReturn(user);
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(userService).getUserByTenantId(null);
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByNastIdAndApiCredentials() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new NastCredentials());
        userDO.setEnabled(true);
        when(userService.getUserByTenantId(null)).thenReturn(userDO);
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(userService.getUserByTenantId(anyString())).thenReturn(user);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(userService).getUserByTenantId("12345");
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(eq(userDO.getUsername()), eq("apiKey"), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsEndpointService_getEndpointsForUser() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsAuthConverterCloudV11_toCloudv11AuthDataJaxb() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        verify(authConverterCloudv11).toCloudv11AuthDataJaxb(any(UserScopeAccess.class), anyList());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_returns200Status() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(null, credentials);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addNastTenant_withNastId_callsTenantService() throws Exception {
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(nastFacade.addNastUser(user)).thenReturn("nastId");
        user.setId("userId");
        user.setNastId("nastId");
        defaultCloud11Service.addNastTenant(user, userDO.getId());
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addMossoTenant_tenantServiceThrowsDuplicateException_exceptionGetsCaught() throws Exception {
        user.setMossoId(12345);
        doThrow(new DuplicateException("test exception")).when(tenantService).addTenant(any(Tenant.class));
        defaultCloud11Service.addMossoTenant(user, userDO.getId());
        verify(tenantService).addTenant(any(Tenant.class));
        verify(tenantService).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addNastTenant_callsEndpointService_getBaseUrlsByBaseUrlType() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(endpointService).getBaseUrlsByBaseUrlType("NAST");
    }

    @Test
    public void addNastTenant_whenNastEnabled_callsNastService() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(nastFacade).addNastUser(user1);
    }

    @Test
    public void addNastTenant_whenNastDisabled_DoesNotCallNastService() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(false);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(nastFacade,never()).addNastUser(user1);
    }

    @Test
    public void addNastTenant_whenNastDisabled_withEmptyNastId_DoesntCallTenantService() throws Exception {
        User user1 = new User();
        user1.setNastId(null);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(false);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(tenantService, never()).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addNastTenant_callsClientService_getClientRoleByClientIdAndRoleName() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(config.getString("serviceName.cloudFiles")).thenReturn("cloudFiles");
        when(clientService.getByName("cloudFiles")).thenReturn(application);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(clientService).getClientRoleByClientIdAndRoleName("id", "foo:default");
    }

    @Test
    public void addNastTenant_callsClientService_getClient() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(clientService).getByName("cloudFiles");
    }

    @Test
    public void addNastTenant_calls_tenantService_addTenantRoleToUser() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1, userDO.getId());
        verify(tenantService).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addMossoTenant_withNullMossoId_doesNothing() throws Exception {
        User user1 = new User();
        user1.setMossoId(null);
        defaultCloud11Service.addMossoTenant(user1, userDO.getId());
        verify(clientService, never()).getClientRoleByClientIdAndRoleName(anyString(), anyString());
        verify(userService, never()).getUserByTenantId(anyString());
    }

    @Test
    public void revokeToken_scopeAccessServiceReturnsNull_returnsNotFoundResponse() throws Exception, IOException {
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(null);
        DefaultCloud11Service spy1 = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doNothing().when(spy1, "authenticateCloudAdminUser", request);
        Response.ResponseBuilder returnedResponse = spy1.revokeToken(request, "test", null);
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    @PrepareForTest(DefaultCloud11Service.class)
    @Test
    public void revokeToken_scopeAccessServiceReturnsNonUserAccess_returnsNotFoundResponse() throws Exception, IOException {
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(new RackerScopeAccess());
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doNothing().when(tempSpy, "authenticateCloudAdminUser", request);
        Response.ResponseBuilder returnedResponse = tempSpy.revokeToken(request, "test", null);
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    @PrepareForTest(DefaultCloud11Service.class)
    @Test
    public void revokeToken_scopeAccessServiceReturnsExpiredToken_returnsNotFoundResponse() throws Exception, IOException {
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doNothing().when(tempSpy, "authenticateCloudAdminUser", request);
        UserScopeAccess userScopeAccessMock = mock(UserScopeAccess.class);
        when(userScopeAccessMock.isAccessTokenExpired(Matchers.<DateTime>anyObject())).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(userScopeAccessMock);
        Response.ResponseBuilder returnedResponse = tempSpy.revokeToken(request, "test", null);
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    @PrepareForTest(DefaultCloud11Service.class)
    @Test
    public void revokeToken_userScopeAccess_setAccessTokenExpired() throws Exception {
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doNothing().when(tempSpy, "authenticateCloudAdminUser", request);
        UserScopeAccess userScopeAccessMock = mock(UserScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(userScopeAccessMock);
        tempSpy.revokeToken(request, "test", null);
        verify(userScopeAccessMock).setAccessTokenExpired();
    }

    @PrepareForTest(DefaultCloud11Service.class)
    @Test
    public void revokeToken_userScopeAccess_updateScopeAccess() throws Exception {
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doNothing().when(tempSpy, "authenticateCloudAdminUser", request);
        UserScopeAccess userScopeAccessMock = mock(UserScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(userScopeAccessMock);
        tempSpy.revokeToken(request, "test", null);
        verify(scopeAccessService).updateScopeAccess(userScopeAccessMock);
    }

    @Test (expected = NotAuthorizedException.class)
    public void authenticateCloudAdminUserForGetRequests_callsAuthHeaderHelper_parseBasicParams_throwsCloudAdminAuthorizationException() throws Exception {
        doThrow(new CloudAdminAuthorizationException()).when(authHeaderHelper).parseBasicParams(anyString());
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
    }

    @Test(expected = NotAuthorizedException.class)
    public void authenticateCloudAdminUserForGetRequests_withNoBasicParams_throwsNotAuthorized() throws Exception {
        request = mock(HttpServletRequest.class);
        when(authHeaderHelper.parseBasicParams(any(String.class))).thenReturn(null);
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
    }

    @Test(expected = NotAuthorizedException.class)
    public void authenticateCloudAdminUserForGetRequests_withInvalidAuthHeaders() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + Base64.encode("auth"));
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void authenticateCloudAdminUserForGetRequests_withServiceAndServiceAdmin_withoutExceptions() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void authenticateCloudAdminUserForGetRequests_withService() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
        assertTrue("no Exception thrown", true);
    }

    @Test
    public void authenticateCloudAdminUserForGetRequests_withServiceAdmin() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
        assertTrue("no Exception thrown", true);
    }

    @Test
    public void createUser_withBaseURLRefs_callsEndpointService_addBaseUrlToUser() throws Exception {
        User user = new User();
        user.setId("someId");
        user.setMossoId(123456);
        user.setBaseURLRefs(new BaseURLRefList());
        user.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        userDO.setId("someId");
        userDO.setMossoId(123456);
    }

    @Test
    public void getAuthtokenFromRequest_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndPassword() throws Exception {
        defaultCloud11Service.getAuthtokenFromRequest(request);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(anyString(), anyString(), anyString());
    }

    @Test
    public void extensions_returns200() throws IOException{
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.extensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), org.hamcrest.Matchers.equalTo(200));
    }

    @Test
    public void extensions_withNonNullExtension_returns200() throws IOException{
        defaultCloud11Service.extensions(httpHeaders);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.extensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), org.hamcrest.Matchers.equalTo(200));
    }

    @Test
    public void getExtension_blankExtensionAlias_throwsBadRequestException() throws IOException{
        when(validator.isBlank(anyString())).thenReturn(true);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getExtension_withCurrentExtensions_throwsNotFoundException() throws IOException{  //There are no extensions at this time.
        defaultCloud11Service.extensions(httpHeaders);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "123");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));

    }

    @Test
    public void getExtension_withExtensionMap_throwsNotFoundException() throws IOException{
        defaultCloud11Service.getExtension(httpHeaders, "123");
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "123");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getExtension_invalidExtension_throwsNotFoundException() throws IOException{
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "INVALID");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Ignore //we have no extensions at the moment.
    @Test
    public void getExtension_withExtensions_addsAliasToExtensionMap() throws IOException{
        defaultCloud11Service.extensions(httpHeaders);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "123");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));

    }

    @Test(expected = CloudAdminAuthorizationException.class)
    public void authenticateCloudAdminUser_withInvalidAuthHeaders() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + Base64.encode("auth"));
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test(expected = CloudAdminAuthorizationException.class)
    public void authenticateCloudAdminUser_nullStringMap() throws Exception {
        when(authHeaderHelper.parseBasicParams(any(String.class))).thenReturn(null);
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test
    public void authenticateCloudAdminUser_withServiceAndServiceAdmin_withoutExceptions() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test(expected = CloudAdminAuthorizationException.class)
    public void authenticateCloudAdminUser_withServiceAndServiceAdminFalse() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test
    public void authenticateCloudAdminUser_withService() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud11Service.authenticateCloudAdminUser(request);
        assertTrue("no Exception thrown", true);
    }

    @Test
    public void authenticateCloudAdminUser_withServiceAdmin() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUser(request);
        assertTrue("no Exception thrown", true);
    }

}
