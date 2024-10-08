package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Application;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public Service toService(Application client) {
        return mapper.map(client, Service.class);
    }
    
    public ServiceList toServiceList(Iterable<Application> clients) {
        ServiceList list = objFactories.getOpenStackIdentityExtKsadmnV1Factory().createServiceList();
        
        for (Application client : clients) {
            list.getService().add(this.toService(client));
        }
        return list;
    }
}
