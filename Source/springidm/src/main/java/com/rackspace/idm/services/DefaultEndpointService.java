package com.rackspace.idm.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.exceptions.BaseUrlConflictException;

public class DefaultEndpointService implements EndpointService {

    private EndpointDao endpointDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultEndpointService(EndpointDao endpointDao) {
        this.endpointDao = endpointDao;
    }

    public void addBaseUrl(CloudBaseUrl baseUrl) {
        
        CloudBaseUrl exists = this.endpointDao.getBaseUrlById(baseUrl.getBaseUrlId());
        
        if (exists != null) {
            String errMsg = String.format("An Endpoint with Id=%s already exists", baseUrl.getBaseUrlId());
            logger.warn(errMsg);
            throw new BaseUrlConflictException(errMsg);
        }
        
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
        if (endpoints != null && endpoints.size() > 0) {
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
