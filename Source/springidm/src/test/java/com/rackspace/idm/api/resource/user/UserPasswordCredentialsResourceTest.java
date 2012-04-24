package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/23/12
 * Time: 1:22 PM
 */
public class UserPasswordCredentialsResourceTest {

    UserPasswordCredentialsResource userPasswordCredentialsResource;
    AuthorizationService authorizationService;
    ScopeAccessService scopeAccessService;
    UserService userService;
    PasswordConverter passwordConverter;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        passwordConverter = mock(PasswordConverter.class);

        when(userService.loadUser(anyString())).thenReturn(new User());
        
        userPasswordCredentialsResource = new UserPasswordCredentialsResource(scopeAccessService,userService,passwordConverter,null,authorizationService,null,null);
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
}
