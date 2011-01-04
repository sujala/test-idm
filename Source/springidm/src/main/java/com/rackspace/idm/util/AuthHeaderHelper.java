package com.rackspace.idm.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.exceptions.NotAuthorizedException;

public class AuthHeaderHelper {

    public Map<String, String> parseBasicParams(String authHeader) {
        String encoded = authHeader.substring(authHeader.indexOf(' '));
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
            throw new NotAuthorizedException("Invalid Auth Header");
        }

        String tokenString = null;

        Map<String, String> tokenParams = parseTokenParams(authHeader);

        if (tokenParams.isEmpty() || !tokenParams.containsKey("token")) {
            throw new NotAuthorizedException("Invalid Auth Header");
        }

        tokenString = tokenParams.get("token");

        if (StringUtils.isBlank(tokenString)) {
            throw new NotAuthorizedException("Invalid Auth Header");
        }

        return tokenString;
    }
}
