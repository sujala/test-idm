package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mortbay.jetty.HttpHeaders;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
    CredentialUnmarshaller credentialUnmarshaller;
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
    com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User("userId");
    HttpServletRequest request;
    private ScopeAccessService scopeAccessService;
    UserScopeAccess userScopeAccess;
    javax.ws.rs.core.HttpHeaders httpHeaders;
    CloudExceptionResponse cloudExceptionResponse;
    Application application = new Application("id",null,"myApp", null, null);
    AtomHopperClient atomHopperClient;
    GroupService userGroupService, cloudGroupService;
    AuthConverterCloudV11 authConverterCloudv11;

    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        authConverterCloudv11 = mock(AuthConverterCloudV11.class);
        ldapCloudAdminRepository = mock(LdapCloudAdminRepository.class);
        credentialUnmarshaller = mock(CredentialUnmarshaller.class);
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
        userGroupService = mock(GroupService.class);
        cloudGroupService = mock(GroupService.class);
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
        defaultCloud11Service = new DefaultCloud11Service(config, scopeAccessService, endpointService, userService, authConverterCloudv11, userConverterCloudV11, endpointConverterCloudV11, ldapCloudAdminRepository, cloudExceptionResponse, clientService, tenantService);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
        defaultCloud11Service.setUserValidator(userValidator);
        defaultCloud11Service.setAuthorizationService(authorizationService);
//        defaultCloud11Service.setAtomHopperClient(atomHopperClient);
        defaultCloud11Service.setCloudGroupService(cloudGroupService);
        defaultCloud11Service.setUserGroupService(userGroupService);
        defaultCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
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
    public void adminAuthenticateResponse_withUserCredentials_redirectsToCloudAuth() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(credentials.getValue()).thenReturn(new UserCredentials());
        defaultCloud11Service.adminAuthenticateResponse(credentials, response);
        verify(response).sendRedirect("cloud/auth");
    }

    @Test
    public void adminAuthenticateResponse_withPasswordCredentialsAndBlankPassword_returnBadRequestResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticateResponse_withPasswordCredentialsAndBlankUsername_returnBadRequestResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticateResponse_withPasswordCredentialsAndNullUserFromUserService_returnNotAuthorizedResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        when(userService.getUser("username")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void adminAuthenticateResponse_withPasswordCredentialsAndDisabledUserFromUserService_returnForbiddenResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        userDO.setEnabled(false);
        when(userService.getUser("username")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(403));
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
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(eq("username"), eq("password"), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentials_callsUserService_getUserByNastId() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new NastCredentials());
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(userService).getUserByNastId(null);
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentialsAndNullUser_returnNotFoundResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new NastCredentials());
        when(userService.getUserByNastId(null)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentialsAndDisabledUser_returnForbiddenResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new NastCredentials());
        userDO.setEnabled(false);
        when(userService.getUserByNastId(null)).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void adminAuthenticateResponse_withNastCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByNastIdAndApiCredentials() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new NastCredentials());
        userDO.setEnabled(true);
        when(userService.getUserByNastId(null)).thenReturn(userDO);
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByNastIdAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentialsAndNullApiKey_returnsBadRequestResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        when(credentials.getValue()).thenReturn(new MossoCredentials());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentialsAndZeroLengthApiKey_returnsBadRequestResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("");
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(userService).getUserByMossoId(12345);
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentialsAndNullUser_returnsNotFoundResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(userService.getUserByMossoId(anyInt())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentialsAndDisabledUser_returnsForbiddenResponse() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByMossoId(anyInt())).thenReturn(userDO);
        userDO.setEnabled(false);
        userDO.setMossoId(12345);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByMossoId(anyInt())).thenReturn(userDO);
        userDO.setEnabled(true);
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByMossoIdAndApiCredentials(eq(12345), eq("apiKey"), anyString());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsEndpointService_getEndpointsForUser() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByMossoId(anyInt())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByMossoIdAndApiCredentials(12345, "apiKey", null)).thenReturn(new UserScopeAccess());
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(endpointService).getEndpointsForUser(anyString());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_callsAuthConverterCloudV11_toCloudv11AuthDataJaxb() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByMossoId(anyInt())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByMossoIdAndApiCredentials(12345, "apiKey", null)).thenReturn(new UserScopeAccess());
        when(endpointService.getEndpointsForUser(anyString())).thenReturn(new ArrayList<CloudEndpoint>());
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(authConverterCloudv11).toCloudv11AuthDataJaxb(any(UserScopeAccess.class), anyList());
    }

    @Test
    public void adminAuthenticateResponse_withMossoCredentials_returns200Status() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByMossoId(anyInt())).thenReturn(userDO);
        userDO.setEnabled(true);
        when(scopeAccessService.getUserScopeAccessForClientIdByMossoIdAndApiCredentials(12345, "apiKey", null)).thenReturn(new UserScopeAccess());
        when(endpointService.getEndpointsForUser(anyString())).thenReturn(new ArrayList<CloudEndpoint>());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
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
    public void addMossoTenant_tenantServiceThrowsDuplicateException_exceptionGetsCaught() throws Exception {
        user.setMossoId(12345);
        doThrow(new DuplicateException("test exception")).when(tenantService).addTenant(any(Tenant.class));
        defaultCloud11Service.addMossoTenant(user);
        verify(tenantService).addTenant(any(Tenant.class));
        verify(tenantService).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
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


    @Test
    public void revokeToken_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
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

    @Test
    public void validateToken_userServiceReturnsDisabledUser_returnsForbidden() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        userDO.setEnabled(false);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void validateToken_apiUsernameAndScopeAccessUsernameAreDifferent_returnsNotAuthorized() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(userScopeAccess.getUsername()).thenReturn("ScopeAccessUsername");
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        userDO.setEnabled(true);
        userDO.setUsername("apiUsername");
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void validateToken_withValidData_callsAuthConverterCloudV11_toCloudV11TokenJaxb() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(userScopeAccess.getUsername()).thenReturn("username");
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        userDO.setEnabled(true);
        userDO.setUsername("username");
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        when(request.getRequestURL()).thenReturn(new StringBuffer("requestUrl/token/1"));
        spy.validateToken(request, null, "belongsTo", "CLOUD", null);
        verify(authConverterCloudv11).toCloudV11TokenJaxb(eq(userScopeAccess), anyString());
    }

    @Test
    public void validateToken_withValidData_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(userScopeAccess.getUsername()).thenReturn("username");
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        userDO.setEnabled(true);
        userDO.setUsername("username");
        when(userService.getUser("belongsTo")).thenReturn(userDO);
        when(request.getRequestURL()).thenReturn(new StringBuffer("requestUrl/token/1"));
        Response.ResponseBuilder responseBuilder = spy.validateToken(request, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void adminAuthenticate_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.adminAuthenticate(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void adminAuthenticate_mediaTypeIsNull_callsAuthenticateJSON() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.adminAuthenticate(request, null, httpHeaders, null);
        verify(spy).authenticateJSON(null, httpHeaders, null, true);
    }

    @Test
    public void adminAuthenticate_mediaTypeIsNotNullAndNotXML_callsAuthenticateJSON() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        MediaType mediaType = mock(MediaType.class);
        when(mediaType.isCompatible(any(MediaType.class))).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        spy.adminAuthenticate(request, null, httpHeaders, null);
        verify(spy).authenticateJSON(null, httpHeaders, null, true);
    }

    @Test
    public void adminAuthenticate_mediaTypeIsNullAndIsXML_callsAuthenticateXML() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        MediaType mediaType = mock(MediaType.class);
        when(mediaType.isCompatible(any(MediaType.class))).thenReturn(true);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        spy.adminAuthenticate(request, null, httpHeaders, null);
        verify(spy).authenticateXML(null, httpHeaders, null, true);
    }

    @Test
    public void authenticate_mediaTypeIsNull_callsAuthenticateJSON() throws Exception {
        spy.authenticate(request, null, httpHeaders, null);
        verify(spy).authenticateJSON(null, httpHeaders, null, false);
    }

    @Test
    public void authenticate_mediaTypeIsNotNullAndNotXML_callsAuthenticateJSON() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(mediaType.isCompatible(any(MediaType.class))).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        spy.authenticate(request, null, httpHeaders, null);
        verify(spy).authenticateJSON(null, httpHeaders, null, false);
    }

    @Test
    public void authenticate_mediaTypeIsNullAndIsXML_callsAuthenticateXML() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(mediaType.isCompatible(any(MediaType.class))).thenReturn(true);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        spy.authenticate(request, null, httpHeaders, null);
        verify(spy).authenticateXML(null, httpHeaders, null, false);
    }

    @Test
    public void authenticateJSON_callsCredentialUnmarshaller_unmarshallCredentialsFromJSON() throws Exception {
        doReturn(null).when(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
        spy.authenticateJSON(null, httpHeaders, "jsonBody", true);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON("jsonBody");
    }

    @Test
    public void authenticateJSON_isAdmin_callsAdminAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
        spy.authenticateJSON(null, httpHeaders, "jsonBody", true);
        verify(spy).adminAuthenticateResponse(null, null);
    }

    @Test
    public void authenticateJSON_isNotAdmin_callsAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).authenticateResponse(any(JAXBElement.class));
        spy.authenticateJSON(null, httpHeaders, "jsonBody", false);
        verify(spy).authenticateResponse(null);
    }

    @Test
    public void authenticateXML_isAdmin_callsAdminAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
        spy.authenticateXML(null, httpHeaders, "<xmlBody/>", true);
        verify(spy).adminAuthenticateResponse(null, null);
    }

    @Test
    public void authenticateXML_isNotAdmin_callsAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).authenticateResponse(any(JAXBElement.class));
        spy.authenticateXML(null, httpHeaders, "<xmlBody/>", false);
        verify(spy).authenticateResponse(null);
    }

    @Test
    public void addBaseUrlRef_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.addBaseURLRef(request, null, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void addBaseUrlRef_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.addBaseURLRef(request, "userId", null, null, null);
        verify(userService).getUser("userId");
    }

    @Test
    public void addBaseUrlRef_withNullUser_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void addBaseUrlRef_withValidUser_callsEndpointService_getBaseUrlById() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.addBaseURLRef(request, "userId", null, null, new BaseURLRef());
        verify(endpointService).getBaseUrlById(anyInt());
    }

    @Test
    public void addBaseUrlRef_withNullBaseUrl_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", null, null, new BaseURLRef());
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void addBaseUrlRef_withDisabledBaseUrl_returnsBadRequestResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(cloudBaseUrl.getEnabled()).thenReturn(false);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", null, null, new BaseURLRef());
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addBaseUrlRef_validCloudBaseUrl_callsEndpointService_addBaseUrlToUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(cloudBaseUrl.getEnabled()).thenReturn(true);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        spy.addBaseURLRef(request, "userId", null, null, new BaseURLRef());
        verify(endpointService).addBaseUrlToUser(anyInt(), anyBoolean(), anyString());
    }

    @Test
    public void addBaseUrlRef_validCloudBaseUrl_returns201Status() throws Exception {
        BaseURLRef baseUrlRef = mock(BaseURLRef.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(cloudBaseUrl.getEnabled()).thenReturn(true);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", null, uriInfo, baseUrlRef);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(201));
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
    public void deleteUser_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
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
//TODO
//    @Test
//    public void deleteUser_withValidUser_callsAtomHopperClient_postUser() throws Exception {
//        doNothing().when(spy).authenticateCloudAdminUser(null);
//        when(userService.getUser(null)).thenReturn(userDO);
//        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
//        spy.deleteUser(null, null, null);
//        verify(atomHopperClient).postUser(eq(userDO), anyString(), eq("deleted"));
//    }

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
    public void getBaseUrlRef_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
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
    public void getBaseUrlRefs_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
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
    public void getServiceCatalog_isAdminCall_authenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getServiceCatalog(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getServiceCatalog_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        spy.getServiceCatalog(null, null, null);
        verify(userService).getUser(null);
    }

    @Test
    public void getServiceCatalog_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getServiceCatalog(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getServiceCatalog_withValidUser_callsEndpointService_getEndpointsForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getServiceCatalog(null, null, null);
        verify(endpointService).getEndpointsForUser(null);
    }

    @Test
    public void getServiceCatalog_withValidUser_callsEndpointConverterCloudV11_toServiceCatalog() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        spy.getServiceCatalog(null, null, null);
        verify(endpointConverterCloudV11).toServiceCatalog(any(List.class));
    }

    @Test
    public void getServiceCatalog_withValidUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.getServiceCatalog(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUser_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUser(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUser_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        spy.getUser(null, "userId", null);
        verify(userService).getUser("userId");
    }

    @Test
    public void getUser_userIsNull_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        Response.ResponseBuilder responseBuilder = spy.getUser(null, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUser_validUser_callsScopeAccessService_getUserScopeAccessForClientId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.getUser(null, "userId", null);
        verify(scopeAccessService).getUserScopeAccessForClientId(anyString(), anyString());
    }

    @Test
    public void getUser_validUser_callsScopeAccessService_getOpenstackEndpointsForScopeAccess() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        spy.getUser(null, "userId", null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(userScopeAccess);
    }

    @Test
    public void getUser_validUser_callsUserConverterCloudV11_openstackToCloudV11User() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.getUser(null, "userId", null);
        verify(userConverterCloudV11).openstackToCloudV11User(eq(userDO), any(List.class));
    }

    @Test
    public void getUser_validUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUser(null, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));

    }


    @Test
    public void getUserEnabled_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUserEnabled(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserEnabled_isAdminCall_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        spy.getUserEnabled(null, "userId", null);
        verify(userService).getUser("userId");
    }

    @Test
    public void getUserEnabled_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserEnabled(null, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserEnabled_withValidUser_callsUserConverterCloudV11_toCloudV11UserWithOnlyEnabled() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.getUserEnabled(null, "userId", null);
        verify(userConverterCloudV11).toCloudV11UserWithOnlyEnabled(userDO);
    }

    @Test
    public void getUserEnabled_withValidUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUserEnabled(null, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserFromMossoId_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUserFromMossoId(request, 0, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserFromMossoId_isAdminCall_callsUserService_getUserByMossoId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getUserFromMossoId(request, 12345, null);
        verify(userService).getUserByMossoId(12345);
    }

    @Test
    public void getUserFromMossoId_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUserByMossoId(12345)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserFromMossoId(request, 12345, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserFromMossoId_withValidUser_returns301Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUserByMossoId(12345)).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUserFromMossoId(request, 12345, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(301));

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
    public void getUserFromNastId_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUserFromNastId(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserFromNastId_isAdminCall_callsUserService_getUserByMossoId() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getUserFromNastId(request, "nastId", null);
        verify(userService).getUserByNastId("nastId");
    }

    @Test
    public void getUserFromNastId_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUserByNastId("nastId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserFromNastId(request, "nastId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserFromNastId_withValidUser_returns301Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUserByNastId("nastId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUserFromNastId(request, "nastId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(301));
    }

    @Test
    public void getUserGroups_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUserGroups(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserGroups_notAuthorized_returns401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getUserGroups(request, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void getUserGroups_blankUserId_returnsBadRequestResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getUserGroups_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserGroups_withValidUser_callsUserGroupService_getGroupsForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.getUserGroups(request, "userId", null);
        verify(userGroupService).getGroupsForUser(anyString());
    }

    @Test
    public void getUserGroups_noUserGroups_callsCloudGroupService_getGroupById() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userGroupService.getGroupsForUser(anyString())).thenReturn(new ArrayList<Group>());
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.getUserGroups(request, "userId", null);
        verify(cloudGroupService).getGroupById(anyInt());
    }

    @Test
    public void getUserGroups_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userGroupService.getGroupsForUser(anyString())).thenReturn(new ArrayList<Group>());
        when(cloudGroupService.getGroupById(anyInt())).thenReturn(new Group());
        when(userService.getUser("userId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserKey_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getUserKey(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getUserKey_isAdminCall_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getUserKey(request, "userId", null);
        verify(userService).getUser("userId");
    }

    @Test
    public void getUserKey_withNullUser_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserKey(request, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUserKey_withValidUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.getUserKey(request, "userId", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.setUserEnabled(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void setUserEnabled_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.setUserEnabled(request, "userId", null, null);
        verify(userService).getUser("userId");
    }

    @Test
    public void setUserEnabled_withNullUser_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.setUserEnabled(request, "userId", null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void setUserEnabled_withValidUser_callsUser_setEnabled() throws Exception {
        UserWithOnlyEnabled enabledUser = mock(UserWithOnlyEnabled.class);
        com.rackspace.idm.domain.entity.User user = mock(com.rackspace.idm.domain.entity.User.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(user);
        when(enabledUser.isEnabled()).thenReturn(true);
        spy.setUserEnabled(request, "userId", enabledUser, null);
        verify(user).setEnabled(true);
    }

    @Test
    public void setUserEnabled_withValidUser_callsUserService_updateUser() throws Exception {
        UserWithOnlyEnabled enabledUser = mock(UserWithOnlyEnabled.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(enabledUser.isEnabled()).thenReturn(true);
        spy.setUserEnabled(request, "userId", enabledUser, null);
        verify(userService).updateUser(userDO, false);
    }

    @Test
    public void setUserEnabled_withValidUser_callsUserConverterCloudV11_toCloudV11UserWithOnlyEnabled() throws Exception {
        UserWithOnlyEnabled enabledUser = mock(UserWithOnlyEnabled.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(enabledUser.isEnabled()).thenReturn(true);
        spy.setUserEnabled(request, "userId", enabledUser, null);
        verify(userConverterCloudV11).toCloudV11UserWithOnlyEnabled(userDO);
    }

    @Test
    public void setUserEnabled_withValidUser_returns200Status() throws Exception {
        UserWithOnlyEnabled enabledUser = mock(UserWithOnlyEnabled.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(enabledUser.isEnabled()).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.setUserEnabled(request, "userId", enabledUser, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setUserKey_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.setUserKey(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void setUserKey_isAdminCall_callUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.setUserKey(request, "userId", null, null);
        verify(userService).getUser("userId");
    }

    @Test
    public void setUserKey_withNullUser_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.setUserKey(request, "userId", null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void setUserKey_withValidUser_callsUser_setApiKey() throws Exception {
        UserWithOnlyKey keyUser = mock(UserWithOnlyKey.class);
        com.rackspace.idm.domain.entity.User user = mock(com.rackspace.idm.domain.entity.User.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(user);
        when(keyUser.isEnabled()).thenReturn(true);
        spy.setUserKey(request, "userId", null, keyUser);
        verify(user).setApiKey(anyString());
    }

    @Test
    public void setUserKey_withValidUser_callsUserService_updateUser() throws Exception {
        UserWithOnlyKey keyUser = mock(UserWithOnlyKey.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(keyUser.isEnabled()).thenReturn(true);
        spy.setUserKey(request, "userId", null, keyUser);
        verify(userService).updateUser(userDO, false);
    }

    @Test
    public void setUserKey_withValidUser_callsUserConverterCloudV11_toCloudV11UserWithOnlyKey() throws Exception {
        UserWithOnlyKey keyUser = mock(UserWithOnlyKey.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(keyUser.isEnabled()).thenReturn(true);
        spy.setUserKey(request, "userId", null, keyUser);
        verify(userConverterCloudV11).toCloudV11UserWithOnlyKey(userDO);
    }

    @Test
    public void setUserKey_withValidUser_returns200Status() throws Exception {
        UserWithOnlyKey keyUser = mock(UserWithOnlyKey.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(keyUser.isEnabled()).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.setUserKey(request, "userId", null, keyUser);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.updateUser(request, null, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void updateUser_isAdminCall_callUserValidator_validate() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        spy.updateUser(request, null, null, user);
        verify(userValidator).validate(user);
    }

    @Test
    public void updateUser_userIdIsBlankAndMatching_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("");
        spy.updateUser(request, "", null, user);
        verify(userService).getUser("");
    }

    @Test
    public void updateUser_userIdIsBlankAndNotMatching_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("");
        spy.updateUser(request, "123", null, user);
        verify(userService).getUser("123");
    }

    @Test
    public void updateUser_userIdNotIsBlankAndMatching_callsUserService_getUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        spy.updateUser(request, "userId", null, user);
        verify(userService).getUser("userId");
    }

    @Test
    public void updateUser_userIdIsNotBlankAndNotMatching_returnBadRequestResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId2");
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, "userId1", null, user);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUser_userServiceFindsUser_callsUserService_updateUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(userService).updateUser(userDO, false);
    }

    @Test
    public void updateUser_userServiceDoesNotFindUser_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, "userId", null, user);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void updateUser_userHasNoBaseURLRefs_doesNotEditUserBaseURLRefs() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(endpointService, never()).addBaseUrlToUser(anyInt(), anyBoolean(), anyString());
        verify(endpointService, never()).removeBaseUrlFromUser(anyInt(), anyString());
    }

    @Test
    public void updateUser_userHasBaseURLRefs_callsEndpointService_getEndpointsForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        user.setBaseURLRefs(new BaseURLRefList());
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(endpointService).getEndpointsForUser("userId");
    }

    @Test
    public void updateUser_endpointServiceHasEndpointsForUser_oldEndpointsAreDeleted() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(new BaseURLRef());
        user.setBaseURLRefs(baseURLRefList);
        when(userService.getUser("userId")).thenReturn(userDO);
        ArrayList<CloudEndpoint> cloudEndpoints = new ArrayList<CloudEndpoint>();
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setBaseUrlId(12345);
        cloudEndpoint.setBaseUrl(baseUrl);
        cloudEndpoints.add(cloudEndpoint);
        when(endpointService.getEndpointsForUser("userId")).thenReturn(cloudEndpoints);
        spy.updateUser(request, "userId", null, user);
        verify(endpointService).removeBaseUrlFromUser(anyInt(), eq("userId"));
    }

    @Test
    public void updateUser_userHasEndpoints_newEndpointsAreAdded() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(new BaseURLRef());
        user.setBaseURLRefs(baseURLRefList);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(endpointService).addBaseUrlToUser(anyInt(), anyBoolean(), eq("userId"));
    }

    //TODO
//    @Test
//    public void updateUser_userIsDisabled_callsAtomHopperClient_postUser() throws Exception {
//        doNothing().when(spy).authenticateCloudAdminUser(request);
//        doNothing().when(userValidator).validate(user);
//        user.setId("userId");
//        user.setEnabled(false);
//        when(userService.getUser("userId")).thenReturn(userDO);
//        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(request);
//        spy.updateUser(request, "userId", null, user);
//        verify(atomHopperClient).postUser(any(com.rackspace.idm.domain.entity.User.class), anyString(), eq("disabled"));
//    }

    @Test
    public void updateUser_userExistsAndIsValid_callsUserConverterCloudV11_toCloudV11User() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(userConverterCloudV11).toCloudV11User(eq(userDO), any(List.class));
    }

    @Test
    public void updateUser_userExistsAndIsValid_return200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        when(userService.getUser("userId")).thenReturn(userDO);
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, "userId", null, user);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLId_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getBaseURLId(request, 0, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseURLId_isAdminCall_callEndpointService_getBaseUrlById() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getBaseURLId(request, 12345, null, null);
        verify(endpointService).getBaseUrlById(12345);
    }

    @Test
    public void getBaseURLId_withNullBaseURL_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrlById(12345)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLId(request, 12345, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLId_withServiceNameNotNullAndNotMatching_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName2");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLId(request, 12345, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLId_withValidServiceAndBaseURL_callsEndpointConverterCloudV11_toBaseUrl() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        spy.getBaseURLId(request, 12345, "serviceName", null);
        verify(endpointConverterCloudV11).toBaseUrl(any(CloudBaseUrl.class));
    }

    @Test
    public void getBaseURLId_withValidServiceAndBaseURL_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLId(request, 12345, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLs_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getBaseURLs(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseURLs_isAdminCall_callEndpointService_getBaseUrls() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getBaseURLs(request, null, null);
        verify(endpointService).getBaseUrls();
    }

    @Test
    public void getBaseURLs_emptyServiceName_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        Response.ResponseBuilder responseBuilder = spy.getBaseURLs(request, "", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLs_noBaseUrls_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        Response.ResponseBuilder responseBuilder = spy.getBaseURLs(request, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLs_noMatchingBaseUrlServices_returnNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName2");
        cloudBaseUrls.add(cloudBaseUrl);
        when(endpointService.getBaseUrls()).thenReturn(cloudBaseUrls);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLs(request, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLs_matchingBaseUrlServices_callsEndpointConverterCloudV11_toBaseUrls() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrls.add(cloudBaseUrl);
        when(endpointService.getBaseUrls()).thenReturn(cloudBaseUrls);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLs(request, "serviceName", null);
        verify(endpointConverterCloudV11).toBaseUrls(anyList());
    }

    @Test
    public void getBaseURLs_matchingBaseUrlServices_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrls.add(cloudBaseUrl);
        when(endpointService.getBaseUrls()).thenReturn(cloudBaseUrls);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLs(request, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getEnabledBaseURL_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getEnabledBaseURL(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getEnabledBaseURL_isAdminCall_callsEndpointService_getBaseUrls() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getEnabledBaseURL(request, null, null);
        verify(endpointService).getBaseUrls();
    }

    @Test
    public void getEnabledBaseURL_withNoEnabledUrls_toBaseUrlsCalledWithEmptyList() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        spy.getEnabledBaseURL(request, null, null);
        verify(endpointConverterCloudV11).toBaseUrls(argument.capture());
        assertThat("argument is empty list", argument.getValue().isEmpty(), equalTo(true));
    }

    @Test
    public void getEnabledBaseURL_withEnabledUrls_toBaseUrlsCalledWithNonEmptyList() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrls.add(cloudBaseUrl);
        when(endpointService.getBaseUrls()).thenReturn(cloudBaseUrls);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        spy.getEnabledBaseURL(request, null, null);
        verify(endpointConverterCloudV11).toBaseUrls(argument.capture());
        assertThat("argument is nonempty list", argument.getValue().isEmpty(), equalTo(false));
    }

    @Test
    public void getEnabledBaseURL_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        Response.ResponseBuilder responseBuilder = spy.getEnabledBaseURL(request, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));

    }

    @Test
    public void addBaseURL_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.addBaseURL(request, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void addBaseURL_isAdminCall_callsEndpointConverterCloudV11_todBaseUrlDO() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        BaseURL baseUrl = new BaseURL();
        spy.addBaseURL(request, null, baseUrl);
        verify(endpointConverterCloudV11).toBaseUrlDO(baseUrl);
    }

    @Test
    public void addBaseURL_isAdminCall_callsEndpointService_addBaseUrl() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(endpointConverterCloudV11.toBaseUrlDO(null)).thenReturn(new CloudBaseUrl());
        spy.addBaseURL(request, null, null);
        verify(endpointService).addBaseUrl(any(CloudBaseUrl.class));
    }

    @Test
    public void addBaseURL_isAdminCall_returns201Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(endpointConverterCloudV11.toBaseUrlDO(any(BaseURL.class))).thenReturn(null);
        doNothing().when(endpointService).addBaseUrl(null);
        Response.ResponseBuilder responseBuilder = spy.addBaseURL(request, null, new BaseURL());
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(201));
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

    @Test
    public void getAuthtokenFromRequest_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndPassword() throws Exception {
        defaultCloud11Service.getAuthtokenFromRequest(request);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(anyString(), anyString(), anyString());
    }
}
