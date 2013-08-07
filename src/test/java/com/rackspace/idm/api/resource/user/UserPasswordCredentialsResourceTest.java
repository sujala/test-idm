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
    private InputValidator inputValidator;


    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        passwordConverter = mock(PasswordConverter.class);
        inputValidator = mock(InputValidator.class);

        when(userService.loadUser(anyString())).thenReturn(new User());
        
        userPasswordCredentialsResource = new UserPasswordCredentialsResource(scopeAccessService, userService,passwordConverter
                ,authorizationService,inputValidator);

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
