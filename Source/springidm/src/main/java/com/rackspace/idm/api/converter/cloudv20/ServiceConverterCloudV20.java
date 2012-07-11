package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Application;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceConverterCloudV20 {
    
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    public Service toService(Application client) {
        Service service = OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService();
        
        service.setId(client.getClientId());
        service.setName(client.getName());
        service.setType(client.getOpenStackType());
        service.setDescription(client.getDescription());

        return service;
    }
    
    public ServiceList toServiceList(List<Application> clients) {
        ServiceList list = OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createServiceList();
        
        for (Application client : clients) {
            list.getService().add(this.toService(client));
        }
        return list;
    }

    public void setOBJ_FACTORIES(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }
}
