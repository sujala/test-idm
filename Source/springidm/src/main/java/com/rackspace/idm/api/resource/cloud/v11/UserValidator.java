package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.apache.commons.lang.CharUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/5/11
 * Time: 1:56 PM
 */
@Component
public class UserValidator {

    static final String USER_ID_EMPTY_MSG = "User cannot be empty.";
    static final String USER_ID_NULL_MSG = "User ID can not be null.";

    public void validate(User user) {
        if (user == null) {
            throw new BadRequestException(USER_ID_NULL_MSG);
        //} else if (user.getNastId() != null && user.getKey() != null) {
            //no op
        } else {
            if (user.getId() == null) {
                throw new BadRequestException(USER_ID_NULL_MSG);
            }
            if (user.getId().isEmpty()) {
                throw new BadRequestException(USER_ID_NULL_MSG);
            }
        }
    }

    public void validateUsername(String username){
        Pattern alphaNumberic = Pattern.compile("[a-zA-z0-9]*");
        if(!alphaNumberic.matcher(username).matches()){
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if(!CharUtils.isAsciiAlpha(username.charAt(0))){
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }

}
