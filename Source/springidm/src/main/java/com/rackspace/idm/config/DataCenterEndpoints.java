package com.rackspace.idm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class DataCenterEndpoints {
    private String[] dcConfigs;
    private Map<String, DataCenterClient> endpoints = new HashMap<String, DataCenterClient>();

    public void put(DataCenterClient client) {
        endpoints.put(client.getDcPrefix(), client);
    }

    public DataCenterClient get(String dc) {
        return endpoints.get(dc);
    }

    public List<DataCenterClient> getAll() {
        Collection<DataCenterClient> all = endpoints.values();
        return Collections.unmodifiableList(new ArrayList<DataCenterClient>(all));
    }

    public String getTokenPrefix(String tokenWithPrefix) {
        return StringUtils.split(tokenWithPrefix, "-")[0];
    }

    public static DataCenterEndpoints refresh(DataCenterEndpoints oldEndpoints, String[] dcConfigs) {
        if (Arrays.equals(oldEndpoints.dcConfigs, dcConfigs)) {
            return oldEndpoints;
        }

        return build(dcConfigs);
    }

    public static DataCenterEndpoints build(String[] dcConfigs) {
        DataCenterEndpoints eps = new DataCenterEndpoints();
        Client jclient = Client.create();
        for (String dcConfig : dcConfigs) {
            String[] dcData = dcConfig.split("\\|");
            WebResource resource = jclient.resource(dcData[1]);
            DataCenterClient client = new DataCenterClient(dcData[0], resource);
            eps.put(client);
        }

        return eps;
    }
}
