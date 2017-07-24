package com.rackspace.idm.domain.entity

import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
class DomainTest extends RootServiceTest{
    @Shared Domain domain1
    @Shared Domain domain2

    def setupSpec(){
        domain1 = new Domain()
        domain2 = new Domain()
    }

    def "Two different domains should not be equal" () {
        given:
        domain1.with {
            it.domainId = "1"
            it.name = "domain1"
            it.enabled = true
        }
        domain2.with {
            it.domainId = "1"
            it.name = "domain1"
            it.enabled = false
        }

        when:
        def result = domain1.equals(domain2)

        then:
        result == false
    }

    def "Two domains should be equal" () {
        given:
        domain1.with {
            it.domainId = "1"
            it.name = "domain1"
            it.enabled = true
        }
        domain2.with {
            it.domainId = "1"
            it.name = "domain1"
            it.enabled = true
        }

        when:
        def result = domain1.equals(domain2)

        then:
        result == true
    }

    def "Domain tenantId setter should copy array argument"() {
        given:
        String[] tenantIds = new String[1]
        tenantIds[0] = "tenantId"
        def domain = new Domain()

        when:
        domain.setTenantIds(tenantIds)

        then:
        def retrievedTenantIds = domain.getTenantIds()
        retrievedTenantIds.size() == 1
        domain.getTenantIds()[0] == "tenantId"
        ! domain.getTenantIds().is(tenantIds)
    }

    def "Domain tenantIds equals null should not throw a Null pointer exception"() {
        given:
        String[] tenantIds = null;
        def domain = new Domain()

        when:
        domain.setTenantIds(tenantIds)

        then:
        noExceptionThrown()
    }

    def "Domain - set tenantIds to null"() {
        given:
        def domain = new Domain()
        String[] tenantIds = new String[1]
        tenantIds[0] = "tenantId"

        when:
        domain.setTenantIds(null)

        then:
        domain.getTenantIds() == null
    }
}

