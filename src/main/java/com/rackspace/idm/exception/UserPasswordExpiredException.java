package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.User;
import lombok.Getter;

public class UserPasswordExpiredException extends IdmException {

    @Getter
    private User user;

    public UserPasswordExpiredException(User user, String message) {
        super(message, ErrorCodes.ERROR_CODE_PASSWORD_EXPIRED);
        this.user = user;
    }
}
