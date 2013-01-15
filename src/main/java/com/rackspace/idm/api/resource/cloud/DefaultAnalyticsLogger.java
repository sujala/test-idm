package com.rackspace.idm.api.resource.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.service.UserService;
import com.sun.jersey.core.util.Base64;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
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

    public void log(Long startTime, String authToken, String basicAuth, String host, String remoteHost, String userAgent, String method, String path, int status, String requestBody, String requestType, String responseBody, String responseType) {
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
            String username = getUsernameFromRequestBody(requestBody, requestType);
            if (username != null) {
                userEntity = userService.getUser(username);
            }
        }

        User user = new User();

        if (userEntity != null) {
            user.setId(userEntity.getId());
            user.setUsername(userEntity.getUsername());
            user.setDomain(userEntity.getDomainId());
        }

        Resource resource = new Resource();
        resource.setUri(getUri(host, getPathWithoutToken(path)));
        resource.setMethod(method);
        resource.setResponseStatus(status);
        message.setResource(resource);

        if (isAuthenticateCall(path, method) || isValidateCall(path, method)) {
            TokenParam tokenParam = getTokenFromResponseBody(responseBody, responseType);
            user.setToken(hashToken(tokenParam.getToken()));
            user.setTokenExp(tokenParam.getTokenExp());
        }

        message.setUser(user);

        Gson gson = new GsonBuilder().create();
        String messageString = gson.toJson(message);

        analyticsLogHandler.log(messageString);
    }

    private boolean isAuthenticateCall(String path, String method) {
        return StringUtils.endsWithIgnoreCase(path, "tokens") && method.equalsIgnoreCase("POST");
    }

    private boolean isValidateCall(String path, String method) {
        String userToken = parseUserTokenFromPath(path);
        return (StringUtils.endsWithIgnoreCase(path, "tokens/" + userToken) && method.equalsIgnoreCase("GET"));
    }

    private TokenParam getTokenFromResponseBody(String responseBody, String responseType) {
        TokenParam tokenParam = new TokenParam();
        if (responseBody != null && responseType != null) {
            if (responseType.equalsIgnoreCase("application/json")) {
                getTokenFromJsonBody(responseBody, tokenParam);
            } else if (responseType.equalsIgnoreCase("application/xml")) {
                getTokenFromXmlBody(responseBody, tokenParam);
            }
        }
        return tokenParam;
    }

    private void getTokenFromJsonBody(String responseBody, TokenParam tokenParam) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
            if (jsonObject.containsKey(JSONConstants.ACCESS)) {
                JSONObject access = (JSONObject) jsonObject.get(JSONConstants.ACCESS);
                if (access.containsKey(JSONConstants.TOKEN)) {
                    JSONObject token = (JSONObject) access.get(JSONConstants.TOKEN);
                    if (token.containsKey(JSONConstants.ID)) {
                        tokenParam.setToken((String)token.get(JSONConstants.ID));
                    }
                    if (token.containsKey(JSONConstants.EXPIRES)){
                        String dateString = (String)token.get(JSONConstants.EXPIRES);
                        tokenParam.setTokenExp(new DateTime(dateString).toDate().getTime());
                    }
                }
            }
        } catch (ParseException e) {
        }
    }

    private void getTokenFromXmlBody(String responseBody, TokenParam tokenParam) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(responseBody));
            Document document = builder.parse(is);

            NodeList nodeList = document.getElementsByTagName(JSONConstants.TOKEN);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                Node idNode = node.getAttributes().getNamedItem(JSONConstants.ID);
                if (idNode != null) {
                    tokenParam.setToken(idNode.getNodeValue());
                }
                Node expNode = node.getAttributes().getNamedItem(JSONConstants.EXPIRES);
                if (expNode != null) {
                    tokenParam.setTokenExp(new DateTime(expNode.getNodeValue()).toDate().getTime());
                }
            }

        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        } catch (IOException e) {
        }
    }

    private String hashToken(String authToken) {
        if (authToken != null) {
            try {
                InputStream is = new ByteArrayInputStream( authToken.getBytes());
                return DigestUtils.md5Hex(is);
            } catch (UnsupportedEncodingException e) {
            } catch (IOException e) {
            }
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
        private String token;
        private long tokenExp;
    }

    @Data
    private class Resource {
        private String uri;
        private String method;
        private int responseStatus;
    }

    @Data
    private class TokenParam {
        private String token;
        private long tokenExp;
    }
}
