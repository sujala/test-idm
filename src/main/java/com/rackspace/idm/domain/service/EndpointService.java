package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;

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

    /**
     * Calculate the OpenStackEndpoint based on the TenantEndpointMeta
     *
     * @param tenantEndpointMeta
     * @return
     */
    OpenstackEndpoint calculateOpenStackEndpointForTenantMeta(TenantEndpointMeta tenantEndpointMeta);

    void updateBaseUrl(CloudBaseUrl baseUrl);

	void setEndpointDao(EndpointDao endpointDao);

    /**
     * Whether or not the specified baseUrl belongs to the configured cloud region.
     * @param baseUrl
     * @return
     */
    @Deprecated
    boolean doesBaseUrlBelongToCloudRegion(CloudBaseUrl baseUrl);

    /**
     * Whether or not the specified baseUrl belongs to cloud region. The domain type is used to determine the region.
     *
     * @param baseUrl
     * @return
     */
    boolean doesBaseUrlBelongToCloudRegion(CloudBaseUrl baseUrl, Domain domain);
}
