package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.CloudBaseUrl;

import java.util.List;

public interface EndpointDao {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void deleteBaseUrl(String baseUrlId);
    
    CloudBaseUrl getBaseUrlById(String baseUrlId);

    Iterable<CloudBaseUrl> getBaseUrlsByService(String service);

    Iterable<CloudBaseUrl> getBaseUrlsByOpenStackType(String openStackType);

    Iterable<CloudBaseUrl> getGlobalUSBaseUrlsByBaseUrlType(String openStackType);

    Iterable<CloudBaseUrl> getGlobalUKBaseUrlsByBaseUrlType(String openStackType);

    Iterable<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId);

    Iterable<CloudBaseUrl> getBaseUrls();

    Iterable<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds);

    void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl);
}
