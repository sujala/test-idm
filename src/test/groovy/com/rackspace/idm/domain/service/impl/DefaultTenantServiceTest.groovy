package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.exception.NotFoundException
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
        mockTenantRoleDao(service)
        mockApplicationService(service)
        mockUserService(service)
        mockEndpointService(service)
        mockScopeAccessService(service)
    }

    def "calling getTenantsForUserByTenantRoles returns tenants"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant()
        def tenantRole = entityFactory.createTenantRole().with { it.tenantIds = [ "tenantId" ]; return it }
        def tenantRoles = [ tenantRole ].asList()

        when:
        def tenants = service.getTenantsForUserByTenantRoles(user)

        then:
        tenants == [ tenant ].asList()
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
        def tenantRole = entityFactory.createTenantRole("tenantName").with { it.tenantIds = [ "tenantId" ]; return it }
        def tenantRoles = [ tenantRole ].asList()
        def tenantWithMatchingId = entityFactory.createTenant("tenantId", "noMatch")
        def tenantWithMatchingName = entityFactory.createTenant("notTenantId", "match")
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
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()

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

    def "getRoleDetails uses ApplicationService to retrieve client role connected to tenant role"() {
        given:
        def roles = [ entityFactory.createTenantRole() ].asList()

        when:
        service.getRoleDetails(roles)

        then:
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
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

    def "if scope access for tenant roles for scopeAccess with null scopeAccess returns IllegalState" () {
        when:
        service.getTenantRolesForScopeAccess(null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null user returns IllegalState" () {
        given:
        def application = entityFactory.createApplication()

        when:
        service.deleteTenantRoleForApplication(application, null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null application returns IllegalState" () {
        given:
        def tenantRole = entityFactory.createTenantRole()

        when:
        service.deleteTenantRoleForApplication(null, tenantRole)

        then:
        thrown(IllegalStateException)
    }

    def "getTenantRolesForClientRole uses DAO to get all TenantRoles for ClientRole"() {
        given:
        def role = entityFactory.createClientRole()

        when:
        service.getTenantRolesForClientRole(role)

        then:
        1 * tenantDao.getAllTenantRolesForClientRole(role)
    }

    def "deleteTenantRole uses DAO to delete role"() {
        given:
        def role = entityFactory.createTenantRole()

        when:
        service.deleteTenantRole(role)

        then:
        1 * tenantDao.deleteTenantRole(role)
    }

    def "getTenantRoleForUser uses DAO to get tenantRole for user"() {
        given:
        def list = [].asList()
        def user = entityFactory.createUser()

        when:
        service.getTenantRoleForUser(user, list)

        then:
        1 * tenantRoleDao.getTenantRoleForUser(user, list)
    }

    def "getIdsForUsersWithTenantRole calls DAO to retrieve context object"() {
        when:
        service.getIdsForUsersWithTenantRole("roleId", 0, 25)

        then:
        1 * tenantDao.getIdsForUsersWithTenantRole("roleId", 0, 25)
    }

    def "addTenantRoleToUser verifies that user is not null"() {
        when:
        service.addTenantRoleToUser(null, entityFactory.createTenantRole())

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRoleToUser verifies that role is not null"() {
        when:
        service.addTenantRoleToUser(entityFactory.createUser(), null)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole verifies that user has uniqueId"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        user.uniqueId = ""

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole uses ApplicationService to retrieve application for role and verifies it exists"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * applicationService.getById(_) >> null
        then:
        thrown(NotFoundException)
    }

    def "addTenantRole uses ApplicationService to retrieve application role linked to tenantrole and verifies it exists"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        def application = entityFactory.createApplication()

        applicationService.getById(_) >> application

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * applicationService.getClientRoleByClientIdAndRoleName(_, _) >> null
        then:
        thrown(NotFoundException)
    }

    def "addTenantRole uses DAO to add tenant role to user"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        def application = entityFactory.createApplication()
        def cRole = entityFactory.createClientRole()

        applicationService.getById(_) >> application
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        tenantRoleDao.addTenantRoleToUser(user, tenantRole)
    }
}
