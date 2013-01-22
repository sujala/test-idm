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
}
