package com.rackspace.idm.domain.entity;

import com.rackspace.cloud.esb.support.ESBClientUtil;
import com.rackspace.cloud.service.servers.CloudServers;
import com.rackspace.idm.domain.service.UserGroupService;
import com.rackspace.idm.exception.ApiException;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/13/11
 * Time: 10:34 AM
 */
@Component
public class ESBCloudServersFactory {

    @Autowired
    private Configuration config;

    private static final Logger LOGGER = Logger.getLogger(UserGroupService.class);

    public CloudServers getCSClient(String accountID) throws ApiException {
        try {
            String cloudServersESBURL = getCloudServersESBURL();
            if (cloudServersESBURL == null) {
                throw new IllegalArgumentException("Property cloud.servers.service.esb.url is not set.");
            }
            CloudServers client = ESBClientUtil.getJaxWsClient(CloudServers.class, cloudServersESBURL);
            ESBClientUtil.setCloudESBHeaders((BindingProvider) client, accountID, "apiuser", null);
            return client;
        } catch (Exception e) {
            LOGGER.error("Unable to create client to Cloud Servers ESB service.", e);
            throw new ApiException(500, "An error was encountered while trying to connect to Cloud Servers.", "");
        }
    }

    private String getCloudServersESBURL() {
        return config.getString("esb.cloud.us.servers.service");
    }
}
