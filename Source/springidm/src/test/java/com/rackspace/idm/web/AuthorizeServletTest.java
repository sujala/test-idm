package com.rackspace.idm.web;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.UserDisabledException;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/21/12
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class AuthorizeServletTest {
    private AuthorizeServlet authorizeServlet;
    private ApplicationService applicationService;
    private UserService userService;
    private ScopeAccessService scopeAccessService;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private AuthorizeServlet spy;

    @Before
    public void setUp() throws Exception    {
        authorizeServlet = new AuthorizeServlet();

        //mocks
        applicationService = mock(ApplicationService.class);
        userService = mock(UserService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);

        spy = spy(authorizeServlet);
    }

    @Test
    public void doGet_redirectUriIsBlank_setStatus400() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("");
        authorizeServlet.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(400);
    }

    @Test
    public void doGet_responseTypeIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("");
        authorizeServlet.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_clientIdIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("");
        authorizeServlet.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_scopeListIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("");
        authorizeServlet.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_responseTypeNotEqualCode_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        authorizeServlet.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_applicationIsNull_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        doReturn(applicationService).when(spy).getClientService();
        spy.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_clientIsNull_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        spy.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_clientNotEnabled_setStatus302() throws Exception {
        Application client = new Application();
        client.setEnabled(false);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        spy.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doGet_callsRequest_getRequestDispatcher() throws Exception {
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        when(httpServletRequest.getRequestDispatcher("/web/login.jsp")).thenReturn(requestDispatcher);
        spy.doGet(httpServletRequest, httpServletResponse);
        verify(httpServletRequest).getRequestDispatcher("/web/login.jsp");
    }

    @Test
    public void doPost_redirectUriIsBlank_setStatus400() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(400);
    }

    @Test
    public void doPost_responseTypeIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_clientIdIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_scopeListIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_usernameIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_passwordIsBlank_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_responseNotEqualCode_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        authorizeServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_applicationIsNull_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_clientIsNull_setStatus302() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_clientNotEnabled_setStatus302() throws Exception {
        Application client = new Application();
        client.setEnabled(false);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_userDisabledException_setStatus302() throws Exception {
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        doReturn(userService).when(spy).getUserService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        doThrow(new UserDisabledException()).when(userService).authenticate("username", "password");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_notAuthenticated_getRequestDispatcher() throws Exception {
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        Application client = new Application();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(new User(), false);
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        doReturn(userService).when(spy).getUserService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        when(userService.authenticate("username", "password")).thenReturn(uaResult);
        when(httpServletRequest.getRequestDispatcher("/web/login.jsp")).thenReturn(requestDispatcher);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(requestDispatcher).forward(httpServletRequest, httpServletResponse);
    }

    @Test
    public void doPost_scopeAccessIsNull_setStatus302() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        Application client = new Application();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        client.setEnabled(true);
        client.setClientId("clientId");
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        doReturn(userService).when(spy).getUserService();
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        when(userService.authenticate("username", "password")).thenReturn(uaResult);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
    }

    @Test
    public void doPost_callsRequest_getRequestDispatcherForward() throws Exception {
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        User user = new User();
        user.setUniqueId("uniqueId");
        user.setUsername("username");
        Application client = new Application();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        client.setEnabled(true);
        client.setClientId("clientId");
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("clientId");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        doReturn(applicationService).when(spy).getClientService();
        doReturn(userService).when(spy).getUserService();
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(applicationService.getClientByScope("scope")).thenReturn(new Application());
        when(applicationService.getById("clientId")).thenReturn(client);
        when(userService.authenticate("username", "password")).thenReturn(uaResult);
        when(scopeAccessService.getDirectScopeAccessForParentByClientId("uniqueId", null)).thenReturn(new ScopeAccess());
        when(userService.getUser("username")).thenReturn(new User());
        when(httpServletRequest.getRequestDispatcher("/web/scope.jsp")).thenReturn(requestDispatcher);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(requestDispatcher).forward(httpServletRequest, httpServletResponse);
    }
}
