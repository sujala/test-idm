package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DelegateCloud20Service implements Cloud20Service{
    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private Configuration config;

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, String body) throws IOException {
        return cloudClient.post(getCloudAuthV20Url()+"tokens", httpHeaders, body);
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }
    public void setConfig(Configuration config) {
        this.config = config;
    }

    private String getCloudAuthV20Url() {
        String cloudAuth20url = config.getString("cloudAuth20url");
        return cloudAuth20url;
    }
}
