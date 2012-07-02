package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserApplicationResourceTest {
    private UserApplicationResource userApplicationResource;
    private ScopeAccessService scopeAccessService;
    private ApplicationService applicationService;
    private UserService userService;
    private AuthorizationService authorizationService;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        applicationService = mock(ApplicationService.class);
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);
        userApplicationResource = new UserApplicationResource(scopeAccessService, applicationService, userService, authorizationService, inputValidator);
        when(userService.loadUser(anyString())).thenReturn(new User());
    }

    @Test
    public void provisionApplicationForUser_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.provisionApplicationForUser("authHeader", "userId", "applicationId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void provisionApplicationForUser_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.provisionApplicationForUser("authHeader", "userId", "applicationId");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void provisionApplicationForUser_callsApplicationService_loadApplication() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.provisionApplicationForUser("authHeader", "userId", "applicationId");
        verify(applicationService).loadApplication("applicationId");
    }

    @Test
    public void provisionApplicationForUser_callsScopeAccessService_addDirectScopeAccess() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.provisionApplicationForUser("authHeader", "userId", "applicationId");
        verify(scopeAccessService).addDirectScopeAccess(anyString(), any(UserScopeAccess.class));
    }

    @Test
    public void provisionApplicationForUser_responseNoContent_returns204() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        Response response = userApplicationResource.provisionApplicationForUser("authHeader", "userId", "applicationId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void removeApplicationFromUser_callsAuthService_verifyIdmSUperAdminAccess() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.removeApplicationFromUser("authHeader", "userId", "applicationId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void removeApplicationFromUser_callsApplicationService_loadApplication() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.removeApplicationFromUser("authHeader", "userId", "applicationId");
        verify(applicationService).loadApplication("applicationId");
    }

    @Test
    public void removeApplicationFromUser_callsScopeAccessService_deleteScopeAccessesForParentByApplicationId() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        userApplicationResource.removeApplicationFromUser("authHeader", "userId", "applicationId");
        verify(scopeAccessService).deleteScopeAccessesForParentByApplicationId(anyString(), anyString());
    }

    @Test
    public void removeApplicationFromUser_responseNoContent_returns204() throws Exception {
        when(applicationService.loadApplication("applicationId")).thenReturn(new Application());
        Response response = userApplicationResource.removeApplicationFromUser("authHeader", "userId", "applicationId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }
}
