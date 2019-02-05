package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Region;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

@Component
public class RegionConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Region> toRegion(Region region) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = mapper.map(
                region, com.rackspace.docs.identity.api.ext.rax_auth.v1.Region.class
        );

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegion(regionEntity);
    }

    public Region fromRegion(com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity) {
        Region region = mapper.map(regionEntity, Region.class);
        region.setIsDefault(regionEntity.isIsDefault());
        region.setIsEnabled(regionEntity.isEnabled());
        region.setCloud(regionEntity.getCloud());
        return region;
    }

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions>  toRegions(Iterable<Region> regions) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions regionsEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegions();

        for (Region region : regions) {
            JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Region> regionEntity = toRegion(region);
            regionsEntity.getRegion().add(regionEntity.getValue());
        }

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegions(regionsEntity);
    }
}
