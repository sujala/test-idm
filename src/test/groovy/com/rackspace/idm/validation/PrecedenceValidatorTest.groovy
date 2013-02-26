package com.rackspace.idm.validation

import spock.lang.Shared
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.exception.ForbiddenException
import testHelpers.RootServiceTest

class PrecedenceValidatorTest extends RootServiceTest {

    @Shared PrecedenceValidator service

    @Shared def userAdminRole
    @Shared def adminRole
    @Shared def serviceAdminRole
    @Shared def List<ClientRole> identityRoles
    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    def setupSpec() {
        service = new PrecedenceValidator()
        random = ("$randomness").replace('-', "")
    }
    
    def setup() {
        mockUserService(service)
        mockApplicationService(service)
        mockConfiguration(service)
    }
    
    def "getIdentityRoleNames calls config to retrieve identity roles"() {
        when:
        service.getIdentityRoleNames()
        
        then:
        1 * config.getString("cloudAuth.userRole") >> ""
        1 * config.getString("cloudAuth.userAdminRole") >> ""
        1 * config.getString("cloudAuth.adminRole") >> ""
        1 * config.getString("cloudAuth.serviceAdminRole") >> ""
    }
    
    def "getCloudAuthClientId calls config to retrieve cloudAuthClientId"() {
        when:
        service.getCloudAuthClientId()
        
        then:
        1 * config.getString("cloudAuth.clientId") >> ""
    }
    
    def "compareWeights throws forbidden exception if caller weight is greater than role weight"() {
        when:
        service.compareWeights(100, 500)
        service.compareWeights(500, 100)
        
        then:
        notThrown(ForbiddenException)
        
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
        1 * applicationService.getUserIdentityRole(_, _, _) >> null

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerRolePrecedenceForAssignment - sets callers weight to cloudAuth.special.rsWeight if caller is userAdmin"() {
        given:
        def user = entityFactory.createUser()
        def role = entityFactory.createClientRole("identity:user-admin").with {
            it.rsWeight = 1000
            return it
        }

        applicationService.getUserIdentityRole(_, _, _) >> role

        when:
        service.verifyCallerRolePrecedenceForAssignment(user, role)

        then:
        2 * config.getString("cloudAuth.userAdminRole") >> "identity:user-admin"
        1 * config.getInt("cloudAuth.special.rsWeight") >> 500
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
        def callerRole = entityFactory.createClientRole()

        when:
        service.verifyCallerPrecedenceOverUser(caller, user)

        then:
        1 * applicationService.getUserIdentityRole(caller, _, _) >> callerRole
        1 * applicationService.getUserIdentityRole(user, _, _) >> userRole
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
        1 * applicationService.getUserIdentityRole(caller, _, _) >> callerRole
        1 * applicationService.getUserIdentityRole(user, _, _) >> userRole

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
        1 * applicationService.getUserIdentityRole(user, _, _) >> null

        then:
        thrown(ForbiddenException)
    }
}
