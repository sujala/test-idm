package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.RegionDao
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

    def "create region must contain a name"() {
        when:
        cloudRegionService.addRegion(region(null, "cloud", false))
        then:
        thrown(BadRequestException)
    }

    def "create region must contain a cloud"() {
        when:
        cloudRegionService.addRegion(region("name", null, false))
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
        RegionDao regionDao = Mock()
        cloudRegionService.regionDao = regionDao

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
        RegionDao regionDao = Mock()
        cloudRegionService.regionDao = regionDao

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

    def region(String name, String cloud, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = isDefault
            return it
        }
    }

    def region() {
        return region("name", "cloud", false)
    }
}
