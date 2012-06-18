package com.rackspace.idm.api.resource.user;

import com.rackspace.api.idm.v1.User;
import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class UsersResourceTest {
    private UsersResource usersResource;
    private UserResource singleUserResource;
    private UserService userService;
    private InputValidator inputValidator;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    private ScopeAccessService scopeAccessService;

    @Before
    public void setUp() throws Exception {
        singleUserResource = mock(UserResource.class);
        userService = mock(UserService.class);
        inputValidator = mock(InputValidator.class);
        userConverter = mock(UserConverter.class);
        authorizationService = mock(AuthorizationService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        usersResource = new UsersResource(singleUserResource, userService, inputValidator,  userConverter, authorizationService, scopeAccessService);
    }

    @Test
    public void getUsers_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        usersResource.getUsers("username", 1, 1, "authHeader");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void getUsers_callsAuthService_authorizeIdmSuperAdminOrRackspaceClient() throws Exception {
        usersResource.getUsers("username", 1, 1, "authHeader");
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void getUsers_usernameIsBlankResponseOk_returns200() throws Exception {
        Response response = usersResource.getUsers("", 1, 1, "authHeader");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getUsers_callsUserService_getAllUsers() throws Exception {
        usersResource.getUsers("username", 1, 1, "authHeader");
        verify(userService).getAllUsers(any(FilterParam[].class), anyInt(), anyInt());
    }

    @Test
    public void addUser_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        User user = new User();
        when(userConverter.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        usersResource.addUser("authHeader", user);
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void addUser_callsAuthService_authorizeIdmSuperAdminOrRackspaceClient() throws Exception {
        User user = new User();
        when(userConverter.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        usersResource.addUser("authHeader", user);
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void addUser_callsUserService_addUser() throws Exception {
        User user = new User();
        when(userConverter.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        usersResource.addUser("authHeader", user);
        verify(userService).addUser(any(com.rackspace.idm.domain.entity.User.class));
    }

    @Test
    public void addUser_callsUserConverter_toUserJaxb() throws Exception {
        User user = new User();
        when(userConverter.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        usersResource.addUser("authHeader", user);
        verify(userConverter).toUserJaxb(any(com.rackspace.idm.domain.entity.User.class));
    }

    @Test
    public void addUser_responseCreated_returns201() throws Exception {
        User user = new User();
        when(userConverter.toUserDO(user)).thenReturn(new com.rackspace.idm.domain.entity.User());
        Response response = usersResource.addUser("authHeader", user);
        assertThat("response code", response.getStatus(), equalTo(201));
    }

    @Test
    public void getUserResource_returnsUserResource() throws Exception {
        UserResource resource = usersResource.getUserResource();
        assertThat("users resource", resource, equalTo(singleUserResource));
    }
}
