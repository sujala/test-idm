package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

@Component
public class DelegateCloud11Service implements Cloud11Service {

    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private Configuration config;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Override
    public Response.ResponseBuilder validateToken(String belongsTo, String type,
                                                  HttpHeaders httpHeaders) throws IOException {

        try {
            return defaultCloud11Service.validateToken(belongsTo, type, httpHeaders);
        } catch (Exception e) {
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("belongsTo", belongsTo);
        queryParams.put("type", type);
        String path = getCloudAuthV11Url().concat(getPath("token", queryParams));
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
