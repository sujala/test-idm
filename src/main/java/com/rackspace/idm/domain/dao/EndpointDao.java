package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.CloudBaseUrl;

import java.util.List;

public interface EndpointDao {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void deleteBaseUrl(String baseUrlId);
    
    CloudBaseUrl getBaseUrlById(String baseUrlId);

    Iterable<CloudBaseUrl> getBaseUrlsByService(String service);

    /**
     * Returns base URLs that match the following criteria:
     * CloudBaseURL.def == true
     * CloudBaseUrl.baseUrlType == baseUrlType parameter
     * CloudBaseUrl.enabled == enabled parameter
     *
     * @param baseUrlType The type of the base url to match on
     * @param enabled If the base urls are enabled or not
     * @return
     */
    Iterable<CloudBaseUrl> getDefaultBaseUrlsByBaseUrlTypeAndEnabled(String baseUrlType, boolean enabled);

    Iterable<CloudBaseUrl> getGlobalUSBaseUrlsByBaseUrlType(String openStackType);

    Iterable<CloudBaseUrl> getGlobalUKBaseUrlsByBaseUrlType(String openStackType);

    Iterable<CloudBaseUrl> getBaseUrls();

    Iterable<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds);

    void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl);

    /**
     * Returns the total number of base URLs
     *
     * @return
     */
    int getBaseUrlCount();

}
