package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.mortbay.jetty.HttpHeaders;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 6:19 PM
 */
@RunWith(PowerMockRunner.class)
public class DefaultCloud11ServiceTest {

    AuthorizationService authorizationService;
    DefaultCloud11Service defaultCloud11Service;
    DefaultCloud11Service spy;
    UserConverterCloudV11 userConverterCloudV11;
    UserValidator userValidator;
    LdapCloudAdminRepository ldapCloudAdminRepository;
    NastFacade nastFacade;
    UserService userService;
    EndpointService endpointService;
    EndpointConverterCloudV11 endpointConverterCloudV11;
    Configuration config;
    UriInfo uriInfo;
    TenantService tenantService;
    ApplicationService clientService;
    User user = new User();
    com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User("userDO");
    HttpServletRequest request;
    private ScopeAccessService scopeAccessService;
    UserScopeAccess userScopeAccess;
    javax.ws.rs.core.HttpHeaders httpHeaders;
    CloudExceptionResponse cloudExceptionResponse;
    Application application = new Application("id",null,"myApp", null, null);
    AtomHopperClient atomHopperClient;

    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        ldapCloudAdminRepository = mock(LdapCloudAdminRepository.class);
        cloudExceptionResponse = new CloudExceptionResponse();
        when(ldapCloudAdminRepository.authenticate("auth", "auth123")).thenReturn(true);
        userService = mock(UserService.class);
        httpHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userScopeAccess = mock(UserScopeAccess.class);
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
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path("userId")).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.toUserDO(user)).thenReturn(user1);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(config.getString("serviceName.cloudServers")).thenReturn("cloudServers");
        when(config.getString("serviceName.cloudFiles")).thenReturn("cloudFiles");
        application.setOpenStackType("foo");
        Application testService = new Application(null, null, "testService", null, null);
        testService.setOpenStackType("foo");
        when(clientService.getByName(any(String.class))).thenReturn(testService);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultCloud11Service = new DefaultCloud11Service(config, scopeAccessService, endpointService, userService, null, userConverterCloudV11, endpointConverterCloudV11, ldapCloudAdminRepository, cloudExceptionResponse, clientService, tenantService);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
        defaultCloud11Service.setUserValidator(userValidator);
        defaultCloud11Service.setAuthorizationService(authorizationService);
        defaultCloud11Service.setAtomHopperClient(atomHopperClient);
        spy = spy(defaultCloud11Service);
    }

    @Test
    public void validateMossoId_calls_UserService_getUsersByMossoId() throws Exception {
        defaultCloud11Service.validateMossoId(123);
        verify(userService).getUserByMossoId(123);
    }

    @Test
    public void validateMossoId_noUserExists_succeeds() throws Exception {
        when(userService.getUserByMossoId(123)).thenReturn(null);
        defaultCloud11Service.validateMossoId(123);
    }

    @Test(expected = BadRequestException.class)
    public void validateMossoId_UserExists_succeeds() throws Exception {
        when(userService.getUserByMossoId(123)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.validateMossoId(123);
    }

    @Test
    public void getUserGroups_notAuthorized_returnsCorrectErrorMessage() throws Exception {
        doThrow(new NotAuthorizedException("You are not authorized to access this resource.")).when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        UnauthorizedFault entity = (UnauthorizedFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("message", entity.getMessage(), equalTo("You are not authorized to access this resource."));
    }

    @Test
    public void getUserGroups_notAuthorized_returnsCorrectErrorCode() throws Exception {
        doThrow(new NotAuthorizedException("You are not authorized to access this resource.")).when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        UnauthorizedFault entity = (UnauthorizedFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("code", entity.getCode(), equalTo(401));
    }

    @Test
    public void getUserGroups_notAuthorized_entityDetailsShouldMatchCloudResponse() throws Exception {
        doThrow(new NotAuthorizedException("You are not authorized to access this resource.")).when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        UnauthorizedFault entity = (UnauthorizedFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("code", entity.getDetails(), equalTo("AuthErrorHandler"));
    }

    @Test
    public void getUserGroups_withInvalidUser_returnsCorrectErrorMessage() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("message", entity.getMessage(), equalTo("User not found :testUser"));
    }

    @Test
    public void getUserGroups_withInvalidUser_returnsCorrectErrorCode() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("code", entity.getCode(), equalTo(404));
    }

    @Test
    public void getUserGroups_withInvalidUser_entityDetailsShouldBeNull() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("code", entity.getDetails(), equalTo(null));
    }

    @Test
    public void authenticateResponse_withNastCredentials_withEmptyUsername_returns400() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("");
        JAXBElement<NastCredentials> credentials = new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withEmptyUsername_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withNullPasswordAndUsername_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withCredentials_withNoApiKey_returns400() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withEmptyPassword_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withUserCredentials_withEmptyUsername_returns400() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withNastCredentials_callsUserService_getUserByNastId() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("nastId");
        JAXBElement<NastCredentials> credentials =
                new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByNastId("nastId");
    }

    @Test
    public void authenticateResponse_withNastCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByNastIdAndApiCredentials() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("nastId");
        JAXBElement<NastCredentials> credentials =
                new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByNastIdAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        JAXBElement<MossoCredentials> credentials =
                new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByMossoId(anyInt());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(1);
        JAXBElement<MossoCredentials> credentials =
                new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, mossoCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByMossoIdAndApiCredentials(anyInt(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withUserCredentials_callsUserService_getUser() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("username");
        userCredentials.setKey("key");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        defaultCloud11Service.authenticateResponse(credentials);
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
        defaultCloud11Service.authenticateResponse(credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_callsUserService_getUser() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("pass");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUser("username");
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndPassword() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("foo");
        passwordCredentials.setPassword("pass");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(anyString(), anyString(), anyString());
    }

    @Test
    public void addNastTenant_callsNastFacade() throws Exception {
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        user.setId("userId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        defaultCloud11Service.addNastTenant(user);
        Mockito.verify(nastFacade).addNastUser(user);
    }

    @Test
    public void addMossoTenant_withMossoId_callsTenantService() throws Exception {
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        user.setId("userId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        defaultCloud11Service.addMossoTenant(user);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addNastTenant_withNastId_callsTenantService() throws Exception {
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(nastFacade.addNastUser(user)).thenReturn("nastId");
        user.setId("userId");
        user.setNastId("nastId");
        defaultCloud11Service.addNastTenant(user);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addNastTenant_withNastId_callsEndpointService_getBaseUrlsByBaseUrlType() throws Exception {
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(nastFacade.addNastUser(user)).thenReturn("nastId");
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        users.setUsers(listUser);
        user.setId("userId");
        user.setNastId("nastId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        defaultCloud11Service.addNastTenant(user);
        verify(endpointService).getBaseUrlsByBaseUrlType("NAST");
    }

    @Test
    public void addMossoTenant_callsEndpointService_getBaseUrlsByBaseUrlType() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        user.setId("userId");
        user.setNastId("nastId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        defaultCloud11Service.addMossoTenant(user1);
        verify(endpointService).getBaseUrlsByBaseUrlType("MOSSO");
    }

    @Test
    public void addMossoTenant_callsTenantService() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        user.setId("userId");
        user.setNastId("nastId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        defaultCloud11Service.addMossoTenant(user1);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addNastTenant_callsTenantService() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        users.setUsers(listUser);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        defaultCloud11Service.addNastTenant(user1);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addNastTenant_tenantServiceThrowsDuplicateException_exceptionGetsCaught() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        users.setUsers(listUser);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        doThrow(new DuplicateException("test exception")).when(tenantService).addTenant(any(Tenant.class));
        defaultCloud11Service.addNastTenant(user1);
    }

    @Test
    public void addNastTenant_callsEndpointService_getBaseUrlsByBaseUrlType() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1);
        verify(endpointService).getBaseUrlsByBaseUrlType("NAST");
    }

    @Test
    public void addNastTenant_whenNastEnabled_callsNastService() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1);
        verify(nastFacade).addNastUser(user1);
    }

    @Test
    public void addNastTenant_whenNastDisabled_DoesNotCallNastService() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(false);
        defaultCloud11Service.addNastTenant(user1);
        verify(nastFacade,never()).addNastUser(user1);
    }

    @Test
    public void addNastTenant_callsClientService_getClientRoleByClientIdAndRoleName() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        when(config.getString("serviceName.cloudFiles")).thenReturn("cloudFiles");
        when(clientService.getByName("cloudFiles")).thenReturn(application);
        defaultCloud11Service.addNastTenant(user1);
        verify(clientService).getClientRoleByClientIdAndRoleName("id","foo:default");
    }

    @Test
    public void addMossoTenant_callsClientService_getClientRoleByClientIdAndRoleName() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(config.getString("serviceName.cloudServers")).thenReturn("cloudServers");
        when(clientService.getByName("cloudServers")).thenReturn(application);
        defaultCloud11Service.addMossoTenant(user1);
        verify(clientService).getClientRoleByClientIdAndRoleName("id","foo:default");
    }


    @Test
    public void addNastTenant_callsClientService_getClient() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1);
        verify(clientService).getByName("cloudFiles");
    }

    @Test
    public void addMossoTenant_callsClientService_getClient() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(config.getString("serviceName.cloudServers")).thenReturn("cloudServers");
        when(clientService.getByName("cloudServers")).thenReturn(application);
        defaultCloud11Service.addMossoTenant(user1);
        verify(clientService).getByName("cloudServers");
    }

    @Test
    public void addNastTenant_calls_tenantService_addTenantRoleToUser() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(nastFacade.addNastUser(user1)).thenReturn("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service.addNastTenant(user1);
        verify(tenantService).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addMossoTenant_calls_tenantService_addTenantRoleToUser() throws Exception {
        User user1 = new User();
        user1.setMossoId(123);
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        defaultCloud11Service.addMossoTenant(user1);
        verify(tenantService).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addMossoTenant_withMossoId_callsEndpointService_getBaseUrlsByBaseUrlType() throws Exception {
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        listUser.add(userDO);
        users.setUsers(listUser);
        user.setId("userId");
        user.setNastId("nastId");
        user.setMossoId(123);
        when(userService.getUsersByMossoId(123)).thenReturn(users);
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        defaultCloud11Service.addMossoTenant(user);
        Mockito.verify(endpointService).getBaseUrlsByBaseUrlType("MOSSO");
    }

    @Test
    public void authenticateResponse_usernameIsNull_returns400() throws Exception {
        JAXBElement<Credentials> cred = new JAXBElement<Credentials>(new QName(""), Credentials.class, new UserCredentials());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(cred);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Ignore
    @Test
    public void getUserGroups_notAuthorized_returns401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getUserGroups(request, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void revokeToken_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.revokeToken(request, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void authenticateResponse_nastCredentials_usesRetrievedUsersUsername() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("id");
        nastCredentials.setKey("key");
        JAXBElement<? extends Credentials> cred = new JAXBElement<Credentials>(QName.valueOf(""), Credentials.class, nastCredentials);
        when(userService.getUserByNastId("id")).thenReturn(new com.rackspace.idm.domain.entity.User("nastUser"));
        spy.authenticateResponse(cred);
        verify(endpointService).getEndpointsForUser("nastUser");
    }

    @Test
    public void authenticateResponse_mossoCredentials_usesRetrievedUsersUsername() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(123);
        mossoCredentials.setKey("key");
        JAXBElement<? extends Credentials> cred = new JAXBElement<Credentials>(QName.valueOf(""), Credentials.class, mossoCredentials);
        when(userService.getUserByMossoId(123)).thenReturn(new com.rackspace.idm.domain.entity.User("mossoUser"));
        spy.authenticateResponse(cred);
        verify(endpointService).getEndpointsForUser("mossoUser");
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

    @Test
    public void validateToken_isAdminCall_callAuthenticateCloudAdminUserForGetRequest() throws Exception {
        spy.validateToken(request, null, null, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void validateToken_withBadType_returnsBadRequest() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, null, "<inV@lid T*pe!", null);
        assertThat("validate token response", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void validateToken_withExpiredToken_returnsNotFound() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(new UserScopeAccess());
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, null, null, null);
        assertThat("validate token response", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_withCloudType_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        verify(userService).getUser("belongsTo");
    }

    @Test
    public void validateToken_withMossoType_callsUserService_getUserByMossoId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        spy.validateToken(null, null, "123", "MOSSO", null);
        verify(userService).getUserByMossoId(123);
    }

    @Test
    public void validateToken_withNastType_callsUserService_getUserByNastId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        spy.validateToken(null, null, "belongsTo", "NAST", null);
        verify(userService).getUserByNastId("belongsTo");
    }

    @Test
    public void validateToken_userServiceReturnsNullUser_returnsNotAuthorized() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        when(userService.getUser("belongsTo")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(401));
    }

    public void validateToken_userServiceReturnsDisabledUser_returnsForbidden() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        when(userDO.isEnabled()).thenReturn(false);
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(403));
    }

    public void validateToken_apiUsernameAndScopeAccessUsernameAreDifferent_returnsNotAuthorized() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(userScopeAccess.getUsername()).thenReturn("ScopeAccessUsername");
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        when(userDO.getUsername()).thenReturn("apiUsername");
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void adminAuthenticate_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.adminAuthenticate(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void addBaserUrlRef_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.addBaseURLRef(request, null, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void createUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.createUser(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void deleteBaseUrlRef_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.deleteBaseURLRef(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void deleteBaseUrlRef_userServiceReturnsNull_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        Response.ResponseBuilder responseBuilder = spy.deleteBaseURLRef(null, null, null, null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void deleteBaseUrlRef_withValidUserId_callsEndpointService_getBaseUrlRefById() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(endpointService).getBaseUrlById(anyInt());
    }

    @Test
    public void deleteBaseUrlRef_withNullBaseUrl_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getBaseUrlById(12345)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.deleteBaseURLRef(null, null, "12345", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));

    }

    @Test
    public void deleteBaseUrlRef_withValidData_callsEndpointService_removeBaseUrlFromUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(endpointService).removeBaseUrlFromUser(12345, null);
    }

    @Test
    public void deleteBaseUrlRef_withValidData_returnsStatus204() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        Response.ResponseBuilder responseBuilder = spy.deleteBaseURLRef(null, null, "12345", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.deleteUser(request, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void deleteUser_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        spy.deleteUser(null, null, null);
        verify(userService).getUser(null);
    }

    @Test
    public void deleteUser_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, null, null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void deleteUser_withValidUser_callsUserService_softDeleteUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        spy.deleteUser(null, null, null);
        verify(userService).softDeleteUser(userDO);
    }

    @Test
    public void deleteUser_withValidUser_callsAtomHopperClient_postUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        spy.deleteUser(null, null, null);
        verify(atomHopperClient).postUser(eq(userDO), anyString(), eq("deleted"));
    }

    @Test
    public void deleteUser_withValidUser_returnsResponseStatus204() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        doNothing().when(atomHopperClient).postUser(eq(userDO), anyString(), eq("deleted"));
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void getBaseUrlRef_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLRef(request, null, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseUrlRef_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        spy.getBaseURLRef(null, "userId", null, null);
        verify(userService).getUser("userId");
    }

    @Test
    public void getBaseUrlRef_nullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(null, null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseUrlRef_withValidUser_callsEndpointService_getEndpointForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getBaseURLRef(null, null, "0", null);
        verify(endpointService).getEndpointForUser(null, 0);
    }

    @Test
    public void getBaseUrlRef_withNullEndpointForUser_returnsNotFoundStatus() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getEndpointForUser(anyString(), anyInt())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(null, null, "0", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseUrlRef_withValidData_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getEndpointForUser(anyString(), anyInt())).thenReturn(new CloudEndpoint());
        when(endpointConverterCloudV11.toBaseUrlRef(any(CloudEndpoint.class))).thenReturn(new BaseURLRef());
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(null, null, "0", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getBaseUrlRefs_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLRefs(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseUrlRefs_isAdminCall_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        spy.getBaseURLRefs(null, null, null);
        verify(userService).getUser(null);
    }

    @Test
    public void getBaseUrlRefs_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRefs(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseUrlRefs_withValidUser_callsConfig() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getBaseURLRefs(null, null, null);
        verify(config).getString("cloudAuth.clientId");
    }

    @Test
    public void getBaseUrlRefs_withValidUser_callsScopeAccessService_getUserScopeAccessForClientId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getBaseURLRefs(null, null, null);
        verify(scopeAccessService).getUserScopeAccessForClientId(anyString(), anyString());
    }

    @Test
    public void getBaseUrlRefs_withValidUser_callsScopeAccessService_getOpenstackEndpointsForScopeAccess() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getBaseURLRefs(null, null, null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getBaseUrlRefs_withValidUser_endpointConverterCloudV11_openstackToBaseUrlRefs() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getBaseURLRefs(null, null, null);
        verify(endpointConverterCloudV11).openstackToBaseUrlRefs(any(List.class));
    }

    @Test
    public void getBaseUrlRefs_withValidUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointConverterCloudV11.openstackToBaseUrlRefs(any(List.class))).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRefs(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getServiceCatalog_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getServiceCatalog(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUser(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserEnabled_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserEnabled(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserFromMossoId_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserFromMossoId(request, 0, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void updateUser_callsValidateUser() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        spy.updateUser(request, null, null, null);
        verify(userValidator).validate(null);
    }

    @Test
    public void updateUser_whenValidatorThrowsBadRequestException_returns400() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        doThrow(new BadRequestException("test exception")).when(userValidator).validate(null);
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, null, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getUserFromNastId_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserFromNastId(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Ignore
    @Test
    public void getUserGroups_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserGroups(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserKey_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserKey(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void setUserEnabled_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.setUserEnabled(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void setUserKey_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.setUserKey(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void updateUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.updateUser(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void getBaseURLId_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLId(request, 0, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseURLs_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLs(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getEnabledBaseURL_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getEnabledBaseURL(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void addBaseURL_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.addBaseURL(request, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void authAdmin_withPasswordCredentials_withInvalidUser_returns401() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType());
        String credentials = "<passwordCredentials password=\"123\" username=\"IValidUser\" xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"/>";
        Response.ResponseBuilder responseBuilder = spy.adminAuthenticate(request, null, httpHeaders, credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void createUser_validMossoId_callValidateMossoId() throws Exception{
        user.setId("1");
        user.setMossoId(123456);
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        spy.createUser(request,httpHeaders,uriInfo,user);
        verify(spy).validateMossoId(123456);
    }

    @Test
    public void createUser_usernameAlreadyExists_correctMessage() throws Exception{
        user.setId("username");
        user.setMossoId(123456);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("username")).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        UsernameConflictFault conflictFault =(UsernameConflictFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("message", conflictFault.getMessage(), equalTo("Username username already exists"));
    }

    @Test
    public void createUser_usernameAlreadyExists_correctCode409() throws Exception{
        user.setId("username");
        user.setMossoId(123456);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("username")).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        UsernameConflictFault conflictFault =(UsernameConflictFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("message", conflictFault.getCode(), equalTo(409));
    }

    @Test
    public void createUser_usernameAlreadyExists_noDetails() throws Exception{
        user.setId("username");
        user.setMossoId(123456);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("username")).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        UsernameConflictFault conflictFault =(UsernameConflictFault)((JAXBElement)responseBuilder.build().getEntity()).getValue();
        assertThat("details", conflictFault.getDetails(), equalTo(null));
    }

    @Test
    public void createUser_VerifyUserAdminRoleIsAdded() throws Exception{
        Users users = new Users();
        List<com.rackspace.idm.domain.entity.User> listUser = new ArrayList();
        users.setUsers(listUser);
        user.setId("1");
        user.setMossoId(123456);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("7");
        clientRole.setName("identity:user-admin");
        when(authorizationService.authorizeCloudIdentityAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(userService.getUsersByMossoId(123456)).thenReturn(users);
        when(clientService.getClientRoleByClientIdAndRoleName(Matchers.<String>any(), Matchers.<String>any())).thenReturn(clientRole);
        when(clientService.getClientRoleById(Matchers.<String>any())).thenReturn(clientRole);
        doNothing().when(spy).addMossoTenant(any(User.class));
        doReturn("nastId").when(spy).addNastTenant(any(User.class));
        spy.createUser(request,httpHeaders,uriInfo,user);
        verify(tenantService).addTenantRoleToUser(Matchers.<com.rackspace.idm.domain.entity.User>any(), Matchers.<TenantRole>any());
    }

    @Test
    public void createUser_withBlankId_returnsBadRequest() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        User user = new User();
        Response.ResponseBuilder responseBuilder = spy.createUser(null, null, null, user);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void createUser_withoutMossId_returnsBadRequest() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        User user = new User();
        user.setId("someId");
        Response.ResponseBuilder responseBuilder = spy.createUser(null, null, null, user);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void createUser_withBaseURLRefs_callsEndpointService_addBaseUrlToUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        User user = new User();
        user.setId("someId");
        user.setMossoId(12345);
        user.setBaseURLRefs(new BaseURLRefList());
        user.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(userService.getUser(anyString())).thenReturn(null);
        when(userConverterCloudV11.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        doNothing().when(spy).validateMossoId(anyInt());
        doNothing().when(userService).addUser(any(com.rackspace.idm.domain.entity.User.class));
        doNothing().when(userService).updateUser(any(com.rackspace.idm.domain.entity.User.class), anyBoolean());
        when(clientService.getClientRoleById(null)).thenReturn(new ClientRole());
        spy.createUser(null, null, null, user);
        verify(endpointService).addBaseUrlToUser(anyInt(), anyBoolean(), anyString());
    }

    @Test
    public void createUser_endpointServiceHasDefaultBaseUrls_callsEndpointService_addBaseUrlToUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        User user = new User();
        user.setId("someId");
        user.setMossoId(12345);
        when(userService.getUser(anyString())).thenReturn(null);
        when(userConverterCloudV11.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(clientService.getClientRoleById(null)).thenReturn(new ClientRole());
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setBaseUrlId(123);
        baseUrls.add(cloudBaseUrl);
        when(endpointService.getDefaultBaseUrls()).thenReturn(baseUrls);
        spy.createUser(null, null, null, user);
        verify(endpointService).addBaseUrlToUser(anyInt(), anyBoolean(), anyString());
    }
}
