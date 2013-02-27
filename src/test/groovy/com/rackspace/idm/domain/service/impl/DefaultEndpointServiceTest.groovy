package com.rackspace.idm.domain.service.impl

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

    def "dao is used to get OpenStackEndpoint for a tenant"() {
        when:
        service.getOpenStackEndpointForTenant(entityFactory.createTenant())

        then:
        1 * endpointDao.getOpenstackEndpointsForTenant(_)
    }
}
