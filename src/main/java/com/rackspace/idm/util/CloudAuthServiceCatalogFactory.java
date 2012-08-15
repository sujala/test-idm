package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;

import java.util.List;

public class CloudAuthServiceCatalogFactory {
    public ServiceCatalog createNew(List<OpenstackEndpoint> endPoints) {
        if (endPoints == null) {
            throw new IllegalArgumentException("endPoints can not be null");
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog();

        for (OpenstackEndpoint endPoint : endPoints) {
            processService(serviceCatalog, endPoint);
        }

        return serviceCatalog;
    }

    static void processService(ServiceCatalog serviceCatalog, OpenstackEndpoint openstackEndPoint) {
        if(openstackEndPoint.getBaseUrls() != null) {
            for (CloudBaseUrl baseUrl : openstackEndPoint.getBaseUrls()) {
            	Service service = getServiceFromName(serviceCatalog, baseUrl.getServiceName());
            	
            	if (service != null) {
	                addEndpoint(service, baseUrl);
            	} else {
	                service = new Service();
	                service.setName(baseUrl.getServiceName());
	                addEndpoint(service, baseUrl);
	                serviceCatalog.getService().add(service);
            	}
            }
        }
    }

	private static void addEndpoint(Service service, CloudBaseUrl baseUrl) {
		Endpoint endpoint = new Endpoint();
		endpoint.setAdminURL(baseUrl.getAdminUrl());
		endpoint.setInternalURL(baseUrl.getInternalUrl());
		endpoint.setPublicURL(baseUrl.getPublicUrl());
		endpoint.setRegion(baseUrl.getRegion());
		endpoint.setV1Default(baseUrl.isV1Default());
        if (endpoint.isV1Default()) {
            service.getEndpoint().add(0,endpoint);
        }else{
            service.getEndpoint().add(endpoint);
        }
    }
    
    static Service getServiceFromName(ServiceCatalog serviceCatalog, String name) {
    	for(Service service : serviceCatalog.getService()) {
    		if (name.equalsIgnoreCase(service.getName())) {
    			return service;
    		}
    	}
    	return null;
    }

}
