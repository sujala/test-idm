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

import javax.ws.rs.core.MultivaluedMap;
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

    public void validateUsername(String username) {
        Pattern alphaNumeric = Pattern.compile("[a-zA-z0-9]*");
        if (!alphaNumeric.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters; only alphanumeric characters are allowed.");
        }
        if (!CharUtils.isAsciiAlpha(username.charAt(0))) {
            throw new BadRequestException("Username must begin with an alphabetic character.");
        }
    }

    public void checkCloudAuthForUsername(String username) throws IOException {
        if (!StringUtils.isBlank(username)) {
            HashMap<String,String> httpHeaders = new HashMap<String, String>();
            String gaUserUsername = config.getString("ga.username");
            String gaUserPassword = config.getString("ga.password");
            httpHeaders.put(org.apache.http.HttpHeaders.AUTHORIZATION, getBasicAuth(gaUserUsername, gaUserPassword));
            //search for user in US Cloud Auth
            String uri = config.getString("cloudAuth11url") + "users/" + username;
            Response.ResponseBuilder cloudAuthUSResponse = cloudClient.get(uri, httpHeaders);
            int status = cloudAuthUSResponse.build().getStatus();
            if (status == 200) {
                throw new DuplicateUsernameException(String.format("Username %s already exists", username));
            }
            //search for user in UK Cloud Auth
            String ukUri = config.getString("cloudAuthUK11url") + "users/" + username;
            Response.ResponseBuilder cloudAuthUKResponse = cloudClient.get(ukUri, httpHeaders);
            status = cloudAuthUKResponse.build().getStatus();
            if (status == 200) {
                throw new DuplicateUsernameException(String.format("Username %s already exists", username));
            }
        }
    }

    private String getBasicAuth(String username, String password) {
        String usernamePassword = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = usernamePassword.getBytes();
        return (new StringBuffer("Basic ").append(Base64.encode(base))).toString();
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }
}
