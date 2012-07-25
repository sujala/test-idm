package com.rackspace.idm.web;


import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultApplicationService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.sun.grizzly.http.servlet.HttpServletResponseImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/12/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class AcceptServletTest {
    private AcceptServlet acceptServlet;
    private AcceptServlet spy;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private ApplicationService applicationService;
    private UserService userService;
    private ScopeAccessService scopeAccessService;

    @Before
    public void setup() throws Exception {
        acceptServlet = new AcceptServlet();

        // Setup mocks
        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
        applicationService = mock(ApplicationService.class);
        userService = mock(UserService.class);
        scopeAccessService = mock(ScopeAccessService.class);

        spy = spy(acceptServlet);
    }

    @Test
    public void doPost_redirectUriIsBlank_setStatus400() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("");
        acceptServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(400);
    }

    @Test
    public void doPost_allParametersIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyResponseTypeIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyClientIdIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyScopeIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyAcceptIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyDaysIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyUsernameIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyVerificationIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_responseTypeDoesNotEqualsCode_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_acceptDoesNotEqualsAccept_setErrorResponseAccessDenied() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "access_denied");
    }

    @Test
    public void doPost_applicationCIsNull_setErrorResponseInvalidScope() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("a b c");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(null);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_scope");
    }

    @Test
    public void doPost_applicationClientIsNull_setErrorResponseUnauthorizedClient() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(null);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "unauthorized_client");
    }

    @Test
    public void doPost_clientDisabled_setErrorResponseUnauthorizedClient() throws Exception {
        Application client = new Application();
        client.setEnabled(false);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "unauthorized_client");
    }

    @Test
    public void doPost_userIsNull_setErrorResponseInvalidRequest() throws Exception {
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(null);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_verifcationNotEqualSecureId_setErrorResponseInvalidRequest() throws Exception {
        User user = new User();
        user.setUsername("username");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_usernameDoesNotMatch_setErrorResponseInvalidRequest() throws Exception {
        User user = new User();
        user.setUsername("");
        user.setSecureId("verification");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_scopeAccessSaIsNull_setErrorResponseAccessDenied() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "access_denied");
    }

    @Test
    public void doPost_daysIsNotInt_setErrorResponseInvalidRequest() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_withNullDelegatedScopeAccess_() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("10");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), anyString())).thenReturn(null);
        when(scopeAccessService.addDelegateScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new DelegatedClientScopeAccess());
        doReturn(123456).when(spy).getAuthCodeExpirationSeconds();
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(302);
        verify(httpServletResponse).setHeader(eq("Location"), anyString());
    }

    @Test
    public void doPost_withDelegatedScopeAccess_callsAddDelegateScopeAccess() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("10");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDelegateScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        when(scopeAccessService.addDelegateScopeAccess(anyString(),any(ScopeAccess.class))).thenReturn(new DelegatedClientScopeAccess());
        doReturn(123456).when(spy).getAuthCodeExpirationSeconds();
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(scopeAccessService, never()).addDelegateScopeAccess(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void doPost_directScopeAccesses_callsGetPermissionsForParent() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("10");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDelegateScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new DelegatedClientScopeAccess());
        when(scopeAccessService.addDelegateScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new DelegatedClientScopeAccess());
        doReturn(123456).when(spy).getAuthCodeExpirationSeconds();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(scopeAccessService).getPermissionsForParent(anyString(), any(Permission.class));
    }

    @Test
    public void doPost_withPermissions_callsDelegatePermission() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("10");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDelegateScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new DelegatedClientScopeAccess());
        when(scopeAccessService.addDelegateScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new DelegatedClientScopeAccess());
        doReturn(123456).when(spy).getAuthCodeExpirationSeconds();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        when(scopeAccessService.addScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        permissions.add(new GrantedPermission());
        permissions.add(new Permission());
        when(scopeAccessService.getPermissionsForParent(anyString(), any(Permission.class))).thenReturn(permissions);

        spy.doPost(httpServletRequest, httpServletResponse);
        verify(scopeAccessService).delegatePermission(anyString(), any(DelegatedPermission.class));
    }

    @Test
    public void doPost_withNoGrantedPermissions_doesNotCallDelegatePermission() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setSecureId("verification");
        user.setUniqueId("uniqueId");
        Application client = new Application();
        client.setEnabled(true);
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("code");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("Accept");
        when(httpServletRequest.getParameter("days")).thenReturn("10");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        doReturn(applicationService).when(spy).getClientService();
        when(applicationService.getClientByScope(anyString())).thenReturn(new Application());
        when(applicationService.getById(anyString())).thenReturn(client);
        doReturn(userService).when(spy).getUserService();
        when(userService.getUserBySecureId(anyString())).thenReturn(user);
        doReturn(scopeAccessService).when(spy).getScopeAccessService();
        when(scopeAccessService.getDelegateScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new DelegatedClientScopeAccess());
        when(scopeAccessService.addDelegateScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new DelegatedClientScopeAccess());
        doReturn(123456).when(spy).getAuthCodeExpirationSeconds();
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ScopeAccess());
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        permissions.add(new Permission());
        when(scopeAccessService.getPermissionsForParent(anyString(), any(Permission.class))).thenReturn(permissions);

        spy.doPost(httpServletRequest, httpServletResponse);
        verify(scopeAccessService, never()).delegatePermission(anyString(), any(DelegatedPermission.class));
    }

    @Test
    public void getClientService_clientServiceIsNotNull_returnsApplicationService() throws Exception {
        DefaultApplicationService defaultApplicationService = mock(DefaultApplicationService.class);
        acceptServlet.setClientService(defaultApplicationService);
        ApplicationService result = acceptServlet.getClientService();
        assertThat("application service", (DefaultApplicationService)result, equalTo(defaultApplicationService));
    }

    @Test
    public void getClientService_clientServiceIsNull_returnsApplicationService() throws Exception {
        DefaultApplicationService defaultApplicationService = mock(DefaultApplicationService.class);
        WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
        doReturn(webApplicationContext).when(spy).getWebApplicationContext();
        when(webApplicationContext.getBean(ApplicationService.class)).thenReturn(defaultApplicationService);
        ApplicationService result = spy.getClientService();
        assertThat("application service", (DefaultApplicationService) result, equalTo(defaultApplicationService));
    }

    @Test
    public void getUserService_UserServiceIsNotNull_returnsUserService() throws Exception {
        DefaultUserService defaultUserService = mock(DefaultUserService.class);
        acceptServlet.setUserService(defaultUserService);
        UserService result = acceptServlet.getUserService();
        assertThat("application service", (DefaultUserService)result, equalTo(defaultUserService));
    }

    @Test
    public void getUserService_UserServiceIsNull_returnsApplicationService() throws Exception {
        DefaultUserService defaultUserService = mock(DefaultUserService.class);
        WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
        doReturn(webApplicationContext).when(spy).getWebApplicationContext();
        when(webApplicationContext.getBean(UserService.class)).thenReturn(defaultUserService);
        UserService result = spy.getUserService();
        assertThat("application service", (DefaultUserService) result, equalTo(defaultUserService));
    }

    @Test
    public void getScopeAccessService_ScopeAccessServiceIsNotNull_returnsScopeAccessService() throws Exception {
        DefaultScopeAccessService defaultScopeAccessService = mock(DefaultScopeAccessService.class);
        acceptServlet.setScopeAccessService(defaultScopeAccessService);
        ScopeAccessService result = acceptServlet.getScopeAccessService();
        assertThat("application service", (DefaultScopeAccessService)result, equalTo(defaultScopeAccessService));
    }

    @Test
    public void getScopeAccessService_ScopeAccessServiceIsNull_returnsApplicationService() throws Exception {
        DefaultScopeAccessService defaultScopeAccessService = mock(DefaultScopeAccessService.class);
        WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
        doReturn(webApplicationContext).when(spy).getWebApplicationContext();
        when(webApplicationContext.getBean(ScopeAccessService.class)).thenReturn(defaultScopeAccessService);
        ScopeAccessService result = spy.getScopeAccessService();
        assertThat("application service", (DefaultScopeAccessService) result, equalTo(defaultScopeAccessService));
    }

    @Test
    public void getAuthCodeExpirationSeconds_configNotNull_returns20() throws Exception {
        Configuration config = mock(Configuration.class);
        acceptServlet.setConfig(config);
        when(config.getInt("authcode.expiration.seconds", 20)).thenReturn(20);
        int result = acceptServlet.getAuthCodeExpirationSeconds();
        assertThat("seconds", result, equalTo(20));
    }

    @Test
    public void getAuthCodeExpirationSeconds_configIsNull_returns20() throws Exception {
        Configuration config = mock(Configuration.class);
        WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
        doReturn(webApplicationContext).when(spy).getWebApplicationContext();
        when(webApplicationContext.getBean(Configuration.class)).thenReturn(config);
        when(config.getInt("authcode.expiration.seconds", 20)).thenReturn(20);
        int result = spy.getAuthCodeExpirationSeconds();
        assertThat("configuration", result, equalTo(20));
    }
}
