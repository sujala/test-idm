package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.ApplicationService
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.Region

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/7/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultRegionServiceTest extends Specification {

    @Autowired DefaultRegionService service
    @Autowired Configuration config
    @Shared EndpointService endpointService
    @Shared ScopeAccessService scopeAccessService
    @Shared ApplicationService applicationService
    @Shared DefaultCloudRegionService cloudRegionService

    def "getDefaultRegions returns only regions within cloud region"() {
        given:
        setupMocks()

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
        setupMocks()

        def regionsInCloud = [
                createRegion("US", "ORD"),
                createRegion("US", "DFW")
        ].asList()

        def regions = [ "ORD", "DFW", "LON" ].asList()
        Set<String> regionNameList = new HashSet<String>(regions)

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
                createBaseUrl("AHH", service.CLOUD_SERVERS_OPENSTACK),
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

    def setupMocks() {
        endpointService = Mock()
        scopeAccessService = Mock()
        applicationService = Mock()
        cloudRegionService = Mock()

        service.endpointService = endpointService
        service.scopeAccessService = scopeAccessService
        service.applicationService = applicationService
        service.defaultCloudRegionService = cloudRegionService
    }
}
