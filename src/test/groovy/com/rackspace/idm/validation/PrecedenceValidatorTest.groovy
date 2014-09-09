package com.rackspace.idm.validation

import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.ForbiddenException
import spock.lang.Shared
import spock.lang.Unroll
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
        def exceptionThrown = false
        try {
            service.compareWeights(first, second)
        }
        catch (ForbiddenException) {
            exceptionThrown = true
        }

        then:
        exceptionThrown == expectedResult

        where:
        first | second || expectedResult
        100   | 500    || false
        500   | 100    || true
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

    @Unroll
    def "hasGreaterAccess returns true if caller role has higher access level than target role"() {
        expect:
        service.hasGreaterAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("500", 500)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("1", 1)
    }

    @Unroll
    def "hasGreaterAccess returns false if caller role has less than or equal access level than target role"() {
        expect:
        !service.hasGreaterAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("50", 50)
        entityFactory.createClientRole("1", 1)   | entityFactory.createClientRole("1", 1)
        entityFactory.createClientRole("1", 1)   | entityFactory.createClientRole("-1", -1)
    }

    @Unroll
    def "hasGreaterOrEqualAccess returns true if caller role has same or higher access level than target role"() {
        expect:
        service.hasGreaterOrEqualAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("500", 500)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("0", 0)
    }

    @Unroll
    def "hasGreaterOrEqualAccess returns false if caller role has lower access level than target role"() {
        expect:
        !service.hasGreaterOrEqualAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("50", 50)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("-5", -5)
    }

    @Unroll
    def "verifyHasGreaterAccess does NOT throw Forbidden exception if caller role has higher access level than target role"() {
        expect:
        service.verifyHasGreaterAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("500", 500)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("1", 1)
    }

    @Unroll
    def "verifyHasGreaterAccess throws ForbiddenException if caller role has less than or equal access level than target role"() {
        when:
        !service.verifyHasGreaterAccess(first, second)

        then:
        thrown(ForbiddenException)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("50", 50)
        entityFactory.createClientRole("1", 1)   | entityFactory.createClientRole("1", 1)
        entityFactory.createClientRole("1", 1)   | entityFactory.createClientRole("-1", -1)
    }

    @Unroll
    def "verifyHasGreaterOrEqualAccess does NOT throw Forbidden exception if caller role has same or higher access level than target role"() {
        expect:
        service.verifyHasGreaterOrEqualAccess(first, second)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("500", 500)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("0", 0)
    }

    @Unroll
    def "verifyHasGreaterOrEqualAccess throws ForbiddenException if caller role has lower access level than target role"() {
        when:
        !service.verifyHasGreaterOrEqualAccess(first, second)

        then:
        thrown(ForbiddenException)

        where:
        first | second
        entityFactory.createClientRole("100", 100)   | entityFactory.createClientRole("50", 50)
        entityFactory.createClientRole("0", 0)   | entityFactory.createClientRole("-5", -5)
    }

    def mockRoleService(service) {
        mockRoleService = Mock()
        service.roleService = mockRoleService;
    }
}
