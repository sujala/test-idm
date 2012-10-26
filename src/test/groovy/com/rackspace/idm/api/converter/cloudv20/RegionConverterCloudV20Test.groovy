package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.converter.cloudv20.RegionConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.Region
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

class RegionConverterCloudV20Test extends Specification {

    @Shared RegionConverterCloudV20 converterCloudV20
    @Shared def US_CLOUD = "US"

    def setupSpec() {
        converterCloudV20 = new RegionConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories();
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert region from ldap to jersey object"() {
        when:
        Region region = region()
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = converterCloudV20.toRegion(region).value

        then:
        region.name == regionEntity.name
        region.isEnabled == regionEntity.enabled
        region.isDefault == regionEntity.isDefault

    }

    def "convert region from jersey object to ldap object"() {
        given:
        Configuration config = Mock()
        converterCloudV20.config = config
        config.getString("cloud.region") >> US_CLOUD

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = regionEntity()
        Region region = converterCloudV20.fromRegion(regionEntity)

        then:
        regionEntity.name == region.name
        regionEntity.enabled == region.isEnabled
        regionEntity.isDefault == region.isDefault
    }

    def "convert regions from ldap object to jersey object"() {
        given:
        Region region = region()
        List<Region> regions = new ArrayList<Region>();
        regions.add(region)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions regionsEntity = converterCloudV20.toRegions(regions).value

        then:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = regionsEntity.region.get(0)
        regionEntity.name == region.name
        regionEntity.enabled == region.isEnabled
        regionEntity.isDefault == region.isDefault
    }

    def "cloud gets assigned from config file"() {
        given:
        Configuration config = Mock()
        converterCloudV20.config = config
        config.getString("cloud.region") >> US_CLOUD

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Region regionEntity = regionEntity()
        Region region = converterCloudV20.fromRegion(regionEntity)

        then:
        region.cloud == US_CLOUD
    }

    def region(String name, String cloud, isEnabled, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = isDefault
            it.isEnabled = isEnabled
            return it
        }
    }

    def region() {
        return region("name", "cloud", true, false)
    }

    def regionEntity(String name, Boolean isEnabled, Boolean isDefault) {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.Region().with {
            it.name = name
            it.enabled = isEnabled
            it.isDefault = isDefault
            return it
        }
    }

    def regionEntity() {
        return regionEntity("name", true, false)
    }
}
