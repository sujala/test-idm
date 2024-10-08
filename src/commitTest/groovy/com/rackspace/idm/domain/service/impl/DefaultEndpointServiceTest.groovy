package com.rackspace.idm.domain.service.impl


import com.rackspace.idm.domain.entity.CloudRegion
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.service.OpenstackType
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static com.rackspace.idm.GlobalConstants.*

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

    def "addGlobalBaseUrls gets global uk baseUrls by baseUrlType when region is 'LON'"() {
        given:
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        def tenant = entityFactory.createTenant()
        def region = "LON"
        def baseUrlType = new OpenstackType("MOSSO")

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUKBaseUrlsByBaseUrlType(_) >> { String baseUrlTypeParam ->
            assert (baseUrlTypeParam == baseUrlType.getName())
            [].asList()
        }
    }

    def "addGlobalBaseUrls gets global us baseUrls by baseUrlType when region is not 'LON'"() {
        given:
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = new OpenstackType("MOSSO")

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(_) >> { String baseUrlTypeParam ->
            assert (baseUrlTypeParam == baseUrlType.getName())
            [].asList()
        }
    }

    def "addGlobalBaseUrls adds baseUrls when they do not exist in list"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl()
        def baseUrls = new HashMap<String, CloudBaseUrl>()

        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = new OpenstackType("MOSSO")

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType.getName()) >> [baseUrl].asList()
        baseUrls.keySet().size() == 1
    }

    def "addGlobalBaseUrls does not add baseUrls when they already exist in list"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl()
        def baseUrls = new HashMap<String, CloudBaseUrl>()
        baseUrls.put(baseUrl.baseUrlId, baseUrl)

        def tenant = entityFactory.createTenant()
        def region = "ORD"
        def baseUrlType = new OpenstackType("MOSSO")

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType.getName()) >> [baseUrl].asList()
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
        def baseUrlType = new OpenstackType("MOSSO")

        when:
        service.addGlobalBaseUrls(baseUrls, tenant, baseUrlType, region)
        def addedBaseUrl = baseUrls.get(baseUrlId)

        then:
        1 * endpointDao.getGlobalUSBaseUrlsByBaseUrlType(baseUrlType.getName()) >> [baseUrl].asList()
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
        identityConfig.getStaticConfig().getCloudRegion() >> cloudRegion
        reloadableConfig.getEndpointDefaultRegionId() >> "DEFAULT"

        expect:
        doesBaseUrlBelongToCloudRegionResult == service.doesBaseUrlBelongToCloudRegion(baseUrl)

        where:
        cloudRegion    |  baseUrlID | baseUrlrsRegion | doesBaseUrlBelongToCloudRegionResult
        CloudRegion.US |  -1000     |  null           | true
        CloudRegion.US |  999       |  null           | true
        CloudRegion.US |  1000      |  null           | false
        CloudRegion.US |  500       |  "DFW"          | true
        CloudRegion.US |  10000     |  "DFW"          | true
        CloudRegion.US |  10000     |   "LON"         | false
        CloudRegion.US |  200       |   "LON"         | false
        CloudRegion.UK |  -1000     |   null          | false
        CloudRegion.UK |  999       |   null          | false
        CloudRegion.UK |  1000      |   null          | true
        CloudRegion.UK |  500       |   "DFW"         | false
        CloudRegion.UK |  10000     |   "DFW"         | false
        CloudRegion.UK |  10000     |   "LON"         | true
        CloudRegion.UK |  200       |   "LON"         | true
        CloudRegion.US |  -1000     |   "DEFAULT"     | true
        CloudRegion.US |  999       |   "DEFAULT"     | true
        CloudRegion.US |  1000      |   "DEFAULT"     | false
        CloudRegion.UK |  -1000     |   "DEFAULT"     | false
        CloudRegion.UK |  999       |   "DEFAULT"     | false
        CloudRegion.UK |  1000      |   "DEFAULT"     | true
    }

    @Unroll
    def "doesBaseUrlBelongToCloudRegion: baseUrl and domain - region = #region, domainType = #domainType"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.type = domainType
            it
        }
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.baseUrlId = "baseUrlId"
            it.region = region
            it
        }

        when:
        def doesBelong = service.doesBaseUrlBelongToCloudRegion(baseUrl, domain)

        then:
        doesBelong == result

        1 * config.getString(DefaultEndpointService.FEATURE_BASEURL_TO_REGION_MAPPING_STRATEGY) >> "rsregion"

        where:
        region | domainType          | result
        "US"   | "RACKSPACE_CLOUD_US"| true
        "LON"  | "RACKSPACE_CLOUD_UK"| true
        "US"   | "RACKSPACE_CLOUD_UK"| false
        "LON"  | "RACKSPACE_CLOUD_US"| false
    }

    def "doesBaseUrlBelongToCloudRegion: test invalid domain scenarios"() {
        given:
        def domain = entityFactory.createDomain()
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.baseUrlId = "baseUrlId"
            it.region = "US"
            it
        }

        when: "domain is null"
        def result = service.doesBaseUrlBelongToCloudRegion(baseUrl, null)

        then:
        !result

        when: "domain's type is null"
        domain.type = null
        result = service.doesBaseUrlBelongToCloudRegion(baseUrl, domain)

        then:
        !result

        when: "domain's type is not 'RACKSPACE_CLOUD_US' or 'RACKSPACE_CLOUD_UK'"
        domain.type = "otherType"
        result = service.doesBaseUrlBelongToCloudRegion(baseUrl, domain)

        then:
        !result
    }

    @Unroll
    def "doesBaseUrlBelongToCloudRegion: only baseUrl - region = #region, cloudRegion = #cloudRegion"() {
        given:
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.baseUrlId = "baseUrlId"
            it.region = region
            it
        }

        when:
        def doesBelong = service.doesBaseUrlBelongToCloudRegion(baseUrl)

        then:
        doesBelong == result

        1 * identityConfig.getStaticConfig().getCloudRegion() >> cloudRegion
        1 * config.getString(DefaultEndpointService.FEATURE_BASEURL_TO_REGION_MAPPING_STRATEGY) >> "rsregion"

        where:
        region | cloudRegion    | result
        "US"   | CloudRegion.US | true
        "LON"  | CloudRegion.UK | true
        "US"   | CloudRegion.UK | false
        "LON"  | CloudRegion.US | false
    }

}
