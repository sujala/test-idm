package com.rackspace.idm.util;

import org.springframework.stereotype.Component;

import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthHeaderHelper {

    public static final String INVALID_AUTH_HEADER = "Invalid Auth Header";

    public Map<String, String> parseBasicParams(String authHeader) {
        String encoded = this.getBase64EncodedString(authHeader);
        Map<String, String> authParams = new HashMap<String, String>();

        String decoded = new String(Base64.decodeBase64(encoded));

        try {
            String[] keyValPairs = decoded.split(":");

            authParams.put("username", keyValPairs[0]);
            authParams.put("password", keyValPairs[1]);
        } catch (Exception e) {
            return null;
        }
        return authParams;
    }

    public Map<String, String> parseTokenParams(String authHeader) {
        Map<String, String> authParams = new HashMap<String, String>();
        if (StringUtils.isBlank(authHeader)) {
            return authParams;
        }

        String[] headerparm = authHeader.split(" ");

        if (headerparm.length > 1 && headerparm[0].toLowerCase().trim().equals("oauth")) {
            authParams.put("token", headerparm[1].trim());
        }

        return authParams;
    }

    public String getTokenFromAuthHeader(String authHeader) {
        if (StringUtils.isBlank(authHeader)) {
            throw new NotAuthorizedException(INVALID_AUTH_HEADER);
        }

        return authHeader.trim();
    }

    public String getBase64EncodedString(String authHeader) {
        if (authHeader == null) {
            throw new CloudAdminAuthorizationException(INVALID_AUTH_HEADER);
        }
        if (authHeader.isEmpty()) {
            throw new CloudAdminAuthorizationException(INVALID_AUTH_HEADER);
        }
        String[] strings = authHeader.split(" ");
        if(strings.length!=2){
            throw new CloudAdminAuthorizationException(INVALID_AUTH_HEADER);
        }
        if(!strings[0].equals("Basic")){
            throw new CloudAdminAuthorizationException(INVALID_AUTH_HEADER);
        }
        return strings[1];
    }
}
