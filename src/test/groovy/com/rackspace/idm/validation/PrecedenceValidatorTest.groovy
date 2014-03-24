package com.rackspace.idm.validation

import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.ForbiddenException
import spock.lang.Shared
import testHelpers.RootServiceTest

class PrecedenceValidatorTest extends RootServiceTest {

    @Shared PrecedenceValidator service

    @Shared RoleService mockRoleService
    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    def setupSpec() {
        service = new PrecedenceValidator()
        random = ("$randomness").replace('-', "")
    }
    
    def setup() {
        mockApplicationService(service)
        mockConfiguration(service)
        mockRoleService(service)
    }

    def "compareWeights throws forbidden exception if caller weight is greater than role weight"() {
        when:
        service.compareWeights(100, 500)

        then:
        notThrown(ForbiddenException)

        when:
        service.compareWeights(500, 100)
        
        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedenceForAssignment - throw ForbiddenException if caller has no Identity Role"() {
        given:
        def user = entityFactory.createUser()
        def role = entityFactory.createClientRole()

        when:
        service.verifyCallerRolePrecedenceForAssignment(user, role)

        then:
        1 * applicationService.getUserIdentityRole(_) >> null

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedenceForAssignment - gets ClientRole from tenantRole"() {
        given:
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole()
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "roleRsId"
            return it
        }

        when:
        service.verifyCallerRolePrecedenceForAssignment(user, tenantRole)

        then:
        1 * applicationService.getClientRoleById("roleRsId") >> clientRole

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerPrecedenceOverUser gets caller and users identity roles"() {
        given:
        def user = entityFactory.createUser("user", "userId1", "domainId", "region")
        def caller = entityFactory.createUser("caller", "userId2", "domainId", "region")
        def userRole = entityFactory.createClientRole()
        def callerRole = entityFactory.createClientRole(100)

        when:
        service.verifyCallerPrecedenceOverUser(caller, user)

        then:
        1 * applicationService.getUserIdentityRole(caller) >> callerRole
        1 * applicationService.getUserIdentityRole(user) >> userRole
    }

    def "verifyCallerPrecedenceOverUser throws ForbiddenException if caller does not have identity role"() {
        given:
        def user = entityFactory.createUser("user", "userId1", "domainId", "region")
        def caller = entityFactory.createUser("caller", "userId2", "domainId", "region")
        def userRole = entityFactory.createClientRole()
        def callerRole = null

        when:
        service.verifyCallerPrecedenceOverUser(caller, user)

        then:
        1 * applicationService.getUserIdentityRole(caller) >> callerRole
        1 * applicationService.getUserIdentityRole(user) >> userRole

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedence transforms tenantRole to clientRole"() {
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "clientRoleId"
            return it
        }
        def clientRole = entityFactory.createClientRole()

        when:
        service.verifyCallerRolePrecedence(user, tenantRole)

        then:
        1 * applicationService.getClientRoleById("clientRoleId") >> clientRole

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedence gets callers Identity Role and throws ForbiddenException if caller does not have one"() {
        given:
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole()

        when:
        service.verifyCallerRolePrecedence(user, clientRole)

        then:
        1 * applicationService.getUserIdentityRole(user) >> null

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedenceForAssignment"() {
        given:
        def caller = entityFactory.createUser("caller", "userId2", "domainId", "region")
        def callerRole = entityFactory.createClientRole(100)
        def role = entityFactory.createClientRole(200)

        and:
        applicationService.getUserIdentityRole(caller) >> callerRole
        mockRoleService.getRoleByName("roleName") >> role

        when:
        service.verifyCallerRolePrecedenceForAssignment(caller, ["roleName"])

        then:
        noExceptionThrown()
    }

    def "verifyCallerRolePrecedenceForAssignment throws exception when callers role can not be determined"() {
        given:
        def caller = entityFactory.createUser("caller", "userId2", "domainId", "region")
        def role = entityFactory.createClientRole(200)

        and:
        applicationService.getUserIdentityRole(caller) >> null
        mockRoleService.getRoleByName("roleName") >> role

        when:
        service.verifyCallerRolePrecedenceForAssignment(caller, ["roleName"])

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedenceForAssignment throws exception when callers weight is > role weight"() {
        given:
        def caller = entityFactory.createUser("caller", "userId2", "domainId", "region")
        def callerRole = entityFactory.createClientRole(200)
        def role = entityFactory.createClientRole(100)

        and:
        applicationService.getUserIdentityRole(caller) >> callerRole
        mockRoleService.getRoleByName("roleName") >> role

        when:
        service.verifyCallerRolePrecedenceForAssignment(caller, ["roleName"])

        then:
        thrown(ForbiddenException)
    }

    def "verifyRolePrecedenceForAssignment"() {
        given:
        def clientRole = entityFactory.createClientRole(100)
        def role = entityFactory.createClientRole(200)

        and:
        mockRoleService.getRoleByName("roleName") >> role

        when:
        service.verifyRolePrecedenceForAssignment(clientRole, ["roleName"])

        then:
        noExceptionThrown()
    }

    def "verifyRolePrecedenceForAssignment throws exception when role weight > any role weight in list"() {
        given:
        def clientRole = entityFactory.createClientRole(200)
        def role = entityFactory.createClientRole(100)

        and:
        mockRoleService.getRoleByName("roleName") >> role

        when:
        service.verifyRolePrecedenceForAssignment(clientRole, ["roleName"])

        then:
        thrown(ForbiddenException)
    }

    def mockRoleService(service) {
        mockRoleService = Mock()
        service.roleService = mockRoleService;
    }
}
