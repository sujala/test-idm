package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 8/3/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class Validator20 {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (username.contains(" ")) {
            String errorMsg = "Username should not contain white spaces";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validateUsernameForUpdateOrCreate(String username) {
        Pattern alphaNumberic = Pattern.compile("[a-zA-z0-9]*");
        if (!alphaNumberic.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if (!CharUtils.isAsciiAlpha(username.charAt(0))) {
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }

    public void validateEmail(String email) {
        if (StringUtils.isBlank(email) || !email.matches("[a-zA-Z0-9_\\-\\.\"]+@[a-zA-Z0-9_\\.]+\\.[a-zA-Z]+")) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validateUserForCreate(User user) {
        validateUsername(user.getUsername());
        validateUsernameForUpdateOrCreate(user.getUsername());
        validateEmail(user.getEmail());
    }

    public void validatePasswordCredentials(PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername) {
        String username = passwordCredentialsRequiredUsername.getUsername();
        String password = passwordCredentialsRequiredUsername.getPassword();
        validateUsername(username);
        if (StringUtils.isBlank(password)) {
            String errMsg = "Expecting password";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

}
