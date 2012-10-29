package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.PolicyDao
import com.rackspace.idm.domain.dao.RegionDao
import com.rackspace.idm.domain.entity.Region
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapRegionRepositoryIntegrationTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared def name
    @Shared def name1
    @Shared def name2
    @Shared def US_CLOUD_REGION = "us_cloud_region"
    @Shared def UK_CLOUD_REGION = "uk_cloud_region"

    @Autowired
    private RegionDao regionDao

    @Autowired
    private PolicyDao policyDao

    def setupSpec() {
        random = ("$randomness").replace('-',"")
        name = "name${random}"
        name1 = name
        name2 = "${name1}2"
    }

    def cleanupSpec() {
    }

    def "region crud"() {
        when:
        Region region = region(name)
        List<Region> regions = regionDao.getRegions()
        regionDao.addRegion(region)
        List<Region> modifiedRegions = regionDao.getRegions()
        Region newRegion = regionDao.getRegion(name)
        regionDao.deleteRegion(name)
        List<Region> originalRegions = regionDao.getRegions()

        then:
        modifiedRegions.size() == regions.size() + 1
        regions.size() == originalRegions.size()
        region.equals(newRegion)
    }

    def "get region by cloud region returns region in cloud"() {
        when:
        List<Region> emptyRegions = regionDao.getRegions(US_CLOUD_REGION)

        Region region1 = region(name1, US_CLOUD_REGION, true, false)
        Region region2 = region(name2, UK_CLOUD_REGION, true, false)

        regionDao.addRegion(region1)
        regionDao.addRegion(region2)

        List<Region> regions = regionDao.getRegions(US_CLOUD_REGION)

        regionDao.deleteRegion(name1)
        regionDao.deleteRegion(name2)

        then:
        emptyRegions.size() == 0
        regions.size() == 1
    }

    def "get default region returns default region"() {
        when:
        Region newRegionNotFound = regionDao.getDefaultRegion(US_CLOUD_REGION)

        Region region1 = region(name1, US_CLOUD_REGION, true, true)
        Region region2 = region(name2, US_CLOUD_REGION, true, false)

        regionDao.addRegion(region1)
        regionDao.addRegion(region2)

        Region newRegion = regionDao.getDefaultRegion(US_CLOUD_REGION)
        List<Region> regions = regionDao.getRegions()

        regionDao.deleteRegion(name1)
        regionDao.deleteRegion(name2)

        then:
        newRegionNotFound == null
        newRegion.equals(region1)
        regions.size() >= 2
    }

    def "get default region returns default region only if enabled"() {
        when:
        Region region1 = region(name1, US_CLOUD_REGION, false, true)
        Region region2 = region(name2, US_CLOUD_REGION, false, false)

        regionDao.addRegion(region1)
        regionDao.addRegion(region2)

        Region newRegion = regionDao.getDefaultRegion(US_CLOUD_REGION)

        regionDao.deleteRegion(name1)
        regionDao.deleteRegion(name2)

        then:
        newRegion == null
    }

    def region(String name, String cloud, Boolean isEnabled, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isEnabled = isEnabled
            it.isDefault = isDefault
            return it
        }
    }

    def region(String name) {
        return region(name, "cloud", true, false)
    }
}
