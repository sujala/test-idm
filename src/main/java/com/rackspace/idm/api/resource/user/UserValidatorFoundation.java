package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
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

    @Autowired
    private Configuration config;

    @Autowired
    private CloudClient cloudClient;

    public void isUsernameEmpty(String username){
        if(username.isEmpty()){
            throw new BadRequestException("Username cannot be empty.");
        }
    }

    public void validateUsername(String username) {
        Pattern alphaNumeric = Pattern.compile("[a-zA-z0-9]*");
        if (!alphaNumeric.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if (!CharUtils.isAsciiAlpha(username.charAt(0))) {
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }
}
