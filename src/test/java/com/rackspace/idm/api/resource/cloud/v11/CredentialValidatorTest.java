package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/18/12
 * Time: 10:55 AM
 */
public class CredentialValidatorTest {

    CredentialValidator credentialValidator;
    UserService userService;

    @Before
    public void setUp() throws Exception {
        credentialValidator = new CredentialValidator();
        userService = mock(UserService.class);
    }

    @Test (expected = BadRequestException.class)
    public void validateNastCredentials_nastIdIsBlank_throwsBadRequestException() throws Exception {
        NastCredentials credential = new NastCredentials();
        credential.setNastId("");
        credentialValidator.validateCredential(credential, userService);
    }

    @Test
    public void validateNastCredentials_callsUserService_getUserByNastId() throws Exception {
        NastCredentials credential = new NastCredentials();
        credential.setNastId("nastId");
        User user = new User();
        user.setEnabled(true);
        when(userService.getUserByTenantId("nastId")).thenReturn(user);
        credentialValidator.validateCredential(credential, userService);
        verify(userService).getUserByTenantId("nastId");
    }

    @Test (expected = NotAuthenticatedException.class)
    public void validateNastCredentials_userIsNull_throwsNotAuthenticatedException() throws Exception {
        NastCredentials credential = new NastCredentials();
        credential.setNastId("nastId");
        when(userService.getUserByTenantId("nastId")).thenReturn(null);
        credentialValidator.validateCredential(credential, userService);
    }

    @Test (expected = UserDisabledException.class)
    public void validateNastCredentials_userIsDisabled_throwsUserDisabledException() throws Exception {
        NastCredentials credential = new NastCredentials();
        credential.setNastId("nastId");
        User user = new User();
        user.setNastId("nastId");
        user.setEnabled(false);
        when(userService.getUserByTenantId("nastId")).thenReturn(user);
        credentialValidator.validateCredential(credential, userService);
    }

    @Test
    public void validateNastCredentials_userNotNull_doesNotThrowException() throws Exception {
        NastCredentials credential = new NastCredentials();
        credential.setNastId("nastId");
        User user = new User();
        user.setNastId("nastId");
        user.setEnabled(true);
        when(userService.getUserByTenantId("nastId")).thenReturn(user);
        try {
            credentialValidator.validateCredential(credential, userService);
        } catch (Exception e) {
            fail("Should not have thrown an exception.");
        }
    }

    @Test (expected = BadRequestException.class)
    public void validateUserCredentials_apiKeyIsBlank_throwsBadRequestException() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setKey("");
        credentialValidator.validateCredential(userCredentials, userService);
    }

    @Test (expected = BadRequestException.class)
    public void validateUserCredentials_usernameIsBlank_throwsBadRequestException() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setKey("apiKey");
        userCredentials.setUsername("");
        credentialValidator.validateCredential(userCredentials, userService);
    }

    @Test
    public void validateUserCredentials_apiKeyAndUsernameNotBlank_doesNotThrowException() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setKey("apiKey");
        userCredentials.setUsername("username");
        try {
            credentialValidator.validateCredential(userCredentials, userService);
            assertTrue(true);
        } catch (Exception e) {
            fail("Should not have thrown an exception.");
        }
    }

    @Test (expected = BadRequestException.class)
    public void validatePasswordCredentials_passwordIsBlank_throwsBadRequestException() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("");
        credentialValidator.validateCredential(passwordCredentials, userService);
    }

    @Test (expected = BadRequestException.class)
    public void validatePasswordCredentials_usernameIsBlank_throwsBadRequestException() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("");
        credentialValidator.validateCredential(passwordCredentials, userService);
    }

    @Test
    public void validatePasswordCredentials_callsUserService_getUser() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("username");
        User user = new User();
        user.setEnabled(true);
        when(userService.getUser("username")).thenReturn(user);
        credentialValidator.validateCredential(passwordCredentials, userService);
        verify(userService).getUser("username");
    }

    @Test (expected = NotAuthorizedException.class)
    public void validatePasswordCredentials_userIsNull_throwsNotAuthorizedException() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("username");
        when(userService.getUser("username")).thenReturn(null);
        credentialValidator.validateCredential(passwordCredentials, userService);
    }

    @Test (expected = UserDisabledException.class)
    public void validatePasswordCredentials_userIsDisabled_throwsUserDisabledException() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("username");
        User user = new User();
        user.setEnabled(false);
        when(userService.getUser("username")).thenReturn(user);
        credentialValidator.validateCredential(passwordCredentials, userService);
    }

    @Test
    public void validatePasswordCredentials_userNotDisabled_doesNotThrowException() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setPassword("password");
        passwordCredentials.setUsername("username");
        User user = new User();
        user.setEnabled(true);
        when(userService.getUser("username")).thenReturn(user);
        try {
            credentialValidator.validateCredential(passwordCredentials, userService);
        } catch (Exception e) {
            fail("Should not have thrown an exception.");
        }
    }

    @Test (expected = BadRequestException.class)
    public void validateMossoCredentials_apiKeyIsBlank_throwsBadRequestException() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setKey("");
        credentialValidator.validateCredential(mossoCredentials, userService);
    }

    @Test (expected = NotAuthenticatedException.class)
    public void validateMossoCredentials_userIsNull_throwsNotAuthenticatedException() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setKey("apiKey");
        mossoCredentials.setMossoId(0);
        when(userService.getUserByTenantId("0")).thenReturn(null);
        credentialValidator.validateCredential(mossoCredentials, userService);
    }

    @Test (expected = UserDisabledException.class)
    public void validateMossoCredentials_userIsDisabled_throwsUserDisabledException() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setKey("apiKey");
        mossoCredentials.setMossoId(0);
        User user = new User();
        user.setMossoId(0);
        user.setEnabled(false);
        when(userService.getUserByTenantId("0")).thenReturn(user);
        credentialValidator.validateCredential(mossoCredentials, userService);
    }

    @Test
    public void validateMossoCredentials_userNotNull_doesNotThrowException() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setKey("apiKey");
        mossoCredentials.setMossoId(0);
        User user = new User();
        user.setMossoId(0);
        user.setEnabled(true);
        when(userService.getUserByTenantId("0")).thenReturn(user);
        try {
            credentialValidator.validateCredential(mossoCredentials, userService);
        } catch (Exception e) {
            fail("Should not have thrown an exception.");
        }
    }

    @Test (expected = BadRequestException.class)
    public void validateCredential_credentialTypeUnknown_throwsBadRequest() throws Exception {
        Credentials credentials = mock(Credentials.class);
        credentialValidator.validateCredential(credentials, userService);
    }
}
