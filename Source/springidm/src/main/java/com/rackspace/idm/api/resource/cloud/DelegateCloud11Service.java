package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import java.util.HashMap;

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

    private String getPath(String path, HashMap<String, String> queryParams) {
        String result = path;
        String queryString = "";

        if(queryParams != null ) {
            for(String key : queryParams.keySet()) {
                if(queryParams.get(key) != null) {
                    queryString += key + "=" + queryParams.get(key) + "&";
                }
            }

            if(queryString.length() > 0) {
                result += "?" + queryString.substring(0, queryString.length() - 1);
            }
        }

        return result;
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }
}
