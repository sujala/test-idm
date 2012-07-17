package com.rackspace.idm.validation;


import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: alan.erwin
 * Date: 4/12/12
 * Time: 9:53 AM
 */
public class UserPasswordCredentialsValidatorTest {
    User user;

    UserPasswordCredentialsValidator userPasswordCredentialsValidator;
    UserPasswordCredentialsValidator spy;

    @Before
    public void setUp() throws Exception {
        userPasswordCredentialsValidator = new UserPasswordCredentialsValidator();
        user = mock(User.class);

        spy = spy(userPasswordCredentialsValidator);
    }

    @Test
    public void validateUserPasswordCredentials_callsValidateNewPassword() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("wrongPassword");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(false);
        userPasswordCredentials.setCurrentPassword(userPassword);
        doNothing().when(spy).validateNewPassword(userPasswordCredentials);
        spy.validateUserPasswordCredentials(userPasswordCredentials, user);
        verify(spy).validateNewPassword(userPasswordCredentials);
    }

    @Test (expected = BadRequestException.class)
    public void validateUserPasswordCredentials_userPasswordNotMatchCurrentPassword_throwBadRquest() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("wrongPassword");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        User user = new User();
        user.setPassword("rightPassword");
        userPasswordCredentialsValidator.validateUserPasswordCredentials(userPasswordCredentials, user);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_currentPasswordIsNull_throwsBadRequest() throws Exception {
        userPasswordCredentialsValidator.validateCurrentPassword(new UserPasswordCredentials(), null);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_passwordIsNull_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        User user = new User();
        userPasswordCredentialsValidator.validateUserPasswordCredentials(userPasswordCredentials, user);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_passwordIsBlank_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        User user = new User();
        userPasswordCredentialsValidator.validateUserPasswordCredentials(userPasswordCredentials, user);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_userPasswordNotMatchCurrentPassword_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("wrongPassword");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        User user = new User();
        user.setPassword("rightPassword");
        userPasswordCredentialsValidator.validateUserPasswordCredentials(userPasswordCredentials, user);
    }

    @Test
    public void validateCurrentPassword_userPasswordMatchCurrentPassword_doesNothing() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("Password");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        User user = new User();
        user.setPassword("Password");
        userPasswordCredentialsValidator.validateCurrentPassword(userPasswordCredentials, user);
    }

    @Test(expected = BadRequestException.class)
    public void validateCurrentPassword_withUserWrongCurrentPassword_throwBadRequest() throws Exception {
        when(user.getPassword()).thenReturn("password");
        UserPasswordCredentials mockCreds = mock(UserPasswordCredentials.class);
        UserPassword mockCurrentPassword = mock(UserPassword.class);
        mockCurrentPassword.setPassword("testPassword");
        when(mockCreds.getCurrentPassword()).thenReturn(mockCurrentPassword);
        userPasswordCredentialsValidator.validateUserPasswordCredentials(mockCreds, user);
    }

    @Test(expected = BadRequestException.class)
    public void validateCurrentPassword_withOutUserWithBlank_throwBadRequest() throws Exception {
        UserPasswordCredentials mockCreds = mock(UserPasswordCredentials.class);
        UserPassword mockCurrentPassword = mock(UserPassword.class);
        mockCurrentPassword.setPassword("");
        when(mockCreds.getCurrentPassword()).thenReturn(mockCurrentPassword);
        userPasswordCredentialsValidator.validateCurrentPassword(mockCreds);
    }

    @Test(expected = BadRequestException.class)
    public void validateCurrentPassword_withOutUserWithNull_throwBadRequest() throws Exception {
        UserPasswordCredentials mockCreds = mock(UserPasswordCredentials.class);
        UserPassword mockCurrentPassword = mock(UserPassword.class);
        mockCurrentPassword.setPassword(null);
        when(mockCreds.getCurrentPassword()).thenReturn(mockCurrentPassword);
        userPasswordCredentialsValidator.validateCurrentPassword(mockCreds);
    }

    @Test(expected = BadRequestException.class)
    public void validateNewPassword_withOutUserWithBlank_throwBadRequest() throws Exception {
        UserPasswordCredentials mockCreds = mock(UserPasswordCredentials.class);
        UserPassword mockCurrentPassword = mock(UserPassword.class);
        mockCurrentPassword.setPassword("");
        when(mockCreds.getNewPassword()).thenReturn(mockCurrentPassword);
        userPasswordCredentialsValidator.validateNewPassword(mockCreds);
    }

    @Test(expected = BadRequestException.class)
    public void validateNewPassword_withOutUserWithNull_throwBadRequest() throws Exception {
        UserPasswordCredentials mockCreds = mock(UserPasswordCredentials.class);
        UserPassword mockCurrentPassword = mock(UserPassword.class);
        mockCurrentPassword.setPassword(null);
        when(mockCreds.getNewPassword()).thenReturn(mockCurrentPassword);
        userPasswordCredentialsValidator.validateNewPassword(mockCreds);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_withOnlyUserPasswordCredentialsParamCurrentPasswordIsNull_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        userPasswordCredentialsValidator.validateCurrentPassword(userPasswordCredentials);
    }

    @Test (expected = BadRequestException.class)
    public void validateCurrentPassword_withOnlyUserPasswordCredentialsParamCurrentPasswordIsBlank_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        userPasswordCredentialsValidator.validateCurrentPassword(userPasswordCredentials);
    }

    @Test
    public void validateCurrentPassword_withOnlyUserPasswordCredentialsParamCurrentPasswordNotBlank_doesNothing() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setCurrentPassword(userPassword);
        userPasswordCredentialsValidator.validateCurrentPassword(userPasswordCredentials);
    }

    @Test (expected = BadRequestException.class)
    public void validateNewPassword_passwordIsNull_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setNewPassword(userPassword);
        userPasswordCredentialsValidator.validateNewPassword(userPasswordCredentials);
    }

    @Test (expected = BadRequestException.class)
    public void validateNewPassword_passwordIsBlank_throwsBadRequest() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setNewPassword(userPassword);
        userPasswordCredentialsValidator.validateNewPassword(userPasswordCredentials);
    }

    @Test
    public void validateNewPassword_passwordNotBlank_doesNothing() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        userPasswordCredentials.setVerifyCurrentPassword(true);
        userPasswordCredentials.setNewPassword(userPassword);
        userPasswordCredentialsValidator.validateNewPassword(userPasswordCredentials);
    }
}
