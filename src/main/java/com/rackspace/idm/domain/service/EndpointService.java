package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.List;

public interface EndpointService {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void deleteBaseUrl(int baseUrlId);

    CloudBaseUrl getBaseUrlById(int baseUrlId);

    CloudBaseUrl checkAndGetEndpointTemplate(int id);

    CloudBaseUrl checkAndGetEndpointTemplate(String id);

    List<CloudBaseUrl> getBaseUrls();

    List<CloudBaseUrl> getGlobalBaseUrls();
    
    List<CloudBaseUrl> getDefaultBaseUrls();

    void setBaseUrlEnabled(int baseUrlId, boolean enabled);

    List<CloudBaseUrl> getBaseUrlsByServiceType(String serviceType);

    List<CloudBaseUrl> getBaseUrlsByServiceName(String serviceName);
    
    List<CloudBaseUrl> getBaseUrlsByBaseUrlType(String baseUrlType);

    List<OpenstackEndpoint> getEndpointsFromTenantList(List<Tenant> tenantList);

    void updateBaseUrl(CloudBaseUrl baseUrl);

	void addPolicyToEndpoint(int baseUrlId, String policyId);

	void deletePolicyToEndpoint(int baseUrlId, String policyId);

	void setEndpointDao(EndpointDao endpointDao);
}
