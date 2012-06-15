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

    public void validateUserPasswordCredentials(UserPasswordCredentials userPasswordCredentials, User user) {
        if(userPasswordCredentials.isVerifyCurrentPassword()){
            validateCurrentPassword(userPasswordCredentials, user);
        }
        validateNewPassword(userPasswordCredentials);
    }

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials, User user) {
        UserPassword currentPassword = userPasswordCredentials.getCurrentPassword();
        if (currentPassword == null) {
            throw new BadRequestException("Invalid request");
        }
        String password = userPasswordCredentials.getCurrentPassword().getPassword();
        if (password == null || password.equals("")) {
            throw new BadRequestException("Invalid request");
        }
        if (!user.getPassword().equals(password)) {
            throw new BadRequestException("Invalid request");
        }
    }

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials) {
        String currentPassword = userPasswordCredentials.getCurrentPassword().getPassword();
        if (currentPassword == null || currentPassword.equals("")) {
            throw new BadRequestException("Invalid request");
        }
    }

    public void validateNewPassword(UserPasswordCredentials userPasswordCredentials) {
        UserPassword newPassword = userPasswordCredentials.getNewPassword();
        if (newPassword == null) {
            throw new BadRequestException("Invalid request");
        }
        String password = userPasswordCredentials.getNewPassword().getPassword();
        if (password == null || password.equals("")) {
            throw new BadRequestException("Invalid request");
        }
    }


}
