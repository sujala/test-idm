package com.rackspace.idm.validation;

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

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials, User user) {
        if( !user.getPassword().equals(userPasswordCredentials.getCurrentPassword().getPassword()) ){
            throw new BadRequestException("Invalid current password");
        }
        this.validateCurrentPassword(userPasswordCredentials);
    }

    public void validateCurrentPassword(UserPasswordCredentials userPasswordCredentials) {
        String currentPassword = userPasswordCredentials.getCurrentPassword().getPassword();
        if (currentPassword == null || currentPassword.equals("")){
            throw new BadRequestException("Invalid current password");
        }
    }
}
