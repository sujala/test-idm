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

    def "addCallerTenantRolesToUser gets callers tenant roles by userObject"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        service.addCallerTenantRolesToUser(caller, user)

        then:
        1 * tenantDao.getTenantRolesForUser(caller) >> [].asList()
    }

    def "addCallerTenantRolesToUser verifies that role to be added is not an identity:* role"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()
        def tenantRoleList = [ entityFactory.createTenantRole() ].asList()

        tenantDao.getTenantRolesForUser(caller) >> tenantRoleList
        applicationDao.getClientRoleById(_) >> entityFactory.createClientRole()
        applicationDao.getClientByClientId(_) >> entityFactory.createApplication()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()

        when:
        service.addCallerTenantRolesToUser(caller, user)

        then:
        1 * config.getString("cloudAuth.adminRole") >> ""
        1 * config.getString("cloudAuth.serviceAdminRole") >> ""
        1 * config.getString("cloudAuth.userAdminRole") >> ""
        1 * config.getString("cloudAuth.userRole") >> ""

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, _)
    }

    def "doesUserContainTenantRole returns false if user does not contain the role"() {
        given:
        def user = entityFactory.createUser()
        def roleId = "roleId"

        when:
        def result = service.doesUserContainTenantRole(user, roleId)

        then:
        result == false
        1 * tenantRoleDao.getTenantRoleForUser(user, roleId) >> null
    }

    def "doesUserContainTenantRole returns true if user does contain the role"() {
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole()
        def roleId = "roleId"

        when:
        def result = service.doesUserContainTenantRole(user, roleId)

        then:
        result == true
        1 * tenantRoleDao.getTenantRoleForUser(user, roleId) >> tenantRole
    }
}

