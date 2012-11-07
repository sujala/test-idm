package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.impl.LdapRegionRepository
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.domain.service.CloudRegionService
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import spock.lang.Specification

class DefaultCloudRegionServiceTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared def US_CLOUD_REGION = "us_cloud_region"

    @Shared CloudRegionService cloudRegionService
    @Shared LdapRegionRepository regionDao

    def setupSpec() {
        random = ("$randomness").replace('-',"")
        cloudRegionService = new DefaultCloudRegionService()
    }

    def cleanupSpec() {
    }

    def "create region with null returns bad request"() {
        when:
        cloudRegionService.addRegion(null)
        then:
        thrown(BadRequestException)
    }

    def "create region must be alpha numeric"() {
        given:
        setupMocks()

        when:
        cloudRegionService.addRegion(region("!@#", "cloud", true, false))

        then:
        thrown(BadRequestException)
    }

    def "create region must contain a name"() {
        when:
        cloudRegionService.addRegion(region(null, "cloud", true, false))
        then:
        thrown(BadRequestException)
    }

    def "create region must contain a cloud"() {
        when:
        cloudRegionService.addRegion(region("name", null, true, false))
        then:
        thrown(BadRequestException)
    }

    def "update a region must contain id"() {
        when:
        cloudRegionService.updateRegion(null, region())
        then:
        thrown(BadRequestException)

    }

    def "update a region must contain region"() {
        when:
        cloudRegionService.updateRegion("1000", null)
        then:
        thrown(BadRequestException)
    }

    def "update region returns not found if missing"() {
        given:
        setupMocks()

        when:
        cloudRegionService.updateRegion("missing", region())

        then:
        thrown(NotFoundException)
    }

    def "delete region must contain name"() {
        when:
        cloudRegionService.deleteRegion(null)

        then:
        thrown(BadRequestException)
    }

    def "delete region returns not found if missing"() {
        given:
        setupMocks()

        when:
        cloudRegionService.deleteRegion("missing")

        then:
        thrown(NotFoundException)
    }

    def "get default region must contain cloud"() {
        when:
        cloudRegionService.getDefaultRegion(null)
        then:
        thrown(BadRequestException)
    }

    def "creating a default region sets all others as not default"() {
        given:
        setupMocks()
        def region1 = region("DFW", "US", true, true)

        regionDao.getRegion(_) >>  null
        regionDao.getDefaultRegion(_) >> region1

        when:
        cloudRegionService.addRegion(region("IAD", "US", true, true))

        then:
        region1.isDefault == false

    }

    def "creating a non default region does not change other regions"() {
        given:
        setupMocks()
        def regions = new ArrayList<Region>()
        def region1 = region("DFW", "US", true, true)
        regions.add(region1)

        regionDao.getRegions(_) >> regions

        when:
        cloudRegionService.addRegion(region("IAD", "US", true, false))

        then:
        region1.isDefault == true
    }

    def "updating a default region sets all others as not default"() {
        given:
        setupMocks()
        def region1 = region("DFW", "US", true, true)
        def region2 = region("IAD", "US", true, true)

        regionDao.getRegion(_) >>  region2
        regionDao.getDefaultRegion(_) >> region1

        when:
        cloudRegionService.updateRegion("IAD", region2)

        then:
        region1.isDefault == false
    }

    def "cannot set default region to non default"() {
        given:
        setupMocks()
        def regions = new ArrayList<Region>()
        def region1 = region("DFW", "US", true, true)
        def region2 = region("DFW", "US", true, false)
        regions.add(region1)

        regionDao.getRegion(_) >> region1
        regionDao.getRegions(_) >> regions

        when:
        cloudRegionService.updateRegion("IAD", region2)

        then:
        thrown(BadRequestException)
    }

    def "default region cannot be deleted"() {
        given:
        setupMocks()
        def region = region("DFW", "US", true, true)
        regionDao.getRegion(_) >> region

        when:
        cloudRegionService.deleteRegion("DFW")

        then:
        thrown(BadRequestException)
    }

    def "default region cannot be disabled"() {
        given:
        setupMocks()
        def region1 = region("DFW", "US", true, true)
        def region2 = region("DFW", "US", false, true)

        regionDao.getRegion(_) >> region1
        regionDao.getDefaultRegion(_) >> region1

        when:
        cloudRegionService.updateRegion("DFW", region2)

        then:
        thrown(BadRequestException)
    }

    def "create region with default not enabled does not change other regions"() {
        given:
        setupMocks()
        def region1 = region("ORD", "US", true, true)
        def region2 = region("DFW", "US", false, true)

        regionDao.getDefaultRegion(_) >> region1

        when:
        cloudRegionService.addRegion(region2)

        then:
        region1.isDefault == true
    }

    def region(String name, String cloud, Boolean isEnabled, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = isDefault
            it.isEnabled = isEnabled
            return it
        }
    }

    def setupMocks() {
        regionDao = Mock()
        cloudRegionService.regionDao = regionDao
    }

    def region() {
        return region("name", "cloud", true, false)
    }
}
