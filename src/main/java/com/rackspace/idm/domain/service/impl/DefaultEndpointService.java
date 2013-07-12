package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.service.CapabilityService;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class DefaultEndpointService implements EndpointService {

    @Autowired
    private EndpointDao endpointDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
    public void deleteBaseUrl(String baseUrlId) {
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
    public CloudBaseUrl getBaseUrlById(String baseUrlId) {
        logger.debug("Getting baserul {}", baseUrlId);
        return this.endpointDao.getBaseUrlById(String.valueOf(baseUrlId));
    }

    @Override
    public CloudBaseUrl checkAndGetEndpointTemplate(String baseUrlId) {
        CloudBaseUrl baseUrl = getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EndpointTemplate %s not found", baseUrlId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return baseUrl;
    }

    @Override
    public void setBaseUrlEnabled(String baseUrlId, boolean enabled) {
        logger.info("Setting baseurl {} enabled {}", baseUrlId, enabled);
        CloudBaseUrl baseUrl = endpointDao.getBaseUrlById(String.valueOf(baseUrlId));
        baseUrl.setEnabled(enabled);
        endpointDao.updateCloudBaseUrl(baseUrl);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByServiceType(String serviceType) {
        logger.debug("Getting baseurls by serviceId");
        List<CloudBaseUrl> allBaseUrls = this.endpointDao.getBaseUrls();
        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseUrl : allBaseUrls) {
            if (baseUrl.getOpenstackType() != null && baseUrl.getOpenstackType().equals(serviceType)) {
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
            if (baseUrl.getBaseUrlType() != null) {
                if (baseUrl.getBaseUrlType().equals(baseUrlType)) {
                    filteredBaseUrls.add(baseUrl);
                }
            }
        }
        logger.debug("Got {} baseurls", filteredBaseUrls.size());
        return filteredBaseUrls;
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId) {
        return endpointDao.getBaseUrlsWithPolicyId(policyId);
    }

    @Override
    public List<OpenstackEndpoint> getEndpointsFromTenantList(List<Tenant> tenantList) {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        for (Tenant tenant : tenantList) {
            OpenstackEndpoint endpoint = this.getOpenstackEndpointsForTenant(tenant);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    private OpenstackEndpoint getOpenstackEndpointsForTenant(Tenant tenant) {
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();

        HashSet<String> tenantBaseUrlIds = new HashSet<String>();

        if (tenant.getBaseUrlIds() != null) {
            tenantBaseUrlIds.addAll(tenant.getBaseUrlIds());
        }

        if (tenant.getV1Defaults() != null) {
            tenantBaseUrlIds.addAll(tenant.getV1Defaults());
        }

        for (String baseUrlId : tenantBaseUrlIds) {
            CloudBaseUrl baseUrl = endpointDao.getBaseUrlById(baseUrlId);
            if (baseUrl != null) {
                baseUrl.setV1Default(tenant.getV1Defaults().contains(baseUrlId));
                baseUrl.setPublicUrl(appendTenantToBaseUrl(baseUrl.getPublicUrl(), tenant.getName()));
                baseUrl.setAdminUrl(appendTenantToBaseUrl(baseUrl.getAdminUrl(), tenant.getName()));
                baseUrl.setInternalUrl(appendTenantToBaseUrl(baseUrl.getInternalUrl(), tenant.getName()));
                baseUrls.add(baseUrl);
            }
        }

        OpenstackEndpoint point = new OpenstackEndpoint();
        point.setTenantId(tenant.getTenantId());
        point.setTenantName(tenant.getName());
        point.setBaseUrls(baseUrls);

        return point;
    }

    String appendTenantToBaseUrl(String url, String tenantId) {
        if (url == null) {
            return null;
        }
        else if (url.endsWith("/")) {
            return url + tenantId;
        } else {
            return url + "/" + tenantId;
        }
    }

    @Override
    public OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant) {
        return this.getOpenstackEndpointsForTenant(tenant);
    }

    @Override
	public void addPolicyToEndpoint(String baseUrlId, String policyId) {
        CloudBaseUrl baseUrl = endpointDao.getBaseUrlById(baseUrlId);
        baseUrl.getPolicyList().add(policyId);
        endpointDao.updateCloudBaseUrl(baseUrl);
	}

	@Override
	public void deletePolicyToEndpoint(String baseUrlId, String policyId) {
        CloudBaseUrl baseUrl = endpointDao.getBaseUrlById(baseUrlId);
        baseUrl.getPolicyList().remove(policyId);
        endpointDao.updateCloudBaseUrl(baseUrl);
    }

    @Override
	public void setEndpointDao(EndpointDao endpointDao) {
		this.endpointDao = endpointDao;
	}
}
