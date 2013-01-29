package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootServiceTest

class CloudDefaultUserAccessibilityTest extends RootServiceTest {
    @Shared CloudDefaultUserAccessibility accessibility

    def setupSpec() {
        accessibility = new CloudDefaultUserAccessibility()
    }

    def setup() {
        setupMocks()
    }

    def "caller has different id and cannot be accessed"() {
        given:
        def caller = entityFactory.createUser("caller", "1", "domain", "region")
        def user = entityFactory.createUser("user", "2", "domain", "region")
        accessibility.caller = caller

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == false
    }

    def "caller has same id and can be accessed"() {
        given:
        def caller = entityFactory.createUser("caller", "1", "domain", "region")
        def user = entityFactory.createUser("user", "1", "domain", "region")
        accessibility.caller = caller

        when:
        def result = accessibility.hasAccess(user)

        then:
        userService.getUserByScopeAccess(_) >> caller
        result == true
    }

    def setupMocks() {
        mockUserService(accessibility)
    }
}
