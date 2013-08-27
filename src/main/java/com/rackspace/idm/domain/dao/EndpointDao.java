package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.CloudBaseUrl;

import java.util.List;

public interface EndpointDao {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void deleteBaseUrl(String baseUrlId);
    
    CloudBaseUrl getBaseUrlById(String baseUrlId);

    List<CloudBaseUrl> getBaseUrlsByService(String service);

    List<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId);

    List<CloudBaseUrl> getBaseUrls();

    List<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds);

    void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl);
}
