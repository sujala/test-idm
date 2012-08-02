package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
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

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ApplicationService applicationService;

    public Set<String> getRegionList(String userId){
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByUserId(userId);
        List<OpenstackEndpoint> openstackEndpointsForScopeAccess = scopeAccessService.getOpenstackEndpointsForScopeAccess(scopeAccess);
        Set<String> regions = new HashSet<String>();
        for (OpenstackEndpoint endpoint : openstackEndpointsForScopeAccess){
            List<CloudBaseUrl> baseUrls = endpoint.getBaseUrls();
            if(baseUrls!=null){
                for (CloudBaseUrl baseUrl : baseUrls){
                    regions.add(baseUrl.getRegion());
                }
            }
        }
        return regions;
    }

    public Set<String> getDefaultRegions(){
        List<Application> openStackServices = applicationService.getOpenStackServices();
        Set<String> defaultRegions = new HashSet<String>();
        if (openStackServices!=null){
            for (Application application : openStackServices){
                if(application.getUseForDefaultRegion()!=null && application.getUseForDefaultRegion()){
                    List<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByServiceName(application.getName());
                    for (CloudBaseUrl baseUrl : baseUrls){
                        defaultRegions.add(baseUrl.getRegion());
                    }
                }
            }
        }
        return defaultRegions;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }
}
