package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootServiceTest

class CloudUserAdminAccessibilityTest extends RootServiceTest {
    @Shared CloudUserAdminAccessibility accessibility

    def setupSpec() {
        accessibility = new CloudUserAdminAccessibility()
    }

    def setup() {
        mockUserService(accessibility)
        mockConfiguration(accessibility)
        mockTenantService(accessibility)

    }

    def "caller has different id and different domain and different role - cannot be accessed"() {
        given:
        def caller = entityFactory.createUser(null, null, "caller", "domain", null, null, null, true)
        def user = entityFactory.createUser(null, null, "user", "domain1", null, null, null, true)
        accessibility.caller = caller

        def tenantRole = entityFactory.createTenantRole(null, null, "role", null, null)
        tenantService.getTenantRolesForUser(_) >> [tenantRole].asList()
        config.getString(_) >> "role"

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == false
    }

    def "caller has different id and different role same domain - cannot be accessed"() {
        given:
        def caller = entityFactory.createUser(null, null, "caller", "domain", null, null, null, true)
        def user = entityFactory.createUser(null, null, "user", "domain", null, null, null, true)
        accessibility.caller = caller

        def tenantRole = entityFactory.createTenantRole(null, null, "role", null, null)
        tenantService.getTenantRolesForUser(_) >> [tenantRole].asList()
        config.getString(_) >> "role2"

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == false
    }

    def "caller has different id and same domain and same role - can be accessed"() {
        given:
        def caller = entityFactory.createUser(null, null, "caller", "domain", null, null, null, true)
        def user = entityFactory.createUser(null, null, "user", "domain", null, null, null, true)
        accessibility.caller = caller

        def tenantRole = entityFactory.createTenantRole(null, null, "role", null, null)
        tenantService.getTenantRolesForUser(_) >> [tenantRole].asList()
        config.getString(_) >> "role"

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == true
    }

    def "caller has same id and can be accessed"() {
        given:
        def caller = entityFactory.createUser(null, null, "user", null, null, null, null, true)
        def user = entityFactory.createUser(null, null, "user", null, null, null, null, true)
        accessibility.caller = caller

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == true
    }
}
