package com.rackspace.idm.api.resource.user;

import com.rackspace.api.idm.v1.UserSecret;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
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
 * Time: 5:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserSecretResourceTest {
    private UserSecretResource userSecretResource;
    private UserService userService;
    private AuthorizationService authorizationService;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);
        userSecretResource = new UserSecretResource(userService, authorizationService, inputValidator);

        when(userService.loadUser(anyString())).thenReturn(new User());
    }

    @Test
    public void getUserSecret_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userSecretResource.getUserSecret("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getUserSecret_responseOk_returns200() throws Exception {
        Response response = userSecretResource.getUserSecret("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void setUserSecret_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("answer");
        userSecret.setSecretQuestion("question");
        EntityHolder<UserSecret> holder = new EntityHolder<UserSecret>(userSecret);
        userSecretResource.setUserSecret("authHeader", "userId", holder);
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void setUserSecret_callsUserService_updateUser() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("answer");
        userSecret.setSecretQuestion("question");
        EntityHolder<UserSecret> holder = new EntityHolder<UserSecret>(userSecret);
        userSecretResource.setUserSecret("authHeader", "userId", holder);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void setUserSecret_responseNoContent_returns204() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("answer");
        userSecret.setSecretQuestion("question");
        EntityHolder<UserSecret> holder = new EntityHolder<UserSecret>(userSecret);
        Response response = userSecretResource.setUserSecret("authHeader", "userId", holder);
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test (expected = BadRequestException.class)
    public void validateRequestBody_secretQuestionIsBlank_throwBadRequest() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("answer");
        userSecret.setSecretQuestion("");
        EntityHolder<UserSecret> holder = new EntityHolder<UserSecret>(userSecret);
        userSecretResource.validateRequestBody(holder);
    }

    @Test (expected = BadRequestException.class)
    public void validateRequestBody_secretAnswerIsBlank_throwBadRequest() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("");
        userSecret.setSecretQuestion("question");
        EntityHolder<UserSecret> holder = new EntityHolder<UserSecret>(userSecret);
        userSecretResource.validateRequestBody(holder);
    }
}
