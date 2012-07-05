package com.rackspace.idm.api.resource.user;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.UserPasswordCredentialsValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/23/12
 * Time: 1:22 PM
 */
public class UserPasswordCredentialsResourceTest {

    private UserPasswordCredentialsResource userPasswordCredentialsResource;
    private AuthorizationService authorizationService;
    private ScopeAccessService scopeAccessService;
    private UserService userService;
    private PasswordConverter passwordConverter;
    private UserPasswordCredentialsValidator userPasswordCredentialsValidator;
    private UserRecoveryTokenResource userRecoveryTokenResource;
    private InputValidator inputValidator;


    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        passwordConverter = mock(PasswordConverter.class);
        userPasswordCredentialsValidator = mock(UserPasswordCredentialsValidator.class);
        userRecoveryTokenResource = mock(UserRecoveryTokenResource.class);
        inputValidator = mock(InputValidator.class);

        when(userService.loadUser(anyString())).thenReturn(new User());
        
        userPasswordCredentialsResource = new UserPasswordCredentialsResource(scopeAccessService,userService,passwordConverter,userRecoveryTokenResource
                ,authorizationService,inputValidator,userPasswordCredentialsValidator);

    }

    @Test
    public void getUserPassword_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        userPasswordCredentialsResource.getUserPassword(null,null);
        verify(scopeAccessService).getScopeAccessByAccessToken(null);
    }

    @Test
    public void getUserPassword_callsAuthService_authorizeIdmSuperAdminOrRackspaceClient() throws Exception {
        userPasswordCredentialsResource.getUserPassword(null,null);
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void setUserPassword_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(new User());
        userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test (expected = NotFoundException.class)
    public void setUserPassword_userIsNull_throwsNotFound() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(null);
        userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
    }

    @Test
    public void setUserPassword_callsUserService_updateUser() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(new User());
        userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test (expected = BadRequestException.class)
    public void setUserPassword_updateUser_throwsBadRequest() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(new User());
        doThrow(new IllegalStateException("bad request", new Exception("bad request"))).when(userService).updateUser(any(User.class), eq(false));
        userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
    }

    @Test
    public void setUserPassword_updateUserThrowsExceptionNotInstanceOfIllegalStateExpcetion_returns204() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(new User());
        doThrow(new RuntimeException()).when(userService).updateUser(any(User.class), eq(false));
        Response result = userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
        assertThat("response code", result.getStatus(), equalTo(204));
    }

    @Test
    public void setUserPassword_responseNoContent_returns204() throws Exception {
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword password = new UserPassword();
        password.setPassword("password");
        userPasswordCredentials.setNewPassword(password);
        EntityHolder<UserPasswordCredentials> userCredentials = new EntityHolder<UserPasswordCredentials>(userPasswordCredentials);
        when(userService.getUserById("userId")).thenReturn(new User());
        Response response = userPasswordCredentialsResource.setUserPassword("authHeader", "userId", userCredentials);
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void resetUserPassword_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userPasswordCredentialsResource.resetUserPassword("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void resetUserPassword_callsUserService_loadUser() throws Exception {
        userPasswordCredentialsResource.resetUserPassword("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void resetUserPassword_callsUserService_resetUserPassword() throws Exception {
        userPasswordCredentialsResource.resetUserPassword("authHeader", "userId");
        verify(userService).resetUserPassword(any(User.class));
    }

    @Test
    public void resetUserPassword_responseOk_returns200() throws Exception {
        Response response = userPasswordCredentialsResource.resetUserPassword("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getRecoveryTokenResource_returnsRecoverTokenResource() throws Exception {
        UserRecoveryTokenResource resource = userPasswordCredentialsResource.getRecoveryTokenResource();
        assertThat("recovery token resource", resource, equalTo(userRecoveryTokenResource));
    }
}
