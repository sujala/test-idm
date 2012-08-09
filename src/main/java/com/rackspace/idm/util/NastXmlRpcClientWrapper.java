package com.rackspace.idm.util;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
public class NastXmlRpcClientWrapper {

    @Autowired
    private NastConfiguration authConfiguration;

    XmlRpcClient getClient(URL url) throws MalformedURLException {
        XmlRpcClientConfigImpl config;
        XmlRpcClient client;

        config = new XmlRpcClientConfigImpl();
        config.setServerURL(url);
        client = new XmlRpcClient();
        client.setConfig(config);

        return client;
    }


    public String addResellerStorageAccount(String[] parameters) throws MalformedURLException, XmlRpcException {
        String response = null;

        for (URL url : authConfiguration.getNastXmlRpcUrl()) {
            if (response == null) {
                response = (String) getClient(url).execute("reseller.add_storage_account", parameters);
            } else {
                String nthResponse = (String) getClient(url).execute("reseller.add_storage_account", new String[]{parameters[0], removeNastPrefix(response)});
            }
        }

        return response;
    }

    public Boolean removeResellerStorageAccount(String nastAccountId) throws MalformedURLException, XmlRpcException {
        boolean response = true;

        for (URL url : authConfiguration.getNastXmlRpcUrl()) {
            response = response && removeResellerStorageAccount(nastAccountId, getClient(url));
        }

        return response;
    }

    public Boolean removeResellerStorageAccount(String nastAccountId, XmlRpcClient rpcClient) throws MalformedURLException, XmlRpcException {
        Boolean response;
        String nastResellerName = authConfiguration.getNastResellerName();
        String cleanedNastId = removeNastPrefix(nastAccountId);
        Object[] params = new Object[]{
                nastResellerName,
                cleanedNastId
        };

        response = (Boolean) rpcClient.execute("reseller.remove_storage_account", params);

        return response;
    }

    String removeNastPrefix(String nastAccountId) {
        if (!StringUtils.isBlank(nastAccountId) && nastAccountId.startsWith(authConfiguration.getNastResellerName())) {
            return nastAccountId.replaceFirst(authConfiguration.getNastResellerName() + "_", "");
        }

        return nastAccountId;
    }

    public void setAuthConfiguration(NastConfiguration authConfiguration) {
        this.authConfiguration = authConfiguration;
    }
}
