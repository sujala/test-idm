package com.rackspace.idm.validation;


import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * User: alan.erwin
 * Date: 4/12/12
 * Time: 9:53 AM
 */
public class UserPasswordCredentialsValidatorTest {
    User user;

    UserPasswordCredentialsValidator userPasswordCredentialsValidator;

    @Before
    public void setUp() throws Exception {
        userPasswordCredentialsValidator = new UserPasswordCredentialsValidator();
        user = mock(User.class);
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
}
