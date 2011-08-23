package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DelegateCloud11Service implements Cloud11Service {

    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private Configuration config;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Override
    public Response.ResponseBuilder validateToken(String tokenId, String belongsTo, String type,
                                                  HttpHeaders httpHeaders) throws IOException {

        try {
            return defaultCloud11Service.validateToken(tokenId, belongsTo, type, httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("belongsTo", belongsTo);
        queryParams.put("type", type);
        String path = getCloudAuthV11Url().concat(getPath("token/"+tokenId, queryParams));
        return cloudClient.get(path, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException {
        Response.ResponseBuilder serviceResponse = defaultCloud11Service.authenticate(response, httpHeaders, body);
        if (serviceResponse.build().getStatus() == 404) {
            return cloudClient.post(getCloudAuthV11Url().concat("auth"), httpHeaders, body);
        }else{
            return serviceResponse;
        }
    }

    @Override
    public Response.ResponseBuilder revokeToken(String tokenId, HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.revokeToken(tokenId, httpHeaders);
        } catch (Exception e) {
        }

        return cloudClient.delete(getCloudAuthV11Url().concat("token/"+tokenId),httpHeaders);
    }

    @Override
    public <T> Response.ResponseBuilder userRedirect(T nastId, HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.userRedirect(nastId, httpHeaders);
        } catch (Exception e) {
        }
        return cloudClient.get(getCloudAuthV11Url().concat("nast/"+nastId),httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(String serviceName, HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.getBaseURLs(serviceName, httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("serviceName", serviceName);
        String path = getCloudAuthV11Url().concat(getPath("baseURLs",queryParams));
        return cloudClient.get(path,httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLId(int baseURLId, String serviceName, HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.getBaseURLId(baseURLId, serviceName, httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("serviceName", serviceName);
        String path = getCloudAuthV11Url().concat(getPath("baseURLs/"+baseURLId, queryParams));
        return cloudClient.get(path,httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(String serviceName, HttpHeaders httpHeaders) throws IOException {
        try {
            return defaultCloud11Service.getEnabledBaseURL(serviceName, httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("serviceName", serviceName);
        String path = getCloudAuthV11Url().concat(getPath("baseURLs/enabled",queryParams));
        return cloudClient.get(path,httpHeaders);
    }

    @Override
    public Response.ResponseBuilder migrate(String user, HttpHeaders httpHeaders, String body) throws IOException {
        try {
            return defaultCloud11Service.migrate(user, httpHeaders, body);
        } catch (Exception e) {
        }
        String path =  "migration/"+user+"/migrate";
        return cloudClient.post(getCloudAuthV11Url().concat(path),httpHeaders,body);
    }

    @Override
    public Response.ResponseBuilder unmigrate(String user, HttpHeaders httpHeaders, String body) throws IOException {
        try {
            return defaultCloud11Service.unmigrate(user, httpHeaders, body);
        } catch (Exception e) {
        }
        String path =  "migration/"+user+"/unmigrate";
        return cloudClient.post(getCloudAuthV11Url().concat(path),httpHeaders,body);
    }

    @Override
    public Response.ResponseBuilder all(HttpHeaders httpHeaders, String body) throws IOException {
        try {
            return defaultCloud11Service.all(httpHeaders, body);
        } catch (Exception e) {
        }
        String path =  "migration/all";
        return cloudClient.post(getCloudAuthV11Url().concat(path),httpHeaders,body);
    }

    private String getPath(String path, HashMap<String, String> queryParams) {
        String result = path;
        String queryString = "";

        if (queryParams != null) {
            for (String key : queryParams.keySet()) {
                if (queryParams.get(key) != null) {
                    queryString += key + "=" + queryParams.get(key) + "&";
                }
            }

            if (queryString.length() > 0) {
                result += "?" + queryString.substring(0, queryString.length() - 1);
            }
        }

        return result;
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
