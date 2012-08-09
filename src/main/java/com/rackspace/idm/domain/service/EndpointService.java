package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.CloudBaseUrl;

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

    void updateBaseUrl(CloudBaseUrl baseUrl);
}
