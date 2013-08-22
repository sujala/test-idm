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
    private ScopeAccessService scopeAccessService;
    private UserPasswordCredentialsResource userPasswordCredentialsResource;
    private UserService userService;
    private UserConverter userConverter;
    private AuthorizationService authorizationService;
    private UserGlobalRolesResource userGlobalRolesResource;
    private InputValidator inputValidator;
    private UserValidatorFoundation userValidator;

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        userPasswordCredentialsResource = mock(UserPasswordCredentialsResource.class);
        userService = mock(UserService.class);
        userConverter = mock(UserConverter.class);
        authorizationService = mock(AuthorizationService.class);
        userGlobalRolesResource = mock(UserGlobalRolesResource.class);
        inputValidator = mock(InputValidator.class);
        userValidator = mock(UserValidatorFoundation.class);
        userResource = new UserResource(scopeAccessService, userPasswordCredentialsResource,
                userService, userConverter, inputValidator, authorizationService, userGlobalRolesResource);
        userResource.setUserValidator(userValidator);
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
    public void getPasswordCredentialResource_returnUserPasswordCredentialResource() throws Exception {
        UserPasswordCredentialsResource resource = userResource.getPasswordCredentialsResource();
        assertThat("user password credential resource", resource, equalTo(userPasswordCredentialsResource));
    }

    @Test
    public void getGlobalRolesResource_returnUserGlobalRolesResource() throws Exception {
        UserGlobalRolesResource resource = userResource.getGlobalRolesResource();
        assertThat("user global roles resource", resource, equalTo(userGlobalRolesResource));
    }
}
