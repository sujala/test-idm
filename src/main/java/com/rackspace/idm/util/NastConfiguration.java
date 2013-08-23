package com.rackspace.idm.util;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 6:01 PM
 */
@Component
public class NastConfiguration {

    @Autowired
    private Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isNastXmlRpcEnabled(){
        return configuration.getBoolean("nast.xmlrpc.enabled");
    }

    public String getNastResellerName() {
        return configuration.getString("nast.xmlrpc.reseller");
    }

    public List<URL> getNastXmlRpcUrl() throws MalformedURLException {
        List<URL> result = new ArrayList<URL>();

        String[] urlList = configuration.getStringArray("nast.xmlrpc.url");

        if (urlList != null) {
	        for (String rawUrl : urlList) {
	            result.add(new URL(rawUrl));
	        }
        }

        return result;
    }
}
