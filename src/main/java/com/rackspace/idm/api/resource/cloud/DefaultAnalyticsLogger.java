package com.rackspace.idm.api.resource.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rackspace.idm.domain.service.UserService;
import com.sun.jersey.core.util.Base64;
import lombok.Data;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    public void log(Long startTime, String authToken, String basicAuth, String host, String userAgent, String method, String path, int status) {
        long duration = new Date().getTime() - startTime;
        String endpoint = config.getString("ga.endpoint");

        Message message = new Message();
        message.setTimestamp(String.valueOf(startTime));
        message.setDuration(String.valueOf(duration));

        Caller caller = new Caller();
        caller.setIp(getHost(host));
        if (authToken != null) {
            caller.setId(getUserIdFromAuthToken(authToken));
        } else if (basicAuth != null) {
            caller.setId(getUserIdFromBasicAuth(basicAuth));
        }
        caller.setAgent(userAgent);
        message.setCaller(caller);

        String userId = getUserIdFromPath(path);

        if (userId != null) {
            User user = new User();
            user.setId(userId);
            com.rackspace.idm.domain.entity.User userEntity = userService.getUserById(userId);
            if (userEntity != null) {
                user.setUsername(userEntity.getUsername());
                user.setDomain(userEntity.getDomainId());
                message.setUser(user);
            }
        }

        Resource resource = new Resource();
        resource.setUri(getUri(endpoint, getPathWithoutToken(path)));
        resource.setMethod(method);
        resource.setResponseStatus(status);
        message.setResource(resource);

        Gson gson = new GsonBuilder().create();
        String messageString = gson.toJson(message);

        analyticsLogHandler.log(messageString);
    }

    private String getPathWithoutToken(String path) {
        String token = parseUserTokenFromPath(path);
        if (token != null) {
            return path.replace(token, OBFUSCATED_TOKEN);
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

    private String getHost(String host) {
        if (host != null) {
            int index = host.indexOf(':');
            if (index >= 0) {
                host = host.substring(0, index);
            }
        }
        return host;
    }

    private String getUserIdFromAuthToken(String authToken) {
        com.rackspace.idm.domain.entity.User user = userService.getUserByAuthToken(authToken);
        if (user != null) {
            return user.getId();
        }
        return null;
    }

    private String getUri(String endpoint, String path) {
        try {
            return new URL(new URL(endpoint), path).toString();
        } catch (MalformedURLException e) {
        }
        return "";
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
