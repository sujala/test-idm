package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BaseUrlConflictException;
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
    public void updateBaseUrl(CloudBaseUrl baseUrl) {
        logger.debug("Updating base url");
        endpointDao.updateCloudBaseUrl(baseUrl);
        logger.debug("Done updating base url.");
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
            if (baseURL.getDef()!=null && baseURL.getDef()) {
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
    public void setBaseUrlEnabled(int baseUrlId, boolean enabled) {
        logger.info("Setting baseurl {} enabled {}", baseUrlId, enabled);
        this.endpointDao.setBaseUrlEnabled(baseUrlId, enabled);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByServiceType(String serviceType) {
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

    @Override
    public List<CloudBaseUrl> getBaseUrlsByServiceName(String serviceName) {
        logger.debug("Getting baseUrls by service name");
        return endpointDao.getBaseUrlsByService(serviceName);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByBaseUrlType(String baseUrlType) {
        logger.debug("Getting baseurls by baseUrlType");
        List<CloudBaseUrl> allBaseUrls = endpointDao.getBaseUrls();
        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        if(allBaseUrls==null){
            return filteredBaseUrls;
        }
        for (CloudBaseUrl baseUrl : allBaseUrls) {
            if (baseUrl.getBaseUrlType().equals(baseUrlType)) {
                filteredBaseUrls.add(baseUrl);
            }
        }
        logger.debug("Got {} baseurls", filteredBaseUrls.size());
        return filteredBaseUrls;
    }
}
