package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.CloudBaseUrl
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

        mockApplicationService(service)
        mockCloudRegionService(service)
        mockConfiguration(service)
        mockDomainService(service)
        mockEndpointService(service)
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

        endpointService.getBaseUrlsByServiceName(_) >>> [
                baseUrlListOne,
                baseUrlListTwo
        ]

        config.getString("cloud.region") >> "US"
        cloudRegionService.getRegions(_) >> regionsInCloud

        when:
        def regionListOne = service.getDefaultRegionsForCloudServersOpenStack()
        def regionListTwo = service.getDefaultRegionsForCloudServersOpenStack()

        then:
        regionListOne.size() == 2
        assert(!regionListOne.contains("LON"))
        regionListTwo.size() == 1
        regionListTwo.toArray()[0].equals("ORD")
    }

    def "getRegionsWithinCloud returns only regions within cloud"() {
        given:
        def regionsInCloud = [
                createRegion("US", "ORD"),
                createRegion("US", "DFW")
        ].asList()

        def regions = [ "ORD", "DFW", "LON" ].asList()
        Set<String> regionNameList = new HashSet<String>(regions)

        config.getString("cloud.region") >> "US"
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
        assert(!returnedList.contains("HHA"))
        assert(!returnedList.contains("HAH"))
    }

    def "validateComputeRegionForUser: calls correct services"() {
        given:
        def selectedRegion = "ORD"

        def domain = entityFactory.createDomain().with {
            it.type = GlobalConstants.DOMAIN_TYPE_RACKSPACE_CLOUD_US
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
        1 * userService.inferCloudBasedOnDomainType(domain.getType()) >> GlobalConstants.CLOUD_REGION.US.toString()
        1 * endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK) >> [cloudBaseUrl]
        1 * cloudRegionService.getRegions(_) >> [entityFactory.createRegion(selectedRegion, GlobalConstants.CLOUD_REGION.US.toString())]
    }

    def "validateComputeRegionForUser: error check"() {
        given:
        def selectedRegion = "ORD"

        def domain = entityFactory.createDomain().with {
            it.type = GlobalConstants.DOMAIN_TYPE_RACKSPACE_CLOUD_UK
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
        1 * userService.inferCloudBasedOnDomainType(domain.getType()) >> GlobalConstants.CLOUD_REGION.US.toString()
        1 * endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK) >> [cloudBaseUrl]
        1 * cloudRegionService.getRegions(_) >> [entityFactory.createRegion(selectedRegion, GlobalConstants.CLOUD_REGION.UK.toString())]
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
