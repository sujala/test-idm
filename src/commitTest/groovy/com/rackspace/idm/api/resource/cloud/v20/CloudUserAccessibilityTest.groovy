package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.Domains
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

    def "Remove duplicate domains" () {
        given:
        Domain domain = new Domain().with {
            it.domainId = "1"
            it.name = "domain"
            it.enabled = true
            return it
        }

        Domain domain2 = new Domain().with {
            it.domainId = "1"
            return it
        }

        Domains domains = new Domains()
        domains.domain = [domain, domain2].asList()

        when:
        Domains result = this.accessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 1
    }

    def "Adds all different domains" () {
        given:
        Domain domain = new Domain().with {
            it.domainId = "1"
            it.name = "domain"
            it.enabled = true
            return it
        }

        Domain domain2 = new Domain().with {
            it.domainId = "2"
            return it
        }

        Domains domains = new Domains()
        domains.domain = [domain, domain2].asList()

        when:
        Domains result = this.accessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 2
    }

    def "Does not add domains" () {
        given:
        Domains domains = new Domains()
        domains.domain = [].asList()

        when:
        Domains result = this.accessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 0
    }

    def "Does not add domains - null list" () {
        given:
        Domains domains = new Domains()
        domains.domain = null

        when:
        Domains result = this.accessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 0
    }

    def "Does not add domains - null" () {
        when:
        Domains result = this.accessibility.removeDuplicateDomains(null)

        then:
        result.domain.size() == 0
    }

    def setupMocks() {
        mockConfiguration(accessibility)
        mockTenantService(accessibility)
        mockDomainService(accessibility)
        mockAuthorizationService(accessibility)
        mockUserService(accessibility)
    }
}
