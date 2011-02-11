package com.rackspace.idm.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class DataCenterEndpoints {
    private Map<String, DataCenterClient> endPoints = new HashMap<String, DataCenterClient>();

    public void put(DataCenterClient client) {
        endPoints.put(client.getDcPrefix(), client);
    }

    public DataCenterClient get(String dc) {
        return endPoints.get(dc);
    }

    public List<DataCenterClient> getAll() {
        Collection<DataCenterClient> all = endPoints.values();
        return Collections.unmodifiableList(new ArrayList<DataCenterClient>(all));
    }

    public String getTokenPrefix(String tokenWithPrefix) {
        return StringUtils.split(tokenWithPrefix, "-")[0];
    }
}
