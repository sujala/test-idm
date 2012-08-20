package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.EndpointForService;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;
import org.openstack.docs.identity.api.v2.VersionForService;

import java.util.List;


public class OpenStackServiceCatalogFactory {
    public ServiceCatalog createNew(List<OpenstackEndpoint> endPoints) {
        if (endPoints == null) {
            throw new IllegalArgumentException("endPoints can not be null");
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog();

        for (OpenstackEndpoint endPoint : endPoints) {
            processEndpoint(serviceCatalog, endPoint);
        }

        return serviceCatalog;
    }

    static void processEndpoint(ServiceCatalog serviceCatalog,
        OpenstackEndpoint endPoint) {
        
        for (CloudBaseUrl baseUrl : endPoint.getBaseUrls()) {
            
            ServiceForCatalog currentService = new OpenStackServiceCatalogHelper(serviceCatalog)
            .getEndPointService(baseUrl.getServiceName(), baseUrl.getOpenstackType());
            
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());
            
            EndpointForService endpointItem = new EndpointForService();
            
            endpointItem.setAdminURL(baseUrl.getAdminUrl());
            endpointItem.setInternalURL(baseUrl.getInternalUrl());
            endpointItem.setPublicURL(baseUrl.getPublicUrl());
            endpointItem.setTenantId(endPoint.getTenantId());
            endpointItem.setRegion(baseUrl.getRegion());
            if (!StringUtils.isBlank(version.getId())) {
                endpointItem.setVersion(version);
            }
            if (baseUrl.isV1Default()) {
                currentService.getEndpoint().add(0, endpointItem);
            } else {
                currentService.getEndpoint().add(endpointItem);
            }
        }
    }
}
