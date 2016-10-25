package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.OpenstackType;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.rackspace.idm.GlobalConstants.TENANT_ALIAS_PATTERN;

@Component
public class DefaultEndpointService implements EndpointService {

    public static final String FEATURE_BASEURL_TO_REGION_MAPPING_STRATEGY = "feature.baseurl.to.cloud.region.mapping.strategy";
    public static final String UK_CLOUD_LON_REGION = "LON";
    public static final int UK_CLOUD_BASEURL_ID_THRESHOLD = 1000;
    public static final String CLOUD_REGION_UK = "UK";
    public static final String CLOUD_REGION_US = "US";
    public static final String CLOUD_REGION_PROP_NAME = "cloud.region";

    @Autowired
    private EndpointDao endpointDao;

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

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

        if (baseUrl.getRegion() == null) {
            baseUrl.setRegion(identityConfig.getReloadableConfig().getEndpointDefaultRegionId());
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
    public Iterable<CloudBaseUrl> getBaseUrls() {
        logger.debug("Getting baseurls");
        return this.endpointDao.getBaseUrls();
    }

    @Override
    public List<CloudBaseUrl> getGlobalBaseUrls() {
        logger.debug("Getting global baseurls");
        List<CloudBaseUrl> globalBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseURL : endpointDao.getBaseUrls()) {
            if (baseURL.getGlobal()) {
                globalBaseUrls.add(baseURL);
            }
        }
        return globalBaseUrls;
    }

    @Override
    public List<CloudBaseUrl> getDefaultBaseUrls() {
        logger.debug("Getting default baseurls");
        List<CloudBaseUrl> defaultBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseURL : endpointDao.getBaseUrls()) {
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
        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl baseUrl : this.endpointDao.getBaseUrls()) {
            if (baseUrl.getOpenstackType() != null && baseUrl.getOpenstackType().equals(serviceType)) {
                filteredBaseUrls.add(baseUrl);
            }
        }
        logger.debug("Got {} baseurls", filteredBaseUrls.size());
        return filteredBaseUrls;
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrlsByServiceName(String serviceName) {
        logger.debug("Getting baseUrls by service name");
        return endpointDao.getBaseUrlsByService(serviceName);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByBaseUrlType(String baseUrlType) {
        logger.debug("Getting baseurls by baseUrlType");
        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        if(identityConfig.getBaseUlrRespectEnabledFlag()) {
            for (CloudBaseUrl baseUrl : endpointDao.getDefaultBaseUrlsByBaseUrlTypeAndEnabled(baseUrlType, true)) {
                filteredBaseUrls.add(baseUrl);
            }
        } else {
            for (CloudBaseUrl baseUrl : endpointDao.getBaseUrls()) {
                if (baseUrl.getBaseUrlType() != null) {
                    if (baseUrl.getBaseUrlType().equals(baseUrlType)) {
                        filteredBaseUrls.add(baseUrl);
                    }
                }
            }
        }
        logger.debug("Got {} baseurls", filteredBaseUrls.size());
        return filteredBaseUrls;
    }

    @Override
    public List<OpenstackEndpoint> getEndpointsFromTenantList(List<Tenant> tenantList) {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        for (Tenant tenant : tenantList) {
            OpenstackEndpoint endpoint = this.getOpenStackEndpointForTenant(tenant);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    @Override
    public OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant, Set<OpenstackType> baseUrlTypes, String region, List<Rule> rules) {
        HashMap<String, CloudBaseUrl> baseUrls = new HashMap<String, CloudBaseUrl>();

        HashSet<String> tenantBaseUrlIds = new HashSet<String>();

        if (tenant.getBaseUrlIds() != null) {
            tenantBaseUrlIds.addAll(tenant.getBaseUrlIds());
        }

        if (tenant.getV1Defaults() != null) {
            tenantBaseUrlIds.addAll(tenant.getV1Defaults());
        }

        if (rules != null) {
            for (Rule rule : rules) {
                if (rule instanceof TenantTypeRule) {
                    TenantTypeRule tenantTypeRule = (TenantTypeRule) rule;

                    if (tenant.getTypes().contains(tenantTypeRule.getTenantType())) {
                        tenantBaseUrlIds.addAll(tenantTypeRule.getEndpointTemplateIds());
                    }
                }
            }
        }

        for (CloudBaseUrl baseUrl : endpointDao.getBaseUrlsById(new ArrayList<>(tenantBaseUrlIds))) {
            processBaseUrl(baseUrl, tenant);
            baseUrls.put(baseUrl.getBaseUrlId(), baseUrl);
        }

        if (baseUrlTypes != null && region != null) {
            for (OpenstackType baseUrlType : baseUrlTypes) {
                addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region);
            }
        }

        OpenstackEndpoint point = new OpenstackEndpoint();
        point.setTenantId(tenant.getTenantId());
        point.setTenantName(tenant.getName());
        point.setBaseUrls(new ArrayList<CloudBaseUrl>(baseUrls.values()));

        return point;
    }

    @Override
    public OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant) {
        return this.getOpenStackEndpointForTenant(tenant, null, null, null);
    }

    private void addGlobalBaseUrls(HashMap<String, CloudBaseUrl> baseUrls, Tenant tenant, OpenstackType baseUrlType, String region) {
        Iterable<CloudBaseUrl> cloudBaseUrls;
        if (region.equalsIgnoreCase("LON")) {
            cloudBaseUrls = endpointDao.getGlobalUKBaseUrlsByBaseUrlType(baseUrlType.getName());
        } else {
            cloudBaseUrls = endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType.getName());
        }
        if (cloudBaseUrls != null) {
            for (CloudBaseUrl baseUrl : cloudBaseUrls) {
                if (!baseUrls.containsKey(baseUrl.getBaseUrlId())) {
                    processBaseUrl(baseUrl, tenant);
                    baseUrl.setV1Default(false);
                    baseUrls.put(baseUrl.getBaseUrlId(), baseUrl);
                }
            }
        }
    }

    private void processBaseUrl(CloudBaseUrl baseUrl, Tenant tenant) {
        baseUrl.setV1Default(tenant.getV1Defaults().contains(baseUrl.getBaseUrlId()));
        baseUrl.setPublicUrl(appendTenantToBaseUrl(baseUrl.getPublicUrl(), tenant.getName(), baseUrl.getTenantAlias()));
        baseUrl.setAdminUrl(appendTenantToBaseUrl(baseUrl.getAdminUrl(), tenant.getName(), baseUrl.getTenantAlias()));
        baseUrl.setInternalUrl(appendTenantToBaseUrl(baseUrl.getInternalUrl(), tenant.getName(), baseUrl.getTenantAlias()));
    }

    String appendTenantToBaseUrl(String url, String tenantId, String tenantAlias) {
        if (url == null) {
            return null;
        }

        String stringToAppend = getTenantAlias(tenantAlias, tenantId);

        if (StringUtils.isEmpty(stringToAppend)) {
            return url;
        } else if (url.endsWith("/")) {
            return url + stringToAppend;
        } else {
            return url + "/" + stringToAppend;
        }
    }

    private String getTenantAlias(String tenantAlias, String tenantId) {
        if (tenantAlias != null) {
            return tenantAlias.replace(TENANT_ALIAS_PATTERN, tenantId);
        }
        return tenantId;
    }

    @Override
	public void setEndpointDao(EndpointDao endpointDao) {
		this.endpointDao = endpointDao;
	}

    private BaseUrlToRegionMappingStrategy getBaseUrlToRegionMappingStrategy() {
        String propValue = config.getString(FEATURE_BASEURL_TO_REGION_MAPPING_STRATEGY);
        BaseUrlToRegionMappingStrategy result = BaseUrlToRegionMappingStrategy.fromCode(propValue);

        //if not provided (or provided value was not valid), default to rsregion (2.1.2 strategy)
        if (result == null) {
            result = BaseUrlToRegionMappingStrategy.RSREGION;
        }

        return result;
    }

    /**
     * The configured cloud region is determined by the property "cloud.region". When "UK" then the cloud region is UK, otherwise it's US.
     *
     * The permissible strategies are determined by BaseUrlToRegionMappingStrategy, defaulting to the BaseUrlToRegionMappingStrategy.RSREGION
     * strategy.
     *
     * @param baseUrl
     * @return
     */
    @Override
    public boolean doesBaseUrlBelongToCloudRegion(CloudBaseUrl baseUrl) {
        BaseUrlToRegionMappingStrategy strategy = getBaseUrlToRegionMappingStrategy();
        boolean ukCloudRegion = isUkCloudRegion();
        if (baseUrl.getBaseUrlId() == null){
            return false;
        }

       if (BaseUrlToRegionMappingStrategy.RSREGION == strategy) {
            //this is the newly added version that requires UK urls to have a region
            if(ukCloudRegion && UK_CLOUD_LON_REGION.equals(baseUrl.getRegion())){
                return true;
            }
            if(!ukCloudRegion && !UK_CLOUD_LON_REGION.equals(baseUrl.getRegion())){
                return true;
            }
        }
        else if (BaseUrlToRegionMappingStrategy.HYBRID == strategy) {
            /*
            this is a hybrid of legacy and rsRegion. BaseUrls without a specified region must have an id <1000 for US, >=1000 for UK.
            if a baseurl specifies the region, it's UK if the region is "LON" (case sensitive) regardless of the id, and US
            otherwise
             */
            String baseUrlRegion = baseUrl.getRegion();
            int baseUrlId = Integer.parseInt(baseUrl.getBaseUrlId());
            if(ukCloudRegion
                    && (baseUrlId >= UK_CLOUD_BASEURL_ID_THRESHOLD && (isDefaultRegion(baseUrlRegion) || UK_CLOUD_LON_REGION.equals(baseUrlRegion))
                        ||
                       (baseUrlId < UK_CLOUD_BASEURL_ID_THRESHOLD && !isDefaultRegion(baseUrlRegion) && UK_CLOUD_LON_REGION.equals(baseUrlRegion)))
               ) {
                //only uk region if id is (>= 1000 AND the region is either blank or LON) OR <1000 and region is LON
                return true;
            }
            if(!ukCloudRegion
                    && (baseUrlId < UK_CLOUD_BASEURL_ID_THRESHOLD && (isDefaultRegion(baseUrlRegion) || !UK_CLOUD_LON_REGION.equals(baseUrlRegion))
                    ||
                    (baseUrlId >= UK_CLOUD_BASEURL_ID_THRESHOLD && !isDefaultRegion(baseUrlRegion) && !UK_CLOUD_LON_REGION.equals(baseUrlRegion)))
                    ) {
                //only us region if id is (< 1000 AND the region is either blank or something other than LON) OR >=1000 and region is something other than LON
                return true;
            }
        }
        return false;
    }

    private boolean isDefaultRegion(String baseUrlRegion) {
        return org.apache.commons.lang.StringUtils.isBlank(baseUrlRegion) || baseUrlRegion.equals(identityConfig.getReloadableConfig().getEndpointDefaultRegionId());
    }

    private boolean isUkCloudRegion() {
        return CLOUD_REGION_UK.equalsIgnoreCase(config.getString(CLOUD_REGION_PROP_NAME));
    }

    public enum BaseUrlToRegionMappingStrategy {
        HYBRID("hybrid"),
        RSREGION("rsregion");

        private String code;

        private BaseUrlToRegionMappingStrategy(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static BaseUrlToRegionMappingStrategy fromCode(String code) {
            if (!org.apache.commons.lang.StringUtils.isBlank(code)) {
                for (BaseUrlToRegionMappingStrategy strategy : values()) {
                    if (code.equalsIgnoreCase(strategy.getCode())) {
                        return strategy;
                    }
                }
            }
            return null;
        }
    }
    
    
}
