package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/5/11
 * Time: 1:56 PM
 */
@Component
public class UserValidator {

    static final String USER_ID_EMPTY_MSG = "User Id Cannot be empty.";

    public void validate(User user) {
        if (user == null) {
            throw new BadRequestException("User can not be null");
        }
        if (user.getId() == null) {
            throw new BadRequestException(USER_ID_EMPTY_MSG);
        }
        if (user.getId().isEmpty()) {
            throw new BadRequestException(USER_ID_EMPTY_MSG);
        }
    }
}
