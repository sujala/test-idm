package com.rackspace.idm.api.resource.user

import spock.lang.Shared
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import testHelpers.RootServiceTest

class UserGlobalRoleResourceTest extends RootServiceTest {


    @Shared UserGlobalRoleResource service

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def AUTH_TOKEN = "authToken"
    @Shared def USER_ID = "userId"
    @Shared def ROLE_ID = "roleId"
    @Shared def TENANT_ID = "tenantId"

    def setupSpec() {
        service = new UserGlobalRoleResource()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        mockScopeAccessService(service)
        mockUserService(service)
        mockApplicationService(service)
        mockAuthorizationService(service)
        mockTenantService(service)
        mockPrecedenceValidator(service)
        mockConfiguration(service)
    }

    def "grantGlobalRoleToUser verifies role exists"() {
        given:
        allowUserAccess()

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * applicationService.getClientRoleById(ROLE_ID) >> null

        then:
        thrown(BadRequestException)
    }

    def "grantGlobalRoleToUser gets user to add role to"() {
        given:
        allowUserAccess()

        def role = entityFactory.createClientRole()
        def user = entityFactory.createUser()

        applicationService.getClientRoleById(ROLE_ID) >> role

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * userService.loadUser(USER_ID) >> user
    }

    def "grantGlobalRoleToUser verifies caller precedence over user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def role = entityFactory.createClientRole()

        applicationService.getClientRoleById(ROLE_ID) >> role
        userService.loadUser(USER_ID) >> user

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * userService.getUserByAuthToken(AUTH_TOKEN) >> caller
        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        then:
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, role)
    }

    def "grantGlobalRoleToUser prevents a user having multiple identityRoles"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def role = entityFactory.createClientRole("identity:role")

        applicationService.getClientRoleById(ROLE_ID) >> role
        userService.loadUser(USER_ID) >> user
        userService.getUserByAuthToken(AUTH_TOKEN) >> caller

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * applicationService.getUserIdentityRole(user, _, _) >> entityFactory.createClientRole()
        then:
        thrown(BadRequestException)
    }

    def "grantGlobalRoleToUser throws BadRequest if adding an identity:* role and user has a global role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def role = entityFactory.createClientRole("identity:role")

        applicationService.getClientRoleById(ROLE_ID) >> role
        userService.loadUser(USER_ID) >> user
        userService.getUserByAuthToken(AUTH_TOKEN) >> caller
        applicationService.getUserIdentityRole(user, _, _) >> null
        config.getString(_) >> "identity:role"

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * tenantService.getGlobalRolesForUser(user)  >> [ entityFactory.createTenantRole() ].asList()
        then:
        thrown(BadRequestException)
    }

    def "grantGlobalRoleToUser adds role to user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def role = entityFactory.createClientRole()

        applicationService.getClientRoleById(ROLE_ID) >> role
        userService.loadUser(USER_ID) >> user
        userService.getUserByAuthToken(AUTH_TOKEN) >> caller
        config.getString(_) >> "identity:role"

        when:
        service.grantGlobalRoleToUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * tenantService.addTenantRoleToUser(_, _) >> { User arg1, TenantRole arg2 ->
            assert(arg1.is(user))
            assert(arg2.name.equals(role.getName()))
            assert(arg2.userId.equals(USER_ID))
            assert(arg2.roleRsId.equals(role.getId()))
        }
    }

    def "deleteGlobalRoleFromUser gets users"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        tenantService.getTenantRoleForUserById(_, _) >> entityFactory.createTenantRole("name")
        userService.getUserByAuthToken(AUTH_TOKEN) >> entityFactory.createUser()

        when:
        service.deleteGlobalRoleFromUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * userService.loadUser(USER_ID) >> user
    }

    def "deleteGlobalRoleFromUser verifies user has role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()

        userService.loadUser(USER_ID) >> user

        when:
        service.deleteGlobalRoleFromUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * tenantService.getTenantRoleForUserById(user, ROLE_ID) >> null
        then:
        thrown(NotFoundException)
    }

    def "deleteGlobalRoleFromUser verifies callers precedence"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenantRoleForUserById(user, ROLE_ID) >> tenantRole

        when:
        service.deleteGlobalRoleFromUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * userService.getUserByAuthToken(AUTH_TOKEN) >> caller
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, tenantRole)
    }

    def "deleteGlobalRoleFromUser prevents user from removing their own identity:* role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = user
        def tenantRole = entityFactory.createTenantRole("identity:role")

        userService.loadUser(USER_ID) >> user
        userService.getUserByAuthToken(AUTH_TOKEN) >> caller
        tenantService.getTenantRoleForUserById(user, ROLE_ID) >> tenantRole

        when:
        service.deleteGlobalRoleFromUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        thrown(BadRequestException)
    }

    def "deleteGlobalRoleFromUser deletes role from user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenantRoleForUserById(user, ROLE_ID) >> tenantRole
        userService.getUserByAuthToken(AUTH_TOKEN) >> caller

        when:
        service.deleteGlobalRoleFromUser(AUTH_TOKEN, USER_ID, ROLE_ID)

        then:
        1 * tenantService.deleteTenantRoleForUser(user, tenantRole)
    }

    def "grantTenantRoleToUser gets user and tenant and verifies tenant"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * userService.loadUser(USER_ID) >> user
        1 * tenantService.getTenant(TENANT_ID) >> null
        then:
        thrown(BadRequestException)
    }

    def "grantTenantRoleToUser verifies tenantRole"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getClientRoleById(ROLE_ID) >> null
        then:
        thrown(BadRequestException)
    }

    def "grantTenantRoleToUser verifies caller precedence"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()
        def cRole = entityFactory.createClientRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant
        applicationService.getClientRoleById(ROLE_ID) >> cRole

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * userService.getUserByAuthToken(AUTH_TOKEN)
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, _)
    }

    def "grantTenantRoleToUser adds tenantRole to user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()
        def cRole = entityFactory.createClientRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant
        applicationService.getClientRoleById(ROLE_ID) >> cRole
        userService.getUserByAuthToken(AUTH_TOKEN)

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * tenantService.addTenantRoleToUser(user, _)
    }

    def "checkAndGetTenantRole verifies that user exists"() {
        given:
        allowUserAccess()

        when:
        service.checkAndGetTenantRole(TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getClientRoleById(ROLE_ID) >> null
        then:
        thrown(BadRequestException)
    }

    def "checkAndGetTenantRole sets tenantRole information from clientRole"() {
        given:
        allowUserAccess()

        def cRole = entityFactory.createClientRole("roleName").with {
            it.clientId = "clientId"
            it.id = "id"
            return it
        }
        applicationService.getClientRoleById(ROLE_ID) >> cRole

        when:
        def tRole = service.checkAndGetTenantRole(TENANT_ID, ROLE_ID)

        then:
        tRole.getClientId().equals(cRole.getClientId())
        tRole.getRoleRsId().equals(cRole.getId())
        tRole.getName().equals(cRole.getName())
        tRole.getTenantIds()[0].equals(TENANT_ID)
    }

    def "getIdentityRoleNames gets a list of known identityRole names"() {
        when:
        service.getIdentityRoleNames()

        then:
        1 * config.getString("cloudAuth.userRole") >> ""
        1 * config.getString("cloudAuth.userAdminRole") >> ""
        1 * config.getString("cloudAuth.adminRole") >> ""
        1 * config.getString("cloudAuth.serviceAdminRole") >> ""
    }

    def "isIdentityRole compares name to known identityRole names"() {
        given:
        config.getString("cloudAuth.userRole") >> "1"
        config.getString("cloudAuth.userAdminRole") >> "2"
        config.getString("cloudAuth.adminRole") >> "3"
        config.getString("cloudAuth.serviceAdminRole") >> "4"

        when:
        def result1= service.isIdentityRole("1")
        def result2 = service.isIdentityRole("2")
        def result3 = service.isIdentityRole("3")
        def result4 = service.isIdentityRole("4")
        def result5 = service.isIdentityRole("5")

        then:
        result1 == true
        result2 == true
        result3 == true
        result4 == true
        result5 == false
    }
}
