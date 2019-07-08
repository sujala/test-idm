package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.CloudRegion
import com.rackspace.idm.domain.entity.DomainType
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import testHelpers.RootServiceTest

import static com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService.*

class DefaultRegionServiceTest extends RootServiceTest {

    @Shared DefaultRegionService service

    def setup() {
        service = new DefaultRegionService()

        mockCloudRegionService(service)
        mockDomainService(service)
        mockEndpointService(service)
        mockIdentityConfig(service)
        mockScopeAccessService(service)
        mockUserService(service)
    }

    def "getDefaultRegions returns only regions within cloud region"() {
        given:
        def baseUrlListOne = [
               createBaseUrl("ORD", service.CLOUD_SERVERS_OPENSTACK),
               createBaseUrl("DFW", service.CLOUD_SERVERS_OPENSTACK),
               createBaseUrl("LON", service.CLOUD_SERVERS_OPENSTACK)
        ].asList()

        def baseUrlListTwo = [
                createBaseUrl("ORD", service.CLOUD_SERVERS_OPENSTACK),
                createBaseUrl("DFW", "notCloudServersOpenStack"),
                createBaseUrl("LON", service.CLOUD_SERVERS_OPENSTACK)
        ].asList()

        def regionsInCloud = [
                createRegion("US", "ORD"),
                createRegion("US", "DFW")
        ].asList()

        identityConfig.staticConfig.getCloudRegion() >> "US"
        cloudRegionService.getRegions(_) >> regionsInCloud

        when: "different cloud regions endpoints"
        def regionListOne = service.getDefaultRegionsForCloudServersOpenStack()

        then: "assert LON endpoint is not returned"
        regionListOne.size() == 2

        assert(!regionListOne.contains("LON"))
        assert(regionListOne.contains("ORD"))
        assert(regionListOne.contains("DFW"))

        1 * endpointService.getBaseUrlsByServiceName(_) >> baseUrlListOne

        when: "get US region endpoints"
        def regionListTwo = service.getDefaultRegionsForCloudServersOpenStack()

        then: "assert only US region is returned"
        regionListTwo.size() == 1
        assert(regionListTwo.contains("ORD"))
        assert(!regionListTwo.contains("LON"))
        assert(!regionListTwo.contains("DFW"))

        1 * endpointService.getBaseUrlsByServiceName(_) >>  baseUrlListTwo
    }

    def "getRegionsWithinCloud returns only regions within cloud"() {
        given:
        def regionsInCloud = [
                createRegion("US", "ORD"),
                createRegion("US", "DFW")
        ].asList()

        def regions = [ "ORD", "DFW", "LON" ].asList()
        Set<String> regionNameList = new HashSet<String>(regions)

        identityConfig.staticConfig.getCloudRegion() >> "US"
        cloudRegionService.getRegions(_) >> regionsInCloud

        when:
        def returnedRegions = service.getRegionsWithinCloud(regionNameList)

        then:
        returnedRegions.size() == 2
        assert(!returnedRegions.contains("LON"))
    }

    def "getCloudServersOpenStackRegions returns only Cloud Servers Open Stack Regions"() {
        given:
        def baseUrlList = [
                createBaseUrl("AHH", CLOUD_SERVERS_OPENSTACK),
                createBaseUrl("HHA", "notCloudServersOpenStack"),
                createBaseUrl("HAH", "notCSOSEither")
        ].asList()

        when:
        def returnedList = service.getCloudServersOpenStackRegions(baseUrlList)

        then:
        returnedList.size() == 1
        assert(returnedList.contains("AHH"))
        assert(!returnedList.contains("HHA"))
        assert(!returnedList.contains("HAH"))
    }

    def "validateComputeRegionForUser: calls correct services"() {
        given:
        def selectedRegion = "ORD"

        def domain = entityFactory.createDomain().with {
            it.type = DomainType.RACKSPACE_CLOUD_UK
            it
        }
        def user = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it
        }

        CloudBaseUrl cloudBaseUrl = entityFactory.createCloudBaseUrl().with {
            it.serviceName = CLOUD_SERVERS_OPENSTACK
            it.region = selectedRegion
            it
        }
        OpenstackEndpoint oe = entityFactory.createOpenstackEndpoint().with {
            it.baseUrls = [cloudBaseUrl]
            it
        }

        when: "user has access to compute region"
        service.validateComputeRegionForUser(selectedRegion, user)

        then:
        noExceptionThrown()

        1 * scopeAccessService.getOpenstackEndpointsForUser(user) >> [oe]

        when: "user has no access to compute region - fallback"
        service.validateComputeRegionForUser(selectedRegion, user)

        then:
        noExceptionThrown()

        1 * scopeAccessService.getOpenstackEndpointsForUser(user) >> []
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * userService.inferCloudBasedOnDomainType(domain.getType()) >> CloudRegion.US.getName()
        1 * endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK) >> [cloudBaseUrl]
        1 * cloudRegionService.getRegions(_) >> [entityFactory.createRegion(selectedRegion, CloudRegion.US.getName())]
    }

    def "validateComputeRegionForUser: error check"() {
        given:
        def selectedRegion = "ORD"

        def domain = entityFactory.createDomain().with {
            it.type = DomainType.RACKSPACE_CLOUD_UK
            it
        }
        def user = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it
        }

        CloudBaseUrl cloudBaseUrl = entityFactory.createCloudBaseUrl().with {
            it.serviceName = CLOUD_SERVERS_OPENSTACK
            it.region = "LON"
            it
        }
        OpenstackEndpoint oe = entityFactory.createOpenstackEndpoint().with {
            it.baseUrls = [cloudBaseUrl]
            it
        }

        when: "user has access to compute region"
        service.validateComputeRegionForUser(selectedRegion, user)

        then:
        thrown(BadRequestException)

        1 * scopeAccessService.getOpenstackEndpointsForUser(user) >> [oe]

        when: "user has no access to compute region - fallback"
        service.validateComputeRegionForUser(selectedRegion, user)

        then:
        thrown(BadRequestException)

        1 * scopeAccessService.getOpenstackEndpointsForUser(user) >> []
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * userService.inferCloudBasedOnDomainType(domain.getType()) >> CloudRegion.US.getName()
        1 * endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK) >> [cloudBaseUrl]
        1 * cloudRegionService.getRegions(_) >> [entityFactory.createRegion(selectedRegion, CloudRegion.UK.getName())]
    }

    def createApplication() {
        new Application().with  {
            it.useForDefaultRegion = true
            return it
        }
    }

    def createBaseUrl(region, service) {
        new CloudBaseUrl().with {
            it.region = region
            it.serviceName = service
            return it
        }
    }

    def createRegion(cloud, name) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            return it
        }
    }
}
