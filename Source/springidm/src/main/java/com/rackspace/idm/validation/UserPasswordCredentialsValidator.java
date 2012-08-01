package com.rackspace.idm.validation;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * User: alan.erwin
 * Date: 4/12/12
 * Time: 10:00 AM
 */
@Component
public class UserPasswordCredentialsValidator {

    public static final String INVALID_REQUEST = "Invalid request";

    public void validateUserPasswordCredentials(UserPasswordCredentials userPasswordCredentials, User user) {
        if(userPasswordCredentials.isVerifyCurrentPassword()){
            validateCurrentPassword(userPasswordCredentials, user);
        }
        validateNewPassword(userPasswordCredentials);
    }

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials, User user) {
        UserPassword currentPassword = userPasswordCredentials.getCurrentPassword();
        if (currentPassword == null) {
            throw new BadRequestException(INVALID_REQUEST);
        }
        String password = userPasswordCredentials.getCurrentPassword().getPassword();
        if (password == null || password.equals("")) {
            throw new BadRequestException(INVALID_REQUEST);
        }
        if (!user.getPassword().equals(password)) {
            throw new BadRequestException(INVALID_REQUEST);
        }
    }

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials) {
        String currentPassword = userPasswordCredentials.getCurrentPassword().getPassword();
        if (currentPassword == null || currentPassword.equals("")) {
            throw new BadRequestException(INVALID_REQUEST);
        }
    }

    public void validateNewPassword(UserPasswordCredentials userPasswordCredentials) {
        UserPassword newPassword = userPasswordCredentials.getNewPassword();
        if (newPassword == null) {
            throw new BadRequestException(INVALID_REQUEST);
        }
        String password = userPasswordCredentials.getNewPassword().getPassword();
        if (password == null || password.equals("")) {
            throw new BadRequestException(INVALID_REQUEST);
        }
    }


}
