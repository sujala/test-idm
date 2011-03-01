package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.jaxb.Service;
import com.rackspace.idm.jaxb.ServiceCatalog;

public class ServiceCatalogHelper {
    private ServiceCatalog serviceCatalog;

    public ServiceCatalogHelper(ServiceCatalog serviceCatalog) {
        if (serviceCatalog == null) {
            throw new IllegalArgumentException("serviceCatalog can not be null");
        }

        this.serviceCatalog = serviceCatalog;
    }

    public Boolean contains(String serviceName) {
        return getService(serviceName) != null;
    }

    public Service getService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName can not be null");
        }

        Service matchingService = null;

        for (Service service : serviceCatalog.getServices()) {
            if (serviceName.equals(service.getName())) {
                matchingService = service;
                break;
            }
        }

        return matchingService;
    }

    public Service getEndPointService(CloudEndpoint endPoint) {
        if (endPoint == null) {
            throw new IllegalArgumentException("endPoint can not be null");
        }
        if (endPoint.getBaseUrl() == null) {
            throw new IllegalArgumentException("endPoint's urlBase can not be null");
        }
        if (endPoint.getBaseUrl().getService() == null) {
            throw new IllegalArgumentException("endPoint's urlBase's service can not be null");
        }

        Service currentService;
        String serviceName = endPoint.getBaseUrl().getService();

        currentService = getService(serviceName);

        if (currentService == null) {
            currentService = new Service();
            currentService.setName(serviceName);
            serviceCatalog.getServices().add(currentService);
        }

        return currentService;
    }
}
