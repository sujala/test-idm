package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.Domains
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 11:15 AM
 * To change this template use File | Settings | File Templates.
 */
class CloudUserAccessibilityTest extends RootServiceTest{

    @Shared CloudUserAccessibility cloudUserAccessibility


    def setupSpec(){
        cloudUserAccessibility = new CloudUserAccessibility()
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
        Domains result = cloudUserAccessibility.removeDuplicateDomains(domains)

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
        Domains result = cloudUserAccessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 2
    }

    def "Does not add domains" () {
        given:
        Domains domains = new Domains()
        domains.domain = [].asList()

        when:
        Domains result = cloudUserAccessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 0
    }

    def "Does not add domains - null list" () {
        given:
        Domains domains = new Domains()
        domains.domain = null

        when:
        Domains result = cloudUserAccessibility.removeDuplicateDomains(domains)

        then:
        result.domain.size() == 0
    }

    def "Does not add domains - null" () {
        when:
        Domains result = cloudUserAccessibility.removeDuplicateDomains(null)

        then:
        result.domain.size() == 0
    }
}
