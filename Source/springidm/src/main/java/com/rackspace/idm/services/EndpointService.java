package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;

public interface EndpointService {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void addBaseUrlToUser(int baseUrlId, boolean def, String username);

    void deleteBaseUrl(int baseUrlId);

    CloudBaseUrl getBaseUrlById(int baseUrlId);

    List<CloudBaseUrl> getBaseUrls();

    CloudEndpoint getEndpointForUser(String username, int baseUrlId);

    List<CloudEndpoint> getEndpointsForUser(String username);

    void removeBaseUrlFromUser(int baseUrlId, String username);
}
