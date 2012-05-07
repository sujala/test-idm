package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;

import java.util.List;

public interface EndpointService {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void addBaseUrlToUser(int baseUrlId, boolean def, String username);

    void deleteBaseUrl(int baseUrlId);

    CloudBaseUrl getBaseUrlById(int baseUrlId);

    List<CloudBaseUrl> getBaseUrls();

    List<CloudBaseUrl> getGlobalBaseUrls();
    
    List<CloudBaseUrl> getDefaultBaseUrls();

    CloudEndpoint getEndpointForUser(String username, int baseUrlId);

    List<CloudEndpoint> getEndpointsForUser(String username);

    void removeBaseUrlFromUser(int baseUrlId, String username);
    
    void setBaseUrlEnabled(int baseUrlId, boolean enabled);

    List<CloudBaseUrl> getBaseUrlsByServiceId(String serviceType);

    void updateBaseUrl(CloudBaseUrl baseUrl);
}
