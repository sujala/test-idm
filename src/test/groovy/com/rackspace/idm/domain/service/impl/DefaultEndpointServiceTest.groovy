package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.Tenant
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
}
