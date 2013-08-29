package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.Iterator;
import java.util.List;

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

    Iterable<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId);

    List<OpenstackEndpoint> getEndpointsFromTenantList(List<Tenant> tenantList);

    OpenstackEndpoint getOpenStackEndpointForTenant(Tenant tenant);

    void updateBaseUrl(CloudBaseUrl baseUrl);

	void addPolicyToEndpoint(String baseUrlId, String policyId);

	void deletePolicyToEndpoint(String baseUrlId, String policyId);

	void setEndpointDao(EndpointDao endpointDao);
}
