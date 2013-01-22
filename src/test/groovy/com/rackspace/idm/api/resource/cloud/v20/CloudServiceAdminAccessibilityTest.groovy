package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootServiceTest

class CloudServiceAdminAccessibilityTest extends RootServiceTest {
    @Shared CloudServiceAdminAccessibility accessibility

    def setupSpec() {
        accessibility = new CloudServiceAdminAccessibility()
    }

    def setup() {
        mockConfiguration(accessibility)
        mockTenantService(accessibility)
        config.getString(_) >>> [ "identityRole", "serviceRole" ]
    }

    def "identity admin has access"() {
        given:
        def user = entityFactory.createUser()
        def tenantRoles = entityFactory.createTenantRole().with {
            it.name = "identityRole"
            return it
        }

        when:
        def result = accessibility.hasAccess(user)

        then:
        result == true
        tenantService.getTenantRolesForUser(_) >> [tenantRoles].asList()
    }

    def "service admin has access"() {
        given:
        def user = entityFactory.createUser()
        def tenantRoles = entityFactory.createTenantRole().with {
            it.name = "serviceRole"
            return it
        }

        when:
        def result = accessibility.hasAccess(user)

        then:
        result == true
        tenantService.getTenantRolesForUser(_) >> [tenantRoles].asList()
    }

    def "not service or identity admin has no access"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = accessibility.hasAccess(user)

        then:
        result == false
        tenantService.getTenantRolesForUser(_) >> [].asList()
    }
}
