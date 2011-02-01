package com.rackspace.idm.config;

import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.api.client.WebResource;

public class DataCenterEndpoints {
    private Map<String, WebResource> endPoints = new HashMap<String, WebResource>();

    void put(String dc, WebResource wr) {
        endPoints.put(dc, wr);
    }
    
    public WebResource get(String dc) {
        return endPoints.get(dc);
    }
}
