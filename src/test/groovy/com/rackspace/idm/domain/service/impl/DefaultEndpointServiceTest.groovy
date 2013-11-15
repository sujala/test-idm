package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import testHelpers.RootServiceTest

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
}
