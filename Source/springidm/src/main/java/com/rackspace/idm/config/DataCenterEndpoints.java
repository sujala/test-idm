package com.rackspace.idm.config;

import java.util.HashMap;
import java.util.Map;

public class DataCenterEndpoints {
    private Map<String, DataCenterClient> endPoints = new HashMap<String, DataCenterClient>();

    public void put(DataCenterClient client) {
        endPoints.put(client.getDcPrefix(), client);
    }
    
    public DataCenterClient get(String dc) {
        return endPoints.get(dc);
    }
}
