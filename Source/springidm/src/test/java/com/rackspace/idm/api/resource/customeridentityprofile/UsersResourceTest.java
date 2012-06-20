package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/20/12
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class UsersResourceTest {

    UserService userService;
    UserConverter userConverter;
    AuthorizationService authorizationService;
    UsersResource usersResource;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        userConverter = mock(UserConverter.class);
        authorizationService = mock(AuthorizationService.class);

        usersResource = new UsersResource(null, userConverter, authorizationService, null, userService);
    }

    @Test
    public void getUsers_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        usersResource.getUsers(null, null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void getUsers_withNullLimitAndOffset_callsUserService_withNegOneLimitAndOffset() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        usersResource.getUsers(null, null, null, null);
        verify(userService).getAllUsers(any(FilterParam[].class), eq(-1), eq(-1));
    }

    @Test
    public void getUsers_callsUserService() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        usersResource.getUsers(null, null, 2, 2);
        verify(userService).getAllUsers(any(FilterParam[].class), eq(2), eq(2));
    }

    @Test
    public void getUsers_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        Response response = usersResource.getUsers(null, null, 2, 2);
        assertThat("response status", response.getStatus(), equalTo(200));
    }
}
