package com.rackspace.idm.domain.service.impl

import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultTenantServiceTest extends RootServiceTest {
    @Shared DefaultTenantService service

    def setupSpec() {
        service = new DefaultTenantService()
    }

    def setup() {
        mockConfiguration(service)
        mockDomainService(service)
        mockTenantDao(service)
        mockApplicationDao(service)
        mockUserDao(service)
        mockEndpointDao(service)
        mockScopeAccessDao(service)
        mockTenantRoleDao(service)
    }

    def "calling getTenantsForUserByTenantRoles returns tenants"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant()
        def tenantRole = entityFactory.createTenantRole(null, null, null, null, "tenantId")
        def tenantRoles = [tenantRole].asList()

        when:
        def tenants = service.getTenantsForUserByTenantRoles(user)

        then:
        tenants == [tenant].asList()
        1 * tenantRoleDao.getTenantRolesForUser(_) >> tenantRoles
        1 * tenantDao.getTenant(_) >> tenant
    }

    def "hasTenantAccess returns false if user is null or tenantId is blank"() {
        expect:
        result == false

        where:
        result << [
                service.hasTenantAccess(null, "tenantId"),
                service.hasTenantAccess(entityFactory.createUser(), ""),
                service.hasTenantAccess(entityFactory.createUser(), null)
        ]
    }

    def "hasTenantAccess returns true if it contains tenant; false otherwise"() {
        given:
        def tenantRoles = [entityFactory.createTenantRole(null, null, null, null, "tenantId")].asList()
        def tenantWithMatchingId = entityFactory.createTenant("tenantId", "noMatch", null, null, true)
        def tenantWithMatchingName = entityFactory.createTenant("notTenantId", "match", null, null, true)
        def user = entityFactory.createUser()

        when:
        def result1 = service.hasTenantAccess(user, "tenantId")
        def result2 = service.hasTenantAccess(user, "tenantId")
        def result3 = service.hasTenantAccess(user, "match")

        then:
        3 * tenantRoleDao.getTenantRolesForUser(_) >>> [ [].asList() ] >> tenantRoles
        2 * tenantDao.getTenant(_) >>> [ tenantWithMatchingId, tenantWithMatchingName]

        result1 == false
        result2 == true
        result3 == true
    }
}
