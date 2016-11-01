package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface EndpointService {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void deleteBaseUrl(String baseUrlId);

    CloudBaseUrl getBaseUrlById(String baseUrlId);

    CloudBaseUrl checkAndGetEndpointTemplate(String id);

    Iterable<CloudBaseUrl> getBaseUrls();

    List<CloudBaseUrl> getGlobalBaseUrls();
    
    List<CloudBaseUrl> getDefaultBaseUrls();

    void setBaseUrlEnabled(String baseUrlId, boolean enabled);

    List<CloudBaseUrl> getBaseUrlsByServiceType(String serviceType);

    Iterable<CloudBaseUrl> getBaseUrlsByServiceName(String serviceName);
    
    List<CloudBaseUrl> getBaseUrlsByBaseUrlType(String baseUrlType);

    List<OpenstackEndpoint> getEndpointsFromTenantList(List<Tenant> tenantList);

    OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant);

    OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant, Set<OpenstackType> openStackTypes, String region, List<Rule> rules);

    void updateBaseUrl(CloudBaseUrl baseUrl);

	void setEndpointDao(EndpointDao endpointDao);

    /**
     * Whether or not the specified baseUrl belongs to the configured cloud region.
     * @param baseUrl
     * @return
     */
    boolean doesBaseUrlBelongToCloudRegion(CloudBaseUrl baseUrl);
}
