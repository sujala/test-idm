package com.rackspace.idm.api.resource.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rackspace.idm.domain.service.UserService;
import com.sun.jersey.core.util.Base64;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("analyticsLogger")
public class DefaultAnalyticsLogger implements AnalyticsLogger {

    @Autowired
    Configuration config;

    @Autowired
    UserService userService;

    @Autowired
    AnalyticsLogHandler analyticsLogHandler;

    private static final String TOKENS = "tokens";
    private static final String USERS = "users";
    private static final String OBFUSCATED_TOKEN = "XXXX";

    public void log(Long startTime, String authToken, String basicAuth, String host, String remoteHost, String userAgent, String method, String path, int status, String requestBody, String contentType) {
        long duration = new Date().getTime() - startTime;

        Message message = new Message();
        message.setTimestamp(String.valueOf(startTime));
        message.setDuration(String.valueOf(duration));

        Caller caller = new Caller();
        caller.setIp(remoteHost);
        if (authToken != null) {
            caller.setToken(hashToken(authToken));
            caller.setId(getUserIdFromAuthToken(authToken));
        } else if (basicAuth != null) {
            caller.setId(getUserIdFromBasicAuth(basicAuth));
        }
        caller.setAgent(userAgent);
        message.setCaller(caller);

        com.rackspace.idm.domain.entity.User userEntity = null;

        String userId = getUserIdFromPath(path);
        if (userId != null) {
            userEntity = userService.getUserById(userId);
        }

        if (userEntity == null) {
            String username = getUsernameFromRequestBody(requestBody, contentType);
            if (username != null) {
                userEntity = userService.getUser(username);
            }
        }

        if (userEntity != null) {
            User user = new User();
            user.setId(userEntity.getId());
            user.setUsername(userEntity.getUsername());
            user.setDomain(userEntity.getDomainId());
            message.setUser(user);
        }

        Resource resource = new Resource();
        resource.setUri(getUri(host, getPathWithoutToken(path)));
        resource.setMethod(method);
        resource.setResponseStatus(status);
        message.setResource(resource);

        Gson gson = new GsonBuilder().create();
        String messageString = gson.toJson(message);

        analyticsLogHandler.log(messageString);
    }

    private String hashToken(String authToken) {
        try {
            InputStream is = new ByteArrayInputStream( authToken.getBytes());
            return DigestUtils.md5Hex(is);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        }
        return OBFUSCATED_TOKEN;
    }

    private String getPathWithoutToken(String path) {
        String token = parseUserTokenFromPath(path);
        if (token != null) {
            return path.replace(token, hashToken(token));
        }
        return path;
    }

    private String getUserIdFromBasicAuth(String basicAuth) {
        String username = getUsernameFromDecoded(getDecodedAuth(basicAuth));
        if (username != null) {
            com.rackspace.idm.domain.entity.User caller = userService.getUser(username);
            if (caller != null) {
                return caller.getId();
            }
        }
        return null;
    }

    private String getUsernameFromDecoded(String authentication) {
        if (authentication != null) {
            List<String> usernameAndPassword = Arrays.asList(authentication.split(":"));
            if (usernameAndPassword.size() == 2) {
                return usernameAndPassword.get(0);
            }
        }
        return null;
    }

    private String getDecodedAuth(String basicAuth) {
        if (basicAuth != null) {
            List<String> authHeader = Arrays.asList(basicAuth.split(" "));
            if (authHeader.size() == 2) {
                return Base64.base64Decode(authHeader.get(1));
            }
        }
        return null;
    }

    private String getUserIdFromPath(String path) {
        String userId = null;
        String token = parseUserTokenFromPath(path);

        if (token != null) {
            userId = getUserIdFromAuthToken(token);
        } else {
            userId = parseUserIdFromPath(path);
        }
        return userId;
    }

    private String parseUserTokenFromPath(String path) {
        return parseValueFromPath(TOKENS, path);
    }

    private String parseUserIdFromPath(String path) {
        return parseValueFromPath(USERS, path);
    }

    private String parseValueFromPath(String find, String path) {
        if (path != null) {
            List<String> tokens = Arrays.asList(path.split("/"));
            int index = tokens.indexOf(find);
            if (index >= 0) {
                if (index + 1 < tokens.size()) {
                    return tokens.get(index + 1);
                }
            }
        }
        return null;
    }

    private String getUserIdFromAuthToken(String authToken) {
        com.rackspace.idm.domain.entity.User user = userService.getUserByAuthToken(authToken);
        if (user != null) {
            return user.getId();
        }
        return null;
    }

    private String getUri(String host, String path) {
        try {
            return new URL(new URL("https://" + host), path).toString();
        } catch (MalformedURLException e) {
        }
        return "";
    }

    private String getUsernameFromRequestBody(String requestBody, String contentType) {
        if (contentType != null && requestBody != null) {
            if (contentType.equalsIgnoreCase("application/json")) {
                Pattern pattern = Pattern.compile(".*\"(?i:username)\"\\s*:\\s*\"([^\"]+)\".*");
                Matcher matcher = pattern.matcher(requestBody);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            } else if (contentType.equalsIgnoreCase("application/xml")) {
                Pattern pattern = Pattern.compile(".*(?i:username)\\s*=\\s*\"([^\"]+)\".*");
                Matcher matcher = pattern.matcher(requestBody);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    @Data
    private class Message {
        private String timestamp;
        private String duration;
        private Caller caller;
        private User user;
        private Resource resource;
    }

    @Data
    private class Caller {
        private String id;
        private String ip;
        private String agent;
        private String token;
    }

    @Data
    private class User {
        private String id;
        private String username;
        private String domain;
    }

    @Data
    private class Resource {
        private String uri;
        private String method;
        private int responseStatus;
    }
}
