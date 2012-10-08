package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.taglibs.standard.tag.common.core.SetSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Hector
 * Date: 8/1/12
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultRegionService {

    static String CLOUD_SERVERS_OPENSTACK = "cloudserversopenstack";

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private ApplicationService applicationService;

    public void validateDefaultRegion(String defaultRegion) {
        Set<String> regions = this.getDefaultRegions();
        checkDefaultRegion(defaultRegion, regions);
    }

    public void validateDefaultRegion(String defaultRegion, ScopeAccess usa) {
        Set<String> regions = this.getDefaultRegionsForUser(usa);
        checkDefaultRegion(defaultRegion, regions);
    }

    public void checkDefaultRegion(String region, Set<String> defaultRegions) {
        String regionString = "";
        for (String defaultRegion : defaultRegions) {
            regionString += " " + defaultRegion;
        }
        if (region != null && !defaultRegions.contains(region)) {
            throw new BadRequestException("Invalid defaultRegion value, accepted values are:" + regionString + ".");
        }
    }

    public Set<String> getDefaultRegionsForUser(ScopeAccess usa) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);
        Set<String> defaultRegions = new HashSet<String>();
        for (OpenstackEndpoint endpoint : endpoints) {
            List<CloudBaseUrl> baseUrls = endpoint.getBaseUrls();
            for (CloudBaseUrl baseUrl : baseUrls) {
                if (baseUrl.getServiceName().equalsIgnoreCase(CLOUD_SERVERS_OPENSTACK)) {
                    defaultRegions.add(baseUrl.getRegion());
                }
            }
        }
        return defaultRegions;
    }

    public Set<String> getDefaultRegions() {
        List<Application> openStackServices = applicationService.getOpenStackServices();
        Set<String> defaultRegions = new HashSet<String>();
        if (openStackServices != null) {
            for (Application application : openStackServices) {
                if (application.getUseForDefaultRegion() != null && application.getUseForDefaultRegion()) {
                    List<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByServiceName(application.getName());
                    for (CloudBaseUrl baseUrl : baseUrls) {
                    	if (baseUrl.getRegion() != null) {
                    		defaultRegions.add(baseUrl.getRegion());
                    	}
                    }
                }
            }
        }
        return defaultRegions;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }
}
