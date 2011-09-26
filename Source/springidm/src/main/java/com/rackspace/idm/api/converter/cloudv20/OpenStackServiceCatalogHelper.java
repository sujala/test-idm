package com.rackspace.idm.api.converter.cloudv20;

import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;

public class OpenStackServiceCatalogHelper {
    private final ServiceCatalog serviceCatalog;

    public OpenStackServiceCatalogHelper(ServiceCatalog serviceCatalog) {
        if (serviceCatalog == null) {
            throw new IllegalArgumentException("serviceCatalog can not be null");
        }

        this.serviceCatalog = serviceCatalog;
    }

    public Boolean contains(String serviceName) {
        return getService(serviceName) != null;
    }

    public ServiceForCatalog getService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName can not be null");
        }

        ServiceForCatalog matchingService = null;

        for (ServiceForCatalog service : serviceCatalog.getService()) {
            if (serviceName.equals(service.getName())) {
                matchingService = service;
                break;
            }
        }

        return matchingService;
    }

    public ServiceForCatalog getEndPointService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("endPoint can not be null");
        }

        ServiceForCatalog currentService = getService(serviceName);

        if (currentService == null) {
            currentService = new ServiceForCatalog();
            currentService.setName(serviceName);
            serviceCatalog.getService().add(currentService);
        }

        return currentService;
    }
}
