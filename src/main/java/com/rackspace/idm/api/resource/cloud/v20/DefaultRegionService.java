package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    static String CLOUD_SERVERS_OPENSTACK = "cloudServersOpenStack";

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private DefaultCloudRegionService defaultCloudRegionService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    public void validateDefaultRegion(String defaultRegion) {
        Set<String> regions = this.getDefaultRegionsForCloudServersOpenStack();
        checkDefaultRegion(defaultRegion, regions);
    }

    public void validateDefaultRegion(String defaultRegion, ScopeAccess usa) {
        Set<String> regions = this.getDefaultRegionsForUser(usa);
        checkDefaultRegion(defaultRegion, regions);
    }

    public void checkDefaultRegion(String region, Set<String> defaultRegions) {
        String regionString = "";
        int size = defaultRegions.size();
        int counter = 0;
        for (String defaultRegion : defaultRegions) {
            counter = counter + 1;
            if (counter < size) {
                regionString += " " + defaultRegion + ",";
            } else {
                regionString += " " + defaultRegion;
            }
        }
        if (region != null && !defaultRegions.contains(region)) {
            throw new BadRequestException("Invalid defaultRegion value, accepted values are:" + regionString.toUpperCase() + ".");
        }
    }

    public Set<String> getDefaultRegionsForUser(ScopeAccess usa) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        for (OpenstackEndpoint endpoint : endpoints) {
            baseUrls.addAll(endpoint.getBaseUrls());
        }

        Set<String> defaultRegions = getCloudServersOpenStackRegions(baseUrls);

        if(defaultRegions.size() == 0){
            defaultRegions.addAll(getDefaultRegionsForCloudServersOpenStack());
        }

        return defaultRegions;
    }

    private Set<String> getCloudServersOpenStackRegions(List<CloudBaseUrl> baseUrls) {
        Set<String> defaultRegions = new HashSet<String>();
        for (CloudBaseUrl baseUrl : baseUrls) {
            if (baseUrl.getServiceName().equalsIgnoreCase(CLOUD_SERVERS_OPENSTACK)) {
                defaultRegions.add(baseUrl.getRegion());
            }
        }
        return defaultRegions;
    }

    private Set<String> getRegionsWithinCloud(Set<String> regionNames) {
        List<String> regionsInCloudRegion = new ArrayList<String>();

        for (Region region : defaultCloudRegionService.getRegions(config.getString("cloud.region"))) {
            regionsInCloudRegion.add(region.getName());
        }

        regionNames.retainAll(regionsInCloudRegion);

        return regionNames;
    }

    public Set<String> getDefaultRegionsForCloudServersOpenStack() {
        List<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK);

        return getRegionsWithinCloud(getCloudServersOpenStackRegions(baseUrls));
    }



    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setDefaultCloudRegionService(DefaultCloudRegionService defaultCloudRegionService) {
        this.defaultCloudRegionService = defaultCloudRegionService;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
