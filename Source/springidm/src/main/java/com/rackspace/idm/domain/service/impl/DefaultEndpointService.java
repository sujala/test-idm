package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultEndpointService implements EndpointService {

    private final EndpointDao endpointDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultEndpointService(EndpointDao endpointDao) {
        this.endpointDao = endpointDao;
    }

    @Override
    public void addBaseUrl(CloudBaseUrl baseUrl) {
        logger.debug("Adding base url");

        CloudBaseUrl exists = this.endpointDao.getBaseUrlById(baseUrl.getBaseUrlId());

        if (exists != null) {
            String errMsg = String.format("An Endpoint with Id=%s already exists", baseUrl.getBaseUrlId());
            logger.warn(errMsg);
            throw new BaseUrlConflictException(errMsg);
        }

        this.endpointDao.addBaseUrl(baseUrl);
        logger.debug("Done adding base url.");
    }

    @Override
    public void addBaseUrlToUser(int baseUrlId, boolean def, String username) {
        logger.debug("Adding baseurl {} to user {}", baseUrlId, username);
        this.endpointDao.addBaseUrlToUser(baseUrlId, def, username);
        logger.debug("Done adding baseurl {} to user {}", baseUrlId, username);
    }

    @Override
    public void deleteBaseUrl(int baseUrlId) {
        logger.debug("Deleting base url {}", baseUrlId);
        this.endpointDao.deleteBaseUrl(baseUrlId);
        logger.debug("Done deleting base url {}", baseUrlId);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrls() {
        logger.debug("Getting baseurls");
        return this.endpointDao.getBaseUrls();
    }

    @Override
    public List<CloudBaseUrl> getGlobalBaseUrls() {
        logger.debug("Getting global baseurls");
        List<CloudBaseUrl> baseUrls = endpointDao.getBaseUrls();
        List<CloudBaseUrl> globalBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseURL : baseUrls) {
            if (baseURL.getGlobal()) {
                globalBaseUrls.add(baseURL);
            }
        }
        return globalBaseUrls;
    }

    @Override
    public List<CloudBaseUrl> getDefaultBaseUrls() {
        logger.debug("Getting default baseurls");
        List<CloudBaseUrl> baseUrls = endpointDao.getBaseUrls();
        List<CloudBaseUrl> defaultBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseURL : baseUrls) {
            if (baseURL.getDef()) {
                defaultBaseUrls.add(baseURL);
            }
        }
        return defaultBaseUrls;
    }

    @Override
    public CloudBaseUrl getBaseUrlById(int baseUrlId) {
        logger.debug("Getting baserul {}", baseUrlId);
        return this.endpointDao.getBaseUrlById(baseUrlId);
    }

    @Override
    public CloudEndpoint getEndpointForUser(String username, int baseUrlId) {
        logger.debug("Getting endpoint {} for user {}", baseUrlId, username);
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
        logger.debug("Got endpoint {} for user {}", baseUrlId, username);
        return endpoint;
    }

    @Override
    public List<CloudEndpoint> getEndpointsForUser(String username) {
        logger.debug("Getting endpoints for user {}", username);
        return this.endpointDao.getEndpointsForUser(username);
    }

    @Override
    public void removeBaseUrlFromUser(int baseUrlId, String username) {
        logger.debug("Removing baseurl {} from user {}", baseUrlId, username);
        CloudBaseUrl baseUrlById = endpointDao.getBaseUrlById(baseUrlId);
        if(baseUrlById==null){
            throw new NotFoundException("Base url with id: "+baseUrlId+" not found");
        }
        String service = baseUrlById.getService();
        List<CloudBaseUrl> baseUrlsByService = endpointDao.getBaseUrlsByService(service);
        if(baseUrlById.getDef()!=null && baseUrlById.getDef()==true || baseUrlsByService.size() < 2){
            throw new BadRequestException("Cannot delete the only endpoint for the service '"+service+"'.");
        }
        endpointDao.removeBaseUrlFromUser(baseUrlId, username);
    }

    @Override
    public void setBaseUrlEnabled(int baseUrlId, boolean enabled) {
        logger.info("Setting baseurl {} enabled {}", baseUrlId, enabled);
        this.endpointDao.setBaseUrlEnabled(baseUrlId, enabled);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByServiceId(String serviceType) {
        logger.debug("Getting baseurls by serviceId");
        List<CloudBaseUrl> allBaseUrls = this.endpointDao.getBaseUrls();
        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseUrl : allBaseUrls) {
            if (baseUrl.getOpenstackType().equals(serviceType)) {
                filteredBaseUrls.add(baseUrl);
            }
        }
        logger.debug("Got {} baseurls", filteredBaseUrls.size());
        return filteredBaseUrls;
    }
}
