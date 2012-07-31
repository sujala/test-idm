package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mortbay.jetty.HttpHeaders;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

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
public class DefaultCloud11ServiceTest {

    AuthorizationService authorizationService;
    DefaultCloud11Service defaultCloud11Service;
    CredentialUnmarshaller credentialUnmarshaller;
    DefaultCloud11Service spy;
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
    com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User("userId");
    HttpServletRequest request;
    private ScopeAccessService scopeAccessService;
    UserScopeAccess userScopeAccess;
    javax.ws.rs.core.HttpHeaders httpHeaders;
    CloudExceptionResponse cloudExceptionResponse;
    Application application = new Application("id",null,"myApp", null);
    AtomHopperClient atomHopperClient;
    GroupService userGroupService, cloudGroupService;
    AuthConverterCloudV11 authConverterCloudv11;
    CredentialValidator credentialValidator;
    private CloudContractDescriptionBuilder cloudContratDescriptionBuilder;
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
        credentialValidator = mock(CredentialValidator.class);
        cloudContratDescriptionBuilder = mock(CloudContractDescriptionBuilder.class);
        tenant = new Tenant();
        tenant.setName("tenant");
        tenant.setEnabled(true);
        tenant.setTenantId("1");
        String baseUrls[] = {};
        tenant.setBaseUrlIds(baseUrls);

        userDO.setId("1");
        userDO.setMossoId(1);
        userDO.setNastId("nastId");

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
        Application testService = new Application(null, null, "testService", null);
        testService.setOpenStackType("foo");
        when(clientService.getByName(any(String.class))).thenReturn(testService);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultCloud11Service = new DefaultCloud11Service(config, scopeAccessService, endpointService, userService, authConverterCloudv11, userConverterCloudV11, endpointConverterCloudV11, cloudExceptionResponse, clientService, tenantService);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
        defaultCloud11Service.setUserValidator(userValidator);
        defaultCloud11Service.setAuthorizationService(authorizationService);
        defaultCloud11Service.setAtomHopperClient(atomHopperClient);
        defaultCloud11Service.setCloudGroupService(cloudGroupService);
        defaultCloud11Service.setUserGroupService(userGroupService);
        defaultCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
        defaultCloud11Service.setCredentialValidator(credentialValidator);
        defaultCloud11Service.setCloudContractDescriptionBuilder(cloudContratDescriptionBuilder);
        defaultCloud11Service.setAuthHeaderHelper(authHeaderHelper);
        spy = spy(defaultCloud11Service);
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
    public void getVersion_responseOk_returns200() throws Exception {
        FileSystemApiDocRepository fileSystemApiDocRepository = new FileSystemApiDocRepository();
        ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil();
        CloudContractDescriptionBuilder cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(fileSystemApiDocRepository, serviceDescriptionTemplateUtil);
        spy.setCloudContractDescriptionBuilder(cloudContractDescriptionBuilder);
        Response.ResponseBuilder result = spy.getVersion(null);
        assertThat("response code", result.build().getStatus(), equalTo(200));
    }

    @Test
    public void deleteUser_callsUserService_hasSubUsers() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.deleteUser(request, "userId", httpHeaders);
        verify(userService).hasSubUsers("userId");
    }

    @Test
    public void deleteUser_callsAuthorizationService_authenticateCloudUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.deleteUser(request, "userId", httpHeaders);
        verify(authorizationService).authorizeCloudUser(any(ScopeAccess.class));
    }

    @Test
    public void authenticateResponse_callsCredentialValidator_validateCredential() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        defaultCloud11Service.authenticateResponse(new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials));
        verify(credentialValidator).validateCredential(nastCredentials);
    }

    @Test
    public void adminAuthenticateResponse_callsCredentialValidator_validateCredential() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        defaultCloud11Service.adminAuthenticateResponse(new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials), null);
        verify(credentialValidator).validateCredential(nastCredentials);
    }

    @Test
    public void adminAuthenticateResponse_notAuthenticatedExceptionthrown_throws401WithCorrectMessage() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setKey("key");
        mossoCredentials.setMossoId(123);
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""),Credentials.class, mossoCredentials);
        when(userService.getUserByMossoId(123)).thenThrow(new NotAuthenticatedException());

        Response response = defaultCloud11Service.adminAuthenticateResponse(jaxbElement, null).build();
        assertThat("response status", response.getStatus(), equalTo(401));
        assertThat("response message",((UnauthorizedFault) (response.getEntity())).getMessage(),equalTo("Username or api key is invalid"));
        assertThat("response message",((UnauthorizedFault) (response.getEntity())).getDetails(),nullValue());

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
        UnauthorizedFault entity = (UnauthorizedFault) responseBuilder.build().getEntity();
        assertThat("message", entity.getMessage(), equalTo("You are not authorized to access this resource."));
    }

    @Test
    public void getUserGroups_notAuthorized_returnsCorrectErrorCode() throws Exception {
        doThrow(new NotAuthorizedException("You are not authorized to access this resource.")).when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        UnauthorizedFault entity = (UnauthorizedFault) responseBuilder.build().getEntity();
        assertThat("code", entity.getCode(), equalTo(401));
    }

    @Test
    public void getUserGroups_notAuthorized_entityDetailsShouldBeNull() throws Exception {
        doThrow(new NotAuthorizedException("You are not authorized to access this resource.")).when(spy).authenticateCloudAdminUserForGetRequests(request);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        UnauthorizedFault entity = (UnauthorizedFault) responseBuilder.build().getEntity();
        assertThat("code", entity.getDetails(), nullValue());
    }

    @Test
    public void getUserGroups_withInvalidUser_returnsCorrectErrorMessage() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)responseBuilder.build().getEntity();
        assertThat("message", entity.getMessage(), equalTo("User not found :testUser"));
    }

    @Test
    public void getUserGroups_withInvalidUser_returnsCorrectErrorCode() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)responseBuilder.build().getEntity();
        assertThat("code", entity.getCode(), equalTo(404));
    }

    @Test
    public void getUserGroups_withInvalidUser_entityDetailsShouldBeNull() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("testUser")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getUserGroups(request, "testUser", httpHeaders);
        ItemNotFoundFault entity = (ItemNotFoundFault)responseBuilder.build().getEntity();
        assertThat("code", entity.getDetails(), equalTo(null));
    }

    @Test
    public void authenticateResponse_withNastCredentials_withEmptyNastId_returns400() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("");
        JAXBElement<NastCredentials> credentials = new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withBlankUsername_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withBlankPassword_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withApiCredentials_withBlankApiKey_returns400() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("username");
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
        userCredentials.setKey("apiKey");
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
        JAXBElement<NastCredentials> credentials = new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByNastId(anyString());
        //verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        JAXBElement<MossoCredentials> credentials = new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByMossoId(anyInt());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(1);
        JAXBElement<MossoCredentials> credentials = new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, mossoCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByMossoId(1);
        //verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_withoutMossoId_returnsBadRequestResponse() throws Exception {
        JAXBElement<MossoCredentials> credentials = new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());
        defaultCloud11Service.authenticateResponse(credentials);
        verify(userService).getUserByMossoId(anyInt());
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
    public void authenticateResponse_withUnknownCredentials_returns404Status() throws Exception {
        Credentials passwordCredentials = new Credentials() {
        };
        JAXBElement<Credentials> credentials =
                new JAXBElement<Credentials>(QName.valueOf("foo"), Credentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials);
        assertThat("Response status", responseBuilder.build().getStatus(), equalTo(404));
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
    public void adminAuthenticateResponse_withUserCredentials_withRedirectThrowingException_failsSilently() throws Exception {
        JAXBElement credentials = mock(JAXBElement.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(credentials.getValue()).thenReturn(new UserCredentials());
        doThrow(new IOException()).when(response).sendRedirect(anyString());
        defaultCloud11Service.adminAuthenticateResponse(credentials, response);
        verify(response).sendRedirect("cloud/auth");
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
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
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
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(eq(userDO.getUsername()), eq("apiKey"), anyString());
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
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
        defaultCloud11Service.adminAuthenticateResponse(credentials, null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
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
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
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
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());
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
    public void addNastTenant_whenNastDisabled_withEmptyNastId_DoesntCallTenantService() throws Exception {
        User user1 = new User();
        user1.setNastId(null);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(false);
        defaultCloud11Service.addNastTenant(user1);
        verify(tenantService, never()).addTenantRoleToUser(any(com.rackspace.idm.domain.entity.User.class), any(TenantRole.class));
    }

    @Test
    public void addNastTenant_withNastId_addsBaseUrlsToTenant() throws Exception {
        User user1 = new User();
        user1.setNastId("nastId");
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(false);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setDef(true);
        baseUrl.setBaseUrlId(1);
        cloudBaseUrls.add(baseUrl);
        CloudBaseUrl baseUrl2 = new CloudBaseUrl();
        baseUrl2.setDef(true);
        baseUrl2.setBaseUrlId(2);
        cloudBaseUrls.add(baseUrl2);
        when(endpointService.getBaseUrlsByBaseUrlType("NAST")).thenReturn(cloudBaseUrls);
        ArgumentCaptor<Tenant> argumentCaptor = ArgumentCaptor.forClass(Tenant.class);
        defaultCloud11Service.addNastTenant(user1);
        verify(tenantService).addTenant(argumentCaptor.capture());
        assertThat("Tenant Base Urls", argumentCaptor.getValue().getBaseUrlIds(), equalTo(new String[]{"1", "2"}));
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
        verify(clientService).getClientRoleByClientIdAndRoleName("id", "foo:default");
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
        verify(clientService).getClientRoleByClientIdAndRoleName("id", "foo:default");
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
    public void addMossoTenant_withNullMossoId_doesNothing() throws Exception {
        User user1 = new User();
        user1.setMossoId(null);
        defaultCloud11Service.addMossoTenant(user1);
        verify(clientService, never()).getClientRoleByClientIdAndRoleName(anyString(), anyString());
        verify(userService, never()).getUsersByMossoId(anyInt());
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
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
    }

    @Test
    public void authenticateResponse_mossoCredentials_usesRetrievedUsersUsername() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(123);
        mossoCredentials.setKey("key");
        JAXBElement<? extends Credentials> cred = new JAXBElement<Credentials>(QName.valueOf(""), Credentials.class, mossoCredentials);
        when(userService.getUserByMossoId(123)).thenReturn(new com.rackspace.idm.domain.entity.User("mossoUser"));
        spy.authenticateResponse(cred);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
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
    public void validateToken_scopeAccessService_returnsNullScope_throwsNotFoundException() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(null);
        when(userService.getUser("belongsTo")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_scopeAccessService_returnsExpiredScope_throwsNotFoundException() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(userScopeAccess);
        when(userService.getUser("belongsTo")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_scopeAccessService_returnsNonUserScopeAccess_throwsNotFoundException() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(new ScopeAccess());
        when(userService.getUser("belongsTo")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, null, "belongsTo", "CLOUD", null);
        assertThat("response builder", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsBlank_doesntCallUserService() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userScopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(new UserScopeAccess());
        when(userService.getUser("belongsTo")).thenReturn(null);
        spy.validateToken(null, null, null, "CLOUD", null);
        verify(userService, never()).getUser("belongsTo");
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

    @Test(expected = BadRequestException.class)
    public void authenticateXML_withInvalidXML_throwsBadRequestException() throws Exception {
        doReturn(null).when(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
        spy.authenticateXML(null, httpHeaders, "<xmlBody/>", true);
        verify(spy).adminAuthenticateResponse(null, null);
    }

    @Test
    public void authenticateXML_isAdmin_callsAdminAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
        spy.authenticateXML(null, httpHeaders, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "  <passwordCredentials username=\"jsmith\" password=\"theUsersPassword\"/>\n" +
                "</auth>", true);
        verify(spy).adminAuthenticateResponse(any(JAXBElement.class), any(HttpServletResponse.class));
    }

    @Test
    public void authenticateXML_isNotAdmin_callsAuthenticateResponse() throws Exception {
        doReturn(null).when(spy).authenticateResponse(any(JAXBElement.class));
        spy.authenticateXML(null, httpHeaders, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "  <passwordCredentials username=\"jsmith\" password=\"theUsersPassword\"/>\n" +
                "</auth>", false);
        verify(spy).authenticateResponse(any(JAXBElement.class));
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
        when(tenantService.getTenant(anyString())).thenReturn(tenant);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        spy.addBaseURLRef(request, "userId", null, null, new BaseURLRef());
        verify(tenantService).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void addBaseUrlRef_validCloudBaseUrl_returns201Status() throws Exception {
        BaseURLRef baseUrlRef = mock(BaseURLRef.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(cloudBaseUrl.getEnabled()).thenReturn(true);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(tenantService.getTenant(anyString())).thenReturn(tenant);
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
        Tenant tenant2 = new Tenant();
        tenant2.setBaseUrlIds(new String[] {"1"});
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        tenant.setBaseUrlIds(new String[] {"1"});
        when(tenantService.getTenant(anyString())).thenReturn(tenant).thenReturn(tenant2);
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(tenantService,times(2)).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void deleteBaseUrlRef_nullTenantByMossoId_updatesTenantOnce() throws Exception {
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setNastId("nast");
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(user1);
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        tenant.setBaseUrlIds(new String[] {"1"});
        when(tenantService.getTenant("nast")).thenReturn(tenant);
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(tenantService,times(1)).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void deleteBaseUrlRef_nullTenantByNastId_updatesTenantOnce() throws Exception {
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setMossoId(123);
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(user1);
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        tenant.setBaseUrlIds(new String[] {"1"});
        when(tenantService.getTenant("123")).thenReturn(tenant);
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(tenantService,times(1)).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void deleteBaseUrlRef_nullBaseUrlRefs_doesNotUpdateTenant() throws Exception {
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setMossoId(123);
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(user1);
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        when(tenantService.getTenant("123")).thenReturn(tenant);
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(tenantService,never()).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void deleteBaseUrlRef_idsDoNotMatch_doesNotUpdateTenant() throws Exception {
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setMossoId(123);
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(user1);
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        tenant.setBaseUrlIds(new String[] {"2"});
        when(tenantService.getTenant("123")).thenReturn(tenant);
        spy.deleteBaseURLRef(null, null, "12345", null);
        verify(tenantService,never()).updateTenant(Matchers.<Tenant>anyObject());
    }

    @Test
    public void deleteBaseUrlRef_withValidData_returnsStatus204() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        when(endpointService.getBaseUrlById(12345)).thenReturn(new CloudBaseUrl());
        CloudBaseUrl cloudBaseUrl = mock(CloudBaseUrl.class);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(cloudBaseUrl.getBaseUrlType()).thenReturn("NAST");
        when(cloudBaseUrl.getBaseUrlId()).thenReturn(1);
        tenant.setBaseUrlIds(new String[] {"1"});
        when(tenantService.getTenant(anyString())).thenReturn(tenant);
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
    @Test
    public void deleteUser_withValidUser_callsAtomHopperClient_postUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        spy.deleteUser(null, null, null);
        verify(atomHopperClient).asyncPost(eq(userDO), anyString(), eq("deleted"), anyString());
    }

    @Test
    public void deleteUser_onADefaultUser_returnsResponseStatus400() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doNothing().when(atomHopperClient).asyncPost(eq(userDO), anyString(), eq("deleted"), anyString());
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void deleteUser_withOnUserWithSubUsers_returnsResponseStatus400() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        when(userService.hasSubUsers(anyString())).thenReturn(true);
        doNothing().when(atomHopperClient).asyncPost(eq(userDO), anyString(), eq("deleted"), anyString());
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void deleteUser_withValidUser_returnsResponseStatus204() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(null);
        when(userService.getUser(null)).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(null);
        doNothing().when(atomHopperClient).asyncPost(eq(userDO), anyString(), eq("deleted"), anyString());
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test(expected = NotAuthorizedException.class)
    public void authenticateCloudAdminUserForGetRequests_withNoBasicParams_throwsNotAuthorized() throws Exception {
        request = mock(HttpServletRequest.class);
        when(authHeaderHelper.parseBasicParams(any(String.class))).thenReturn(null);
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
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
        when(userService.getUser(null)).thenReturn(userDO);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(new UserScopeAccess());
        spy.getBaseURLRef(null, null, "0", null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
    }

    @Test
    public void getBaseUrlRef_withNullEndpointForUser_returnsNotFoundStatus() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(null, null, "0", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseUrlRef_withValidData_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(userDO);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(new UserScopeAccess());
        List<OpenstackEndpoint> endpointsForUser = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantName("foo");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        endpointsForUser.add(openstackEndpoint);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject())).thenReturn(endpointsForUser);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(null, null, "1", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getBaseUrlRefs_isAdminCall_callAuthenticateCloudAdminUserForGetRequests() throws Exception {
        spy.getBaseURLRefs(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }


    @Test
    public void getBaseUrlRefs_isAdminCall_callsEndpointServce_getEndpointsForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(new UserScopeAccess());
        spy.getBaseURLRefs(null, "userId", null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject());
    }

    @Test
    public void getBaseUrlRefs_callsEndpointConverterCloudV11_toBaseUrlRefs_withCloudEndpoints() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(new UserScopeAccess());
        List<OpenstackEndpoint> endpointsForUser = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantName("foo");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        endpointsForUser.add(openstackEndpoint);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject())).thenReturn(endpointsForUser);
        spy.getBaseURLRefs(null, "userId", null);
        verify(endpointConverterCloudV11).openstackToBaseUrlRefs(endpointsForUser);
    }

    @Test
    public void getBaseUrlRefs_withValidUser_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(null);
        when(userService.getUser(null)).thenReturn(userDO);
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
        List<OpenstackEndpoint> endpointsForUser = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantName("foo");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        endpointsForUser.add(openstackEndpoint);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject())).thenReturn(endpointsForUser);
        spy.getServiceCatalog(null, null, null);
        verify(endpointConverterCloudV11).toServiceCatalog(Matchers.anyList());
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
        when(authorizationService.authorizeCloudServiceAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        spy.updateUser(request, null, null, null);
        verify(userValidator).validate(null);
    }

    @Test
    public void updateUser_whenValidatorThrowsBadRequestException_returns400() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
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
        ArrayList<Group> groups = new ArrayList<Group>();
        groups.add(new Group());
        when(userGroupService.getGroupsForUser(anyString())).thenReturn(groups);
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
    public void setUserEnabled_withValidDisabledUser_notifiesAtomFeed() throws Exception {
        UserWithOnlyEnabled enabledUser = mock(UserWithOnlyEnabled.class);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userDO);
        when(enabledUser.isEnabled()).thenReturn(false);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(any(HttpServletRequest.class));
        Response.ResponseBuilder responseBuilder = spy.setUserEnabled(request, "userId", enabledUser, null);
        verify(atomHopperClient).asyncPost(any(com.rackspace.idm.domain.entity.User.class), anyString(), anyString(), anyString());
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
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
    public void updateUser_userIdIsBlank_returns400() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doThrow(new BadRequestException()).when(userValidator).validate(user);
        user.setId("");
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, "", null, user);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
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
        verify(userService, never()).addBaseUrlToUser(anyInt(), Matchers.<com.rackspace.idm.domain.entity.User>anyObject());
        verify(userService, never()).removeBaseUrlFromUser(anyInt(), Matchers.<com.rackspace.idm.domain.entity.User>anyObject());
    }

    @Test
    public void updateUser_userHasBaseURLRefs_callsEndpointService_getEndpointsForUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        user.setBaseURLRefs(new BaseURLRefList());
        when(userService.getUser("userId")).thenReturn(userDO);
        spy.updateUser(request, "userId", null, user);
        verify(scopeAccessService).getUserScopeAccessForClientId(null, null);
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
        List<OpenstackEndpoint> currentEndpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantId(tenant.getTenantId());
        openstackEndpoint.setTenantName(tenant.getName());
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(baseUrl);
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        currentEndpoints.add(openstackEndpoint);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.<ScopeAccess>anyObject())).thenReturn(currentEndpoints);
        spy.updateUser(request, "userId", null, user);
        verify(userService).removeBaseUrlFromUser(anyInt(), Matchers.<com.rackspace.idm.domain.entity.User>anyObject());
    }

    @Test
    public void updateUser_userIsDisabled_callsAtomHopperClient_postUser() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validate(user);
        user.setId("userId");
        user.setEnabled(false);
        when(userService.getUser("userId")).thenReturn(userDO);
        doReturn(new UserScopeAccess()).when(spy).getAuthtokenFromRequest(request);
        spy.updateUser(request, "userId", null, user);
        verify(atomHopperClient).asyncPost(any(com.rackspace.idm.domain.entity.User.class), anyString(), eq("disabled"), anyString());
    }

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
        spy.getBaseURLById(request, 0, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseURLId_isAdminCall_callEndpointService_getBaseUrlById() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        spy.getBaseURLById(request, 12345, null, null);
        verify(endpointService).getBaseUrlById(12345);
    }

    @Test
    public void getBaseURLId_withNullBaseURL_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(endpointService.getBaseUrlById(12345)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLById(request, 12345, null, null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLId_withServiceNameNotNullAndNotMatching_returnsNotFoundResponse() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName2");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLById(request, 12345, "serviceName", null);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLId_withValidServiceAndBaseURL_callsEndpointConverterCloudV11_toBaseUrl() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        spy.getBaseURLById(request, 12345, "serviceName", null);
        verify(endpointConverterCloudV11).toBaseUrl(any(CloudBaseUrl.class));
    }

    @Test
    public void getBaseURLId_withValidServiceAndBaseURL_returns200Status() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        when(endpointService.getBaseUrlById(12345)).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLById(request, 12345, "serviceName", null);
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
    public void getEnabledBaseURL_withMixedUrls_toBaseUrlsCalledWithCorrectListSize() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrls.add(cloudBaseUrl);
        CloudBaseUrl cloudBaseUrl2 = new CloudBaseUrl();
        cloudBaseUrl2.setEnabled(false);
        cloudBaseUrls.add(cloudBaseUrl2);
        when(endpointService.getBaseUrls()).thenReturn(cloudBaseUrls);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        spy.getEnabledBaseURL(request, null, null);
        verify(endpointConverterCloudV11).toBaseUrls(argument.capture());
        assertThat("argument is nonempty list", argument.getValue().size(), equalTo(1));
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
        when(authorizationService.authorizeCloudServiceAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType());
        String credentials = "<passwordCredentials password=\"123\" username=\"IValidUser\" xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"/>";
        Response.ResponseBuilder responseBuilder = spy.adminAuthenticate(request, null, httpHeaders, credentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void createUser_validMossoId_callValidateMossoId() throws Exception{
        user.setId("1");
        user.setMossoId(123456);
        when(authorizationService.authorizeCloudServiceAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
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
        UsernameConflictFault conflictFault =(UsernameConflictFault)responseBuilder.build().getEntity();
        assertThat("message", conflictFault.getMessage(), equalTo("Username username already exists"));
    }

    @Test
    public void createUser_usernameAlreadyExists_correctCode409() throws Exception{
        user.setId("username");
        user.setMossoId(123456);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("username")).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        UsernameConflictFault conflictFault =(UsernameConflictFault)responseBuilder.build().getEntity();
        assertThat("message", conflictFault.getCode(), equalTo(409));
    }

    @Test
    public void createUser_usernameAlreadyExists_noDetails() throws Exception{
        user.setId("username");
        user.setMossoId(123456);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("username")).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        UsernameConflictFault conflictFault =(UsernameConflictFault)responseBuilder.build().getEntity();
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
        when(authorizationService.authorizeCloudServiceAdmin(Matchers.<ScopeAccess>anyObject())).thenReturn(true);
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
        verify(userService).addBaseUrlToUser(anyInt(), Matchers.<com.rackspace.idm.domain.entity.User>anyObject());
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

    @Test(expected = CloudAdminAuthorizationException.class)
    public void authenticateCloudAdminUser_withInvalidAuthHeaders() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + Base64.encode("auth"));
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test
    public void authenticateCloudAdminUser_withServiceAndIdentityAdmin_withoutExceptions() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test(expected = CloudAdminAuthorizationException.class)
    public void authenticateCloudAdminUser_withServiceAndIdentityAdminFalse() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud11Service.authenticateCloudAdminUser(request);
    }

    @Test
    public void authenticateCloudAdminUser_withService() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud11Service.authenticateCloudAdminUser(request);
        assertTrue("no Exception thrown", true);
    }

    @Test
    public void authenticateCloudAdminUser_withIdentityAdmin() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud11Service.authenticateCloudAdminUser(request);
        assertTrue("no Exception thrown", true);
    }


    @Test
    public void validateToken_belongsToIsBlank_responseOk_returns200() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("notExpired");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(userScopeAccess);
        when(request.getRequestURL()).thenReturn(new StringBuffer("url/token/tokenId"));
        Response.ResponseBuilder responseBuilder = spy.validateToken(request, "tokenId", null, null, httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addBaseURLRef_baseUrlTypeIsMosso_responseCreated_returns201() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setBaseUrlIds(new String[0]);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setBaseUrlType("MOSSO");
        cloudBaseUrl.setBaseUrlId(1);
        com.rackspace.idm.domain.entity.User userTest = new com.rackspace.idm.domain.entity.User();
        userTest.setMossoId(1);
        BaseURLRef baseUrlRef = new BaseURLRef();
        baseUrlRef.setId(1);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userTest);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        doNothing().when(tenantService).updateTenant(tenant);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", httpHeaders, uriInfo, baseUrlRef);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addBaseURLRef_tenantBaseUrlIdMatchesCloudBaseUrlId_returns400() throws Exception {
        Tenant tenant = new Tenant();
        String[] baseUrlIds = {"2", "1"};
        tenant.setBaseUrlIds(baseUrlIds);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setBaseUrlType("MOSSO");
        cloudBaseUrl.setBaseUrlId(1);
        com.rackspace.idm.domain.entity.User userTest = new com.rackspace.idm.domain.entity.User();
        userTest.setMossoId(1);
        BaseURLRef baseUrlRef = new BaseURLRef();
        baseUrlRef.setId(1);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(userTest);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.addBaseURLRef(request, "userId", httpHeaders, uriInfo, baseUrlRef);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void createUser_callsUserService_addBaseUrlToUser_throwsException_responseCreated_returns201() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        ClientRole roleId = new ClientRole();
        roleId.setId("roleId");
        ClientRole clientRole = new ClientRole();
        clientRole.setName("clientName");
        clientRole.setClientId("clientId");
        clientRole.setId("roleId");
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(1);
        BaseURLRefList baseURLRefList = new BaseURLRefList();
        List<BaseURLRef> baseURLRefs = baseURLRefList.getBaseURLRef();
        baseURLRefs.add(baseURLRef);
        com.rackspace.idm.domain.entity.User userTest = new com.rackspace.idm.domain.entity.User();
        userTest.setUniqueId("uniqueId");
        userTest.setId("userId");
        User user = new User();
        user.setId("userId");
        user.setMossoId(1);
        user.setBaseURLRefs(baseURLRefList);
        doNothing().when(spy).authenticateCloudAdminUser(request);
        doNothing().when(userValidator).validateUsername("userId");
        when(userService.getUser("userId")).thenReturn(null);
        when(userConverterCloudV11.toUserDO(user)).thenReturn(userTest);
        doNothing().when(spy).validateMossoId(1);
        doNothing().when(userService).addUser(userTest);
        doNothing().when(spy).addMossoTenant(user);
        doReturn("nastId").when(spy).addNastTenant(user);
        doNothing().when(userService).updateUser(userTest, false);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("userAdmin");
        when(clientService.getClientRoleByClientIdAndRoleName("clientId", "userAdmin")).thenReturn(roleId);
        when(clientService.getClientRoleById("roleId")).thenReturn(clientRole);
        doNothing().when(tenantService).addTenantRoleToUser(eq(userTest), any(TenantRole.class));
        doThrow(new BadRequestException()).when(userService).addBaseUrlToUser(1, userTest);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getUserScopeAccessForClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(new ArrayList<OpenstackEndpoint>());
        Response.ResponseBuilder responseBuilder = spy.createUser(request, httpHeaders, uriInfo, user);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addMossoTenant_callsAddBaseUrlToTenant() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        User user = new User();
        user.setMossoId(1);
        when(endpointService.getBaseUrlsByBaseUrlType("MOSSO")).thenReturn(cloudBaseUrlList);
        doNothing().when(spy).addbaseUrlToTenant(any(Tenant.class), eq(cloudBaseUrl));
        spy.addMossoTenant(user);
        verify(spy).addbaseUrlToTenant(any(Tenant.class), eq(cloudBaseUrl));
    }

    @Test
    public void addbaseUrlToTenant_isUkCloudRegionAndRegionIsLon_addsBaseUrlIdToTenant() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setRegion("Lon");
        cloudBaseUrl.setBaseUrlId(1);
        Tenant tenant = new Tenant();
        when(config.getString("cloud.region")).thenReturn("UK");
        spy.addbaseUrlToTenant(tenant, cloudBaseUrl);
        assertThat("base url id", tenant.containsBaseUrlId("1"), equalTo(true));
    }

    @Test
    public void addBaseUrlToTenant_isUkCloudRegionAndRegionNotLon_doesNotAddBaseUrlIdToTenant() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setRegion("notLon");
        cloudBaseUrl.setBaseUrlId(1);
        Tenant tenant = new Tenant();
        when(config.getString("cloud.region")).thenReturn("UK");
        spy.addbaseUrlToTenant(tenant, cloudBaseUrl);
        assertThat("base url id", tenant.containsBaseUrlId("1"), equalTo(false));
    }

    @Test
    public void addBaseUrlToTenant_notUkCloudRegionAndRegionIsLon_doesNotAddBaseUrlIdToTenant() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setRegion("Lon");
        cloudBaseUrl.setBaseUrlId(1);
        Tenant tenant = new Tenant();
        when(config.getString("cloud.region")).thenReturn("notUK");
        spy.addbaseUrlToTenant(tenant, cloudBaseUrl);
        assertThat("base url id", tenant.containsBaseUrlId("1"), equalTo(false));
    }

    @Test
    public void addBaseUrlToTenant_notUkCloudRegionAndRegionNotLon_doesNotAddBaseUrlIdToTenant() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setRegion("notLon");
        cloudBaseUrl.setBaseUrlId(1);
        Tenant tenant = new Tenant();
        when(config.getString("cloud.region")).thenReturn("notUK");
        spy.addbaseUrlToTenant(tenant, cloudBaseUrl);
        assertThat("base url id", tenant.containsBaseUrlId("1"), equalTo(true));
    }

    @Test
    public void addBaseUrlToTenant_cloudBaseUrlGetDefIsFalse_doesNothing() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(false);
        spy.addbaseUrlToTenant(tenant, cloudBaseUrl);
        assertTrue("does nothing", true);
    }

    @Test
    public void deleteBaseURLRef_baseUrlDefaultIsTrue_throwsBadRequest_returns400() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        doNothing().when(spy).authenticateCloudAdminUser(request);
        when(userService.getUser("userId")).thenReturn(user);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.deleteBaseURLRef(request, "userId", "1", httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getBaseURLRef_baseUrlIdDoesNotMatchCloudBaseUrlId_responseNotFound_returns404() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(2);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        List<OpenstackEndpoint> openstackEndpointList = new ArrayList<OpenstackEndpoint>();
        openstackEndpointList.add(openstackEndpoint);
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUniqueId("uniqueId");
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(user);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getUserScopeAccessForClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(openstackEndpointList);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRef(request, "userId", "1", httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getBaseURLRefs_userServiceGetUserReturnsNull_throwsNotFound_returns404() throws Exception {
        doNothing().when(spy).authenticateCloudAdminUserForGetRequests(request);
        when(userService.getUser("userId")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.getBaseURLRefs(request, "userId", httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }
}
