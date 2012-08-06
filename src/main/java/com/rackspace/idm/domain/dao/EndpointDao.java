package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.List;

public interface EndpointDao {
    void addBaseUrl(CloudBaseUrl baseUrl);

    void addBaseUrlToUser(int baseUrlId, boolean def, String username);
    
    void deleteBaseUrl(int baseUrlId);
    
    CloudBaseUrl getBaseUrlById(int baseUrlId);

    List<CloudBaseUrl> getBaseUrlsByService(String service);

    List<CloudBaseUrl> getBaseUrls();

    List<CloudEndpoint> getEndpointsForUser(String username);
    
    void removeBaseUrlFromUser(int baseUrlId, String username);

    void setBaseUrlEnabled(int baseUrlId, boolean enabled);

    OpenstackEndpoint getOpenstackEndpointsForTenant(Tenant tenant);

    void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl);
}
