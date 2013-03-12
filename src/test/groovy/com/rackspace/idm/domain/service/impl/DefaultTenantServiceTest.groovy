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
        tenantRoleDao.getTenantRolesForUser(_) >> [].asList()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()

        when:
        service.addCallerTenantRolesToUser(caller, user)

        then:
        config.getString("cloudAuth.adminRole") >> ""
        config.getString("cloudAuth.serviceAdminRole") >> ""
        config.getString("cloudAuth.userAdminRole") >> ""
        config.getString("cloudAuth.userRole") >> ""
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

    def "deleteTenantRoleForUser deletes tenantRole from subUsers if user is user-admin"() {
        given:
        def roleName = "identity"
        def identityRole = entityFactory.createTenantRole(roleName)
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {it.name = roleName; it.propagate = true; return it}
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subUser", "subUserId", "domainId", "REGION")

        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        applicationService.getClientRoleById(_) >> cRole
        config.getString(_) >> roleName

        when:
        service.deleteTenantRoleForUser(user, role)

        then:
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
        then:
        1 * userService.getSubUsers(user) >> [ subUser ].asList()
        1 * tenantRoleDao.deleteTenantRoleForUser(subUser, role)
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
        config.getString(_) >> ""
        tenantRoleDao.getTenantRolesForUser(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        tenantRoleDao.addTenantRoleToUser(user, tenantRole)
    }

    def "isDefaultuser verifies if user has defaultUser role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2 = entityFactory.createClientRole("roleTwo")
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        config.getString("cloudAuth.userRole") >> roleName
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isDefaultUser(user)

        then:
        result == expected

        where:
        expected | roleName
        false    | "roleThree"
        true     | "roleOne"
    }

    def "isUserAdmin verifies if user has userAdmin role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2 = entityFactory.createClientRole("roleTwo")
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        config.getString("cloudAuth.userAdminRole") >> userAdminRoleName
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isUserAdmin(user)

        then:
        result == isUserAdmin

        where:
        isUserAdmin | userAdminRoleName
        false       | "roleThree"
        true        | "roleOne"
    }

    def "isIdentityAdmin verifies if user has identityAdmin role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2 = entityFactory.createClientRole("roleTwo")
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        config.getString("cloudAuth.adminRole") >> roleName
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isIdentityAdmin(user)

        then:
        result == expected

        where:
        expected | roleName
        false    | "roleThree"
        true     | "roleOne"
    }

    def "isServiceAdmin verifies if user has serviceAdmin role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2 = entityFactory.createClientRole("roleTwo")
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        config.getString("cloudAuth.serviceAdminRole") >> roleName
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isServiceAdmin(user)

        then:
        result == expected

        where:
        expected | roleName
        false    | "roleThree"
        true     | "roleOne"
    }

    def "addTenantRole adds role to subusers if user is user-admin"() {
        given:
        def roleName = "thisRole"
        def role = entityFactory.createClientRole().with {
            it.name = roleName
            it.propagate = true
            return it
        }
        def tenantRole = entityFactory.createTenantRole()
        def identityRole = entityFactory.createTenantRole(roleName)
        def user = entityFactory.createUser("user", "userId", "domainId", "region")
        def subUser = entityFactory.createUser()

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> role
        applicationService.getClientRoleById(_) >> role
        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        userService.getSubUsers(user) >> [ subUser ].asList()
        config.getString("cloudAuth.userAdminRole") >> roleName

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, tenantRole)
        then:
        1 * tenantRoleDao.addTenantRoleToUser(subUser, tenantRole)
    }

    def "Add userId To TenantRole if it does not have it set"() {
        given:
        def tenantRole = entityFactory.createTenantRole()

        when:
        service.addUserIdToTenantRole(tenantRole)

        then:
        tenantRole.getUserId() != null
        tenantRole.getUserId() == "1"
        1 * tenantDao.updateTenantRole(_)

    }

    def "Add userId To TenantRole - sending null"() {
        when:
        service.addUserIdToTenantRole(null)

        then:
        notThrown(Exception)
    }

    def "GET - tenantRoles for tenant"(){
        when:
        def tenantRole = null
        if (useTenantsRole){
            tenantRole = entityFactory.createTenantRole()
            tenantRole.name == null
        }
        def clientRole = null
        if (useClientRole){
            clientRole = entityFactory.createClientRole()
            clientRole.description = "desc"
            clientRole.name = "name"
        }
        if (tenantRole != null){
            tenantDao.getAllTenantRolesForTenant(_) >> [tenantRole].asList()
        }else{
            tenantDao.getAllTenantRolesForTenant(_) >> [].asList()
        }
        applicationService.getClientRoleById(_) >> clientRole
        def result = service.getTenantRolesForTenant(value)

        then:
        if (result != null && result.size() != 0){
            result.get(0).description == desc
            result.get(0).name == name
        }else{
            result == [].asList()
        }

        where:
        value   | desc    | name   | useTenantsRole  | useClientRole
        null    | null    | null   | false           | false
        "1"     | "desc"  | "name" | true            | true

    }
}
