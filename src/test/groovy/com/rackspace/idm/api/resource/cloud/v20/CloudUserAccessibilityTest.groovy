package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.ObjectFactory
import spock.lang.Shared
import testHelpers.RootServiceTest

class CloudUserAccessibilityTest extends RootServiceTest {

    @Shared CloudUserAccessibility accessibility

    def setupSpec() {
        accessibility = new CloudUserAccessibility()
        accessibility.objFactory = new ObjectFactory()
    }

    def setup() {
        setupMocks()
    }

    def "userContainsRole checks if the user has the role - does not"() {
        given:
        def user = entityFactory.createUser()
        def tenantRoles = [entityFactory.createTenantRole()].asList()

        when:
        def result = accessibility.userContainsRole(tenantRoles, "role")

        then:
        result == false
    }

    def "userContainsRole checks if the user has the role - does"() {
        given:
        def user = entityFactory.createUser()
        def tenantRoles = [entityFactory.createTenantRole()].asList()

        when:
        def result = accessibility.userContainsRole(tenantRoles, "name")

        then:
        result == true
    }

    def setupMocks() {
        mockConfiguration(accessibility)
        mockTenantService(accessibility)
        mockDomainService(accessibility)
        mockAuthorizationService(accessibility)
        mockUserService(accessibility)
    }
}
