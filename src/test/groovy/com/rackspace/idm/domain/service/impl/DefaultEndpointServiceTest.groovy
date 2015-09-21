package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static com.rackspace.idm.GlobalConstants.*

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 2/26/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultEndpointServiceTest extends RootServiceTest {

    @Shared DefaultEndpointService service

    def setupSpec() {
        service = new DefaultEndpointService()
    }

    def setup() {
        mockEndpointDao(service)
        mockConfiguration(service)
        mockIdentityConfig(service)
    }

    def "dao is used to retrieve a list of baseUrls with policyId when calling getBaseUrlsWithPolicyId"() {
        when:
        service.getBaseUrlsWithPolicyId("policyId")

        then:
        1 * endpointDao.getBaseUrlsWithPolicyId("policyId")
    }

    def "deletePolicyToEndpoint throws NotFoundException if policy does not exist"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.policyList = [].asList()
            it
        }
        endpointDao.getBaseUrlById(_) >> baseUrl

        when:
        service.deletePolicyToEndpoint("0", "1")

        then:
        thrown(NotFoundException)
    }

    def "addGlobalBaseUrls gets global uk baseUrls by baseUrlType when region is 'LON'"() {
        given:
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        def tenant = entityFactory.createTenant()
        def region = "LON"
        def baseUrlType = "MOSSO"

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUKBaseUrlsByBaseUrlType(_) >> { String baseUrlTypeParam ->
            assert (baseUrlTypeParam == baseUrlType)
            [].asList()
        }
    }

    def "addGlobalBaseUrls gets global us baseUrls by baseUrlType when region is not 'LON'"() {
        given:
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = "MOSSO"

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(_) >> { String baseUrlTypeParam ->
            assert (baseUrlTypeParam == baseUrlType)
            [].asList()
        }
    }

    def "addGlobalBaseUrls adds baseUrls when they do not exist in list"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl()
        def baseUrls = new HashMap<String, CloudBaseUrl>()

        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = "MOSSO"

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType) >> [baseUrl].asList()
        baseUrls.keySet().size() == 1
    }

    def "addGlobalBaseUrls does not add baseUrls when they already exist in list"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl()
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        baseUrls.put(baseUrl.baseUrlId, baseUrl)

        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = "MOSSO"

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType) >> [baseUrl].asList()
        baseUrls.keySet().size() == 1
    }

    def "addGlobalBaseUrls does not add baseUrl as v1Default"() {
        given:
        def baseUrlId = "1"
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            it.v1Default = true
            it
        }
        def baseUrls = new HashMap<String, CloudBaseUrl>()

        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = "MOSSO"

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)
        def addedBaseUrl = baseUrls.get(baseUrlId)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType) >> [baseUrl].asList()
        addedBaseUrl.v1Default == false
    }

    def "processBaseUrl only concatenates tenant when tenantAlias is enabled"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.publicUrl = "http://public.url"
            it.adminUrl = "http://admin.url"
            it.internalUrl = "http://internal.url"
            it
        }
        def tenant = entityFactory.createTenant()

        when:
        baseUrl.tenantAlias = tenantAlias
        service.processBaseUrl(baseUrl, tenant)

        then:
        baseUrl.publicUrl.contains(tenant.name) == expectedResult
        baseUrl.adminUrl.contains(tenant.name) == expectedResult
        baseUrl.internalUrl.contains(tenant.name) == expectedResult

        where:
        tenantAlias             | expectedResult
        TENANT_ALIAS_PATTERN    | true
        ""                      | false
    }

    def "getOpenstackEndpointsForTenant - gets correct information" () {
        given:
        Tenant tenant = entityFactory.createTenant()
        CloudBaseUrl baseUrl = entityFactory.createCloudBaseUrl()
        baseUrl.adminUrl = "AdminUrl"
        HashSet set = new HashSet()
        set.add(baseUrl.baseUrlId)
        tenant.baseUrlIds = set
        tenant.v1Defaults = set

        when:
        OpenstackEndpoint endpoint = service.getOpenStackEndpointForTenant(tenant)

        then:
        endpoint != null
        1 * endpointDao.getBaseUrlsById(_) >> [baseUrl].asList()
        endpoint.tenantId == tenant.tenantId
        endpoint.tenantName == tenant.name
        endpoint.baseUrls.size() == 1
        endpoint.baseUrls[0].v1Default == true
        endpoint.baseUrls[0].enabled == true
        endpoint.baseUrls[0].openstackType == baseUrl.openstackType
        endpoint.baseUrls[0].adminUrl == baseUrl.adminUrl
    }

    @Unroll("doesBaseUrlBelongToCloudRegion cloudRegion: #cloudRegion, baseUrlID: #baseUrlID, baseUrlrsRegion: #baseUrlrsRegion, expects #doesBaseUrlBelongToCloudRegionResult")
    def "doesBaseUrlBelongToCloudRegion for hybrid strategy"() {
        given:
        CloudBaseUrl baseUrl = entityFactory.createCloudBaseUrl().with( {
            it.baseUrlId = baseUrlID
            it.def = true
            it.enabled = true
            it.global = false
            it.region = baseUrlrsRegion
            return it
        })
        config.getString(DefaultEndpointService.FEATURE_BASEURL_TO_REGION_MAPPING_STRATEGY) >> DefaultEndpointService.BaseUrlToRegionMappingStrategy.HYBRID.getCode()
        config.getString(DefaultEndpointService.CLOUD_REGION_PROP_NAME) >> cloudRegion
        reloadableConfig.getEndpointDefaultRegionId() >> "DEFAULT"

        expect:
        doesBaseUrlBelongToCloudRegionResult == service.doesBaseUrlBelongToCloudRegion(baseUrl)

        where:
        cloudRegion                             |   baseUrlID   |   baseUrlrsRegion     |   doesBaseUrlBelongToCloudRegionResult
        DefaultEndpointService.CLOUD_REGION_US  |   -1000       |   null                |   true
        DefaultEndpointService.CLOUD_REGION_US  |   999         |   null                |   true
        DefaultEndpointService.CLOUD_REGION_US  |   1000        |   null                |   false
        DefaultEndpointService.CLOUD_REGION_US  |   500         |   "DFW"               |   true
        DefaultEndpointService.CLOUD_REGION_US  |   10000       |   "DFW"               |   true
        DefaultEndpointService.CLOUD_REGION_US  |   10000       |   "LON"               |   false
        DefaultEndpointService.CLOUD_REGION_US  |   200         |   "LON"               |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   -1000       |   null                |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   999         |   null                |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   1000        |   null                |   true
        DefaultEndpointService.CLOUD_REGION_UK  |   500         |   "DFW"               |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   10000       |   "DFW"               |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   10000       |   "LON"               |   true
        DefaultEndpointService.CLOUD_REGION_UK  |   200         |   "LON"               |   true
        DefaultEndpointService.CLOUD_REGION_US  |   -1000       |   "DEFAULT"           |   true
        DefaultEndpointService.CLOUD_REGION_US  |   999         |   "DEFAULT"           |   true
        DefaultEndpointService.CLOUD_REGION_US  |   1000        |   "DEFAULT"           |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   -1000       |   "DEFAULT"           |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   999         |   "DEFAULT"           |   false
        DefaultEndpointService.CLOUD_REGION_UK  |   1000        |   "DEFAULT"           |   true
    }
}
