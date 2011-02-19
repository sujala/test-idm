package com.rackspace.idm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * @author john.eo
 * Not thread safe
 */
public class DataCenterEndpoints {
    private Configuration config;
    private String[] dcConfigs;
    private Client jclient = new Client(); // According to the docs, this object
                                           // is expensive to create.
    private Map<String, DataCenterClient> endpoints = new HashMap<String, DataCenterClient>();

    @Autowired
    public DataCenterEndpoints(Configuration config) {
        this.config = config;
        dcConfigs = config.getStringArray("dc");
        endpoints = build(dcConfigs, jclient);
    }

    public DataCenterClient get(String dc) {
        return getEndPoints().get(dc);
    }

    public List<DataCenterClient> getAll() {
        Collection<DataCenterClient> all = getEndPoints().values();
        return Collections.unmodifiableList(new ArrayList<DataCenterClient>(all));
    }

// TODO get rid of this!
//    public List<String> getAllTokenPermuations(String tokenWithPrefix) {
//        // Build a list of all possible prefix/token combination
//        String tokenWithoutPrefix = StringUtils.split(tokenWithPrefix, "-")[1];
//        List<String> tokenPermutations = new ArrayList<String>();
//        for (DataCenterClient client : getEndPoints().values()) {
//            tokenPermutations.add(String.format("%s-%s", client.getDcPrefix(), tokenWithoutPrefix));
//        }
//
//        return tokenPermutations;
//    }

    public static String getTokenPrefix(String tokenWithPrefix) {
        return StringUtils.split(tokenWithPrefix, "-")[0];
    }

    /**
     * Detects changes to the DC config.
     * 
     * @return
     */
    private Map<String, DataCenterClient> getEndPoints() {
        String[] newDcConfigs = config.getStringArray("dc");
        if (!Arrays.equals(dcConfigs, newDcConfigs)) {
            endpoints = build(newDcConfigs, jclient);
        }

        return endpoints;
    }

    private static Map<String, DataCenterClient> build(String[] dcConfigs, Client jclient) {
        Map<String, DataCenterClient> endpoints = new HashMap<String, DataCenterClient>();
        for (String dcConfig : dcConfigs) {
            String[] dcData = dcConfig.split("\\|");
            WebResource resource = jclient.resource(dcData[1]);
            DataCenterClient client = new DataCenterClient(dcData[0], resource);
            endpoints.put(client.getDcPrefix(), client);
        }

        return Collections.unmodifiableMap(endpoints);
    }
}
