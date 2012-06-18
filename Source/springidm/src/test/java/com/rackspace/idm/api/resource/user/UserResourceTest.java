package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 4:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserResourceTest {
    private UserResource userResource;
    private UserApplicationsResource userApplicationsResource;
    private ScopeAccessService scopeAccessService;
    private UserPasswordCredentialsResource userPasswordCredentialsResource;
    private UserTenantsResource userTenantsResource;
    private UserSecretResource userSecretResource;
    private UserDelegatedRefreshTokensResource userDelegatedRefreshTokensResource;
    private UserService userService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    private UserGlobalRolesResource userGlobalRolesResource;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        userApplicationsResource = mock(UserApplicationsResource.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userPasswordCredentialsResource = mock(UserPasswordCredentialsResource.class);
        userTenantsResource = mock(UserTenantsResource.class);
        userSecretResource = mock(UserSecretResource.class);
        userDelegatedRefreshTokensResource = mock(UserDelegatedRefreshTokensResource.class);
        userService = mock(UserService.class);
        userConverter = mock(UserConverter.class);
        authorizationService = mock(AuthorizationService.class);
        userGlobalRolesResource = mock(UserGlobalRolesResource.class);
        inputValidator = mock(InputValidator.class);
        userResource = new UserResource(userApplicationsResource, scopeAccessService, userPasswordCredentialsResource, userTenantsResource,
                userSecretResource, userDelegatedRefreshTokensResource, userService, userConverter, inputValidator, authorizationService, userGlobalRolesResource);
    }

    @Test
    public void getUserById_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        userResource.getUserById("authHeader", "userId");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void getUserById_callsAuthService_authorizeRackspaceClient() throws Exception {
        userResource.getUserById("authHeader", "userId");
        verify(authorizationService).authorizeRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void getUserById_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userResource.getUserById("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getUserById_callsUserService_loadUser() throws Exception {
        when(authorizationService.authorizeRackspaceClient(any(ScopeAccess.class))).thenReturn(true);
        userResource.getUserById("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getUserById_callsUserConverter_toUserJaxbWithoutAnyAdditionalElements() throws Exception {
        userResource.getUserById("authHeader", "userId");
        verify(userConverter).toUserJaxbWithoutAnyAdditionalElements(any(User.class));
    }

    @Test
    public void getUserById_responseOk_returns200() throws Exception {
        Response response = userResource.getUserById("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void updateUser_callsAuthService_authorizeRackspaceClient() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(authorizationService).authorizeRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void updateUser_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void updateUser_callsScopeAccessService_expireAllTokensForUser() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(scopeAccessService).expireAllTokensForUser(anyString());
    }

    @Test
    public void updateUser_callsUserService_updateUserById() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(userService).updateUserById(any(User.class), eq(false));
    }

    @Test
    public void updateUser_callsUserConverter_toUserWithoutAnyAdditionalElements() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(new User());
        when(userService.loadUser("userId")).thenReturn(new User());
        userResource.updateUser("authHeader", "userId", holder);
        verify(userConverter).toUserJaxbWithoutAnyAdditionalElements(any(User.class));
    }

    @Test
    public void updateUser_responseOk_returns200() throws Exception {
        com.rackspace.api.idm.v1.User user = new com.rackspace.api.idm.v1.User();
        User updatedUser = new User();
        updatedUser.setEnabled(false);
        EntityHolder<com.rackspace.api.idm.v1.User> holder = new EntityHolder<com.rackspace.api.idm.v1.User>(user);
        when(authorizationService.authorizeRackspaceClient(any(ScopeAccess.class))).thenReturn(true);
        when(userConverter.toUserDO(any(com.rackspace.api.idm.v1.User.class))).thenReturn(updatedUser);
        when(userService.loadUser("userId")).thenReturn(new User());
        Response response = userResource.updateUser("authHeader", "userId", holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUser_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        userResource.deleteUser("authHeader", "userId");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void deleteUser_callsAuthService_authorizeRackspaceClient() throws Exception {
        userResource.deleteUser("authHeader", "userId");
        verify(authorizationService).authorizeRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void deleteUser_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userResource.deleteUser("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void deleteUser_callsUserService_loadUser() throws Exception {
        when(authorizationService.authorizeRackspaceClient(any(ScopeAccess.class))).thenReturn(true);
        userResource.deleteUser("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void deleteUser_callsUserService_softDeleteUser() throws Exception {
        when(authorizationService.authorizeRackspaceClient(any(ScopeAccess.class))).thenReturn(true);
        userResource.deleteUser("authHeader", "userId");
        verify(userService).softDeleteUser(any(User.class));
    }

    @Test
    public void deleteUser_responseNoContent_returns204() throws Exception {
        Response response = userResource.deleteUser("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void getUserSecretResource_returnUserSecretResource() throws Exception {
        UserSecretResource resource = userResource.getUserSecretResource();
        assertThat("user secret resrouce", resource, equalTo(userSecretResource));
    }

    @Test
    public void getPasswordCredentialResource_returnUserPasswordCredentialResource() throws Exception {
        UserPasswordCredentialsResource resource = userResource.getPasswordCredentialsResource();
        assertThat("user password credential resource", resource, equalTo(userPasswordCredentialsResource));
    }

    @Test
    public void getUserTokenResource_returnUserDelegatedRefreshTokenResource() throws Exception {
        UserDelegatedRefreshTokensResource resource = userResource.getUserTokenResource();
        assertThat("user delegated refresh tokens resource", resource, equalTo(userDelegatedRefreshTokensResource));
    }

    @Test
    public void getApplications_returnUserApplicationsResource() throws Exception {
        UserApplicationsResource resource = userResource.getApplications();
        assertThat("user applications resource", resource, equalTo(userApplicationsResource));
    }

    @Test
    public void getTenantsResource_returnUserTenantResource() throws Exception {
        UserTenantsResource resource = userResource.getTenantsResource();
        assertThat("user tenants resource", resource, equalTo(userTenantsResource));
    }

    @Test
    public void getGlobalRolesResource_returnUserGlobalRolesResource() throws Exception {
        UserGlobalRolesResource resource = userResource.getGlobalRolesResource();
        assertThat("user global roles resource", resource, equalTo(userGlobalRolesResource));
    }
}
