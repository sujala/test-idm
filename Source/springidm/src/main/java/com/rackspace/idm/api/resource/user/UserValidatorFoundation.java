package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.CharUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 7/9/12
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class UserValidatorFoundation {
    public void validateUsername(String username) {
        Pattern alphaNumberic = Pattern.compile("[a-zA-z0-9]*");
        if (!alphaNumberic.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if (!CharUtils.isAsciiAlpha(username.charAt(0))) {
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }
}
