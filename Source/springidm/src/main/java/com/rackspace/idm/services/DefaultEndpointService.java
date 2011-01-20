package com.rackspace.idm.services;

import java.util.List;

import org.slf4j.Logger;

import com.rackspace.idm.dao.EndpointDao;
import com.rackspace.idm.entities.CloudBaseUrl;
import com.rackspace.idm.entities.CloudEndpoint;

public class DefaultEndpointService implements EndpointService {

    private EndpointDao endpointDao;
    private Logger logger;

    public DefaultEndpointService(EndpointDao endpointDao, Logger logger) {
        this.endpointDao = endpointDao;
        this.logger = logger;
    }

    public void addBaseUrl(CloudBaseUrl baseUrl) {
        this.endpointDao.addBaseUrl(baseUrl);

    }

    public void addBaseUrlToUser(int baseUrlId, boolean def, String username) {
       this.endpointDao.addBaseUrlToUser(baseUrlId, def, username);
    }

    public void deleteBaseUrl(int baseUrlId) {
        this.endpointDao.deleteBaseUrl(baseUrlId);
    }

    public List<CloudBaseUrl> getBaseUrls() {
        return this.endpointDao.getBaseUrls();
    }
    
    public CloudBaseUrl getBaseUrlById(int baseUrlId) {
        return this.endpointDao.getBaseUrlById(baseUrlId);
    }
    
    public CloudEndpoint getEndpointForUser(String username, int baseUrlId) {
        List<CloudEndpoint> endpoints = this.getEndpointsForUser(username);
        CloudEndpoint endpoint = null;
        if (endpoints != null && endpoints.size() > 1) {
            for (CloudEndpoint e : endpoints) {
                if (e.getBaseUrl().getBaseUrlId() == baseUrlId) {
                    endpoint = e;
                    break;
                }
            }
        }
        return endpoint;
    }

    public List<CloudEndpoint> getEndpointsForUser(String username) {
        return this.endpointDao.getEndpointsForUser(username);
    }

    public void removeBaseUrlFromUser(int baseUrlId, String username) {
        this.endpointDao.removeBaseUrlFromUser(baseUrlId, username);
    }

}
