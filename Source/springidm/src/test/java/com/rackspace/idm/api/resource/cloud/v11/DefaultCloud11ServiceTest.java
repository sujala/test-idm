package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mortbay.jetty.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 6:19 PM
 */
public class DefaultCloud11ServiceTest {

    DefaultCloud11Service defaultCloud11Service;
    DefaultCloud11Service spy;
    UserConverterCloudV11 userConverterCloudV11;
    UserValidator userValidator;
    LdapCloudAdminRepository ldapCloudAdminRepository;
    NastFacade nastFacade;
    UserService userService;
    EndpointService endpointService;
    Configuration config;
    UriInfo uriInfo;
    User user = new User();
    HttpServletRequest request;
    String token = "token";
    private ScopeAccessService scopeAccessService;


    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        ldapCloudAdminRepository = mock(LdapCloudAdminRepository.class);
        when(ldapCloudAdminRepository.authenticate("auth", "auth123")).thenReturn(true);
        userService = mock(UserService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        endpointService = mock(EndpointService.class);
        uriInfo = mock(UriInfo.class);
        config = mock(Configuration.class);
        request = mock(HttpServletRequest.class);
        userValidator = mock(UserValidator.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path("userId")).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.toUserDO(user)).thenReturn(user1);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service = new DefaultCloud11Service(config, scopeAccessService, endpointService, userService, null, userConverterCloudV11, null, ldapCloudAdminRepository);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
        defaultCloud11Service.setUserValidator(userValidator);
        spy = spy(defaultCloud11Service);
    }

    @Test
    public void usernameConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder builder = defaultCloud11Service.usernameConflictExceptionResponse("foo");
        assertThat("response code", builder.build().getStatus(), equalTo(409));
    }

    @Test
    public void authenticateResponse_withNastCredentials_withEmptyUsername_returns400() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("");
        JAXBElement<NastCredentials> credentials =
                new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withEmptyUsername_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withNullPasswordAndUsername_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withCredentials_withNoApiKey_returns400() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_withEmptyPassword_returns400() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withUserCredentials_withEmptyUsername_returns400() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(credentials, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void authenticateResponse_withNastCredentials_callsUserService_getUserByNastId() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("nastId");
        JAXBElement<NastCredentials> credentials =
                new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(userService).getUserByNastId("nastId");
    }

    @Test
    public void authenticateResponse_withNastCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByNastIdAndApiCredentials() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("nastId");
        JAXBElement<NastCredentials> credentials =
                new JAXBElement<NastCredentials>(QName.valueOf("foo"), NastCredentials.class, nastCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByNastIdAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        JAXBElement<MossoCredentials> credentials =
                new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(userService).getUserByMossoId(anyInt());
    }

    @Test
    public void authenticateResponse_withMossoCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(1);
        JAXBElement<MossoCredentials> credentials =
                new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, mossoCredentials);
        when(userService.getUser(null)).thenReturn(new com.rackspace.idm.domain.entity.User());
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByMossoIdAndApiCredentials(anyInt(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withUserCredentials_callsUserService_getUser() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("username");
        userCredentials.setKey("key");
        JAXBElement<UserCredentials> credentials =
                new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, userCredentials);
        defaultCloud11Service.authenticateResponse(credentials, null);
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
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void authenticateResponse_withPasswordCredentials_callsUserService_getUser() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("pass");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);
        defaultCloud11Service.authenticateResponse(credentials, null);
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
        defaultCloud11Service.authenticateResponse(credentials, null);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword(anyString(), anyString(), anyString());
    }

    @Test
    public void createUser_callsNastFacade() throws Exception {
        user.setId("userId");
        user.setMossoId(123);
        defaultCloud11Service.createUser(request, null, uriInfo, user);
        Mockito.verify(nastFacade).addNastUser(user);
    }

    @Test
    public void authenticateResponse_usernameIsNull_returns400() throws Exception {
        JAXBElement<Credentials> cred = new JAXBElement<Credentials>(new QName(""), Credentials.class, new UserCredentials());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(cred, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

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
    public void validateToken_isAdminCall_callAuthenticateCloudAdminUserForGetRequest() throws Exception {
        spy.validateToken(request, null, null, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
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
    public void deleteUser_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.deleteUser(request, null, null);
        verify(spy).authenticateCloudAdminUser(request);
    }

    @Test
    public void getBaseUrlRef_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLRef(request, null, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

    @Test
    public void getBaseUrlRefs_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getBaseURLRefs(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
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
        spy.updateUser(request,null,null,null);
        verify(userValidator).validate(null);
    }

    @Test
    public void updateUser_whenValidatorThrowsBadRequestException_returns400() throws Exception {
        doThrow(new BadRequestException("test exception")).when(userValidator).validate(null);
        Response.ResponseBuilder responseBuilder = spy.updateUser(request, null, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void getUserFromNastId_isAdminCall_callAuthenticateCloudAdminUser() throws Exception {
        spy.getUserFromNastId(request, null, null);
        verify(spy).authenticateCloudAdminUserForGetRequests(request);
    }

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

}
