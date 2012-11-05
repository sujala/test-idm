package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.exception.BadRequestException;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

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
    Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public JAXBElement<Capability> toCapability(com.rackspace.idm.domain.entity.Capability capability) {
        Capability capabilityEntity = mapper.map(capability, Capability.class);

        //This is needed since Capability.java for the generated sources does not have a setter for resources
        if (capability.getResources() != null) {
            for (String resource : capability.getResources()) {
                capabilityEntity.getResources().add(resource);
            }
        }

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createCapability(capabilityEntity);
    }

    public com.rackspace.idm.domain.entity.Capability fromCapability(Capability capabilityEntity) {
        return mapper.map(capabilityEntity, com.rackspace.idm.domain.entity.Capability.class);
    }

    public JAXBElement<Capabilities> toCapabilities(List<com.rackspace.idm.domain.entity.Capability> capabilities) {
        Capabilities capabilitiesEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createCapabilities();
        if (capabilities != null) {
            for (com.rackspace.idm.domain.entity.Capability capability : capabilities) {
                capabilitiesEntity.getCapability().add(toCapability(capability).getValue());
            }
        }
        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createCapabilities(capabilitiesEntity);
    }

    public com.rackspace.idm.domain.entity.Capabilities fromCapabilities(Capabilities capabilities) {
        if (capabilities == null || capabilities.getCapability().size() == 0) {
            throw new BadRequestException("Capabilities can not be empty or null");
        }
        com.rackspace.idm.domain.entity.Capabilities capabilitiesDO = new com.rackspace.idm.domain.entity.Capabilities();
            for (Capability capability : capabilities.getCapability()) {
                capabilitiesDO.getCapability().add(fromCapability(capability));
            }

        return capabilitiesDO;
    }
}
