package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.entity.Applications;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserApplicationsResourceTest {
    private UserApplicationsResource userApplicationsResource;
    private UserApplicationResource applicationResource;
    private ApplicationConverter applicationConverter;
    private UserService userService;
    private AuthorizationService authorizationService;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        applicationConverter = mock(ApplicationConverter.class);
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);
        applicationResource = mock(UserApplicationResource.class);
        userApplicationsResource = new UserApplicationsResource(applicationResource, applicationConverter, userService,
                authorizationService, inputValidator);
    }

    @Test
    public void getApplicationsForUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        userApplicationsResource.getApplicationsForUser("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getApplicationsForUser_callsUserService_loadUser() throws Exception {
        userApplicationsResource.getApplicationsForUser("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getApplicationsForUser_callsApplicationConverter_toApplicationJaxbMin() throws Exception {
        userApplicationsResource.getApplicationsForUser("authHeader", "userId");
        verify(applicationConverter).toApplicationJaxbMin(any(Applications.class));
    }

    @Test
    public void getApplicationsForUser_responseOk_returns200() throws Exception {
        Response response = userApplicationsResource.getApplicationsForUser("authHeader", "userId");
        assertThat("respone code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getApplicationResource_returnsApplicationResource() throws Exception {
        UserApplicationResource resource = userApplicationsResource.getApplicationResource();
        assertThat("application resource", resource, equalTo(applicationResource));
    }
}
