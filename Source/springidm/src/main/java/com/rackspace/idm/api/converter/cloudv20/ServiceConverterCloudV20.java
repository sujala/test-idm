package com.rackspace.idm.api.converter.cloudv20;

import java.util.List;

import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Client;

@Component
public class ServiceConverterCloudV20 {
    
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    
    public Service toService(Client client) {
        Service service = OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService();
        
        service.setDescription(client.getDescription());
        service.setId(client.getClientId());
        service.setType(client.getOpenStackType());
        
        return service;
    }
    
    public ServiceList toServiceList(List<Client> clients) {
        ServiceList list = OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createServiceList();
        
        for (Client client : clients) {
            list.getService().add(this.toService(client));
        }
        return list;
    }
}
