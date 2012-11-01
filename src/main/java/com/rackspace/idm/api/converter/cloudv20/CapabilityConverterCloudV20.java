package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/25/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class CapabilityConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories objFactories;

    public Capability toCapability(com.rackspace.idm.domain.entity.Capability capability) {
        Capability jaxbCapability = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createCapability();
        jaxbCapability.setAction(capability.getAction());
        jaxbCapability.setId(capability.getId());
        jaxbCapability.setName(capability.getName());
        jaxbCapability.setUrl(capability.getUrl());
        jaxbCapability.setDescription(capability.getDescription());
        for(String resource : capability.getResources()){
            jaxbCapability.getResources().add(resource);
        }
        return jaxbCapability;

    }

    public com.rackspace.idm.domain.entity.Capability toCapabilityDO(Capability capability){
        com.rackspace.idm.domain.entity.Capability capabilityDO = new com.rackspace.idm.domain.entity.Capability();
        capabilityDO.setAction(capability.getAction());
        capabilityDO.setId(capability.getId());
        capabilityDO.setName(capability.getName());
        capabilityDO.setUrl(capability.getUrl());
        capabilityDO.setDescription(capability.getDescription());
        for(String resource : capability.getResources()){
            capabilityDO.getResources().add(resource);
        }
        return capabilityDO;
    }

    public Capabilities toCapabilities(com.rackspace.idm.domain.entity.Capabilities capabilities){
        Capabilities jaxbCapabilities = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createCapabilities();
        for(com.rackspace.idm.domain.entity.Capability capability : capabilities.getCapability()){
            jaxbCapabilities.getCapability().add(toCapability(capability));
        }
        return jaxbCapabilities;
    }

    public com.rackspace.idm.domain.entity.Capabilities toCapabilitiesDO(Capabilities capabilities){
        com.rackspace.idm.domain.entity.Capabilities capabilitiesDO = new com.rackspace.idm.domain.entity.Capabilities();
        for(Capability capability : capabilities.getCapability()){
            capabilitiesDO.getCapability().add(toCapabilityDO(capability));
        }
        return capabilitiesDO;
    }
}
