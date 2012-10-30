package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Region;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

@Component
public class RegionConverterCloudV20 {

    @Autowired
    Configuration config;

    @Autowired
    private JAXBObjectFactories objFactories;

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Region> toRegion(Region region) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegion();
        regionEntity.setName(region.getName());
        regionEntity.setEnabled(region.getIsEnabled());
        regionEntity.setIsDefault(region.getIsDefault());
        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegion(regionEntity);
    }

    public Region fromRegion(com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity) {
        Region region = new Region();
        region.setName(regionEntity.getName());
        region.setIsEnabled(regionEntity.isEnabled());
        region.setIsDefault(regionEntity.isIsDefault());
        region.setCloud(config.getString("cloud.region"));
        return region;
    }

    public JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions>  toRegions(List<Region> regions) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions regionsEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegions();

        for (Region region : regions) {
            JAXBElement<com.rackspace.docs.identity.api.ext.rax_auth.v1.Region> regionEntity = toRegion(region);
            regionsEntity.getRegion().add(regionEntity.getValue());
        }

        return objFactories.getRackspaceIdentityExtRaxgaV1Factory().createRegions(regionsEntity);
    }
}
