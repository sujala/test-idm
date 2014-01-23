package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.AuthorizationContext
import com.rackspace.idm.domain.entity.ScopeAccess
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultAuthorizationServiceTest extends RootServiceTest {
    @Shared DefaultAuthorizationService service

    def setupSpec() {
        service = new DefaultAuthorizationService()
    }

    def setup() {
        mockConfiguration(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockApplicationService(service)
        mockUserService(service)
        retrieveAccessControlRoles()
    }

    def "authorizeRacker verifies the scopeAccess"() {
        when:
        def result = service.authorizeRacker(createAuthContext(scopeAccess, [roleId]))

        then:
        result == expectedResult
        scopeAccessService.isScopeAccessExpired(scopeAccess) >> expired

        where:
        expectedResult  | expired   | roleId    | scopeAccess
        false           | true      | "2"       | null
        false           | true      | "2"       | createRackerScopeAcccss()
        false           | false     | "2"       | createRackerScopeAcccss()
        true            | false     | "1"       | createRackerScopeAcccss()
    }

    def "authorizeRacker verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createRackerScopeAcccss())

        when:
        def result = service.authorizeRacker(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeRacker verifies scopeAccess belongs to a racker"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeRacker(createAuthContext(userScopeAccess))

        then:
        result == false
    }

    def "authorizeCloudIdentityAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudIdentityAdmin(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudIdentityAdmin verifies scopeAccess belongs to a cloudIdentityAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def nonIdentityAdminResult = service.authorizeCloudIdentityAdmin(createAuthContext(userScopeAccess))


        then:
        nonIdentityAdminResult == false
    }

    def "authorizeCloudIdentityAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeCloudIdentityAdmin(createAuthContext(scopeAccess, ["1"]))

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
    }

    def "authorizeCloudServiceAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudServiceAdmin(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudServiceAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def nonServiceAdminResult = service.authorizeCloudServiceAdmin(createAuthContext(userScopeAccess))

        then:
        nonServiceAdminResult == false
    }

    def "authorizeCloudServiceAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeCloudServiceAdmin(createAuthContext(scopeAccess, ["1"]))

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
    }

    def "authorizeCloudUserAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudUserAdmin(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudUserAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeCloudUserAdmin(createAuthContext(userScopeAccess))

        then:
        result == false
    }

    def "authorizeCloudUserAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeCloudUserAdmin(createAuthContext(scopeAccess, ["1"]))

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
    }

    def "authorizeCloudUser verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudUser(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudUser verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeCloudUser(createAuthContext(userScopeAccess))

        then:
        result == false
    }

    def "authorizeCloudUser allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeCloudUser(createAuthContext(scopeAccess, ["1"]))

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
    }

    def "authorizeIdmSuperAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeIdmSuperAdmin(createAuthContext(expiredScopeAccess))

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeIdmSuperAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeIdmSuperAdmin(createAuthContext(userScopeAccess))

        then:
        result == false
    }

    def "authorizeIdmSuperAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeIdmSuperAdmin(createAuthContext(scopeAccess, ["1"]))

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
    }

    def "hasDefaultUserRole calls tenantService to user is not null" () {
        given:
        def user = null

        when:
        def result = service.hasDefaultUserRole(user)

        then:
        result == false
    }

    def "hasDefaultUserRole calls tenantService to verify role does not exist" () {
        given:
        def context = createAuthContext()

        when:
        def result = service.hasDefaultUserRole(context)

        then:
        result == false
    }

    def "hasDefaultUserRole calls tenantService to verify role exist"() {
        given:
        def context = createAuthContext(["1"])

        when:
        def result = service.hasDefaultUserRole(context)

        then:
        result == true
    }

    def "hasUserAdminRole calls tenantService to user is not null" () {
        given:
        def context = null

        when:
        def result = service.hasUserAdminRole(context)

        then:
        result == false
    }

    def "hasUserAdminRole calls tenantService to verify role does not exist" () {
        given:
        def context = createAuthContext()

        when:
        def result = service.hasUserAdminRole(context)

        then:
        result == false
    }

    def "hasUserAdminRole calls tenantService to verify role exist"() {
        given:
        def context = createAuthContext(["1"])

        when:
        def result = service.hasUserAdminRole(context)

        then:
        result == true
    }

    def "hasIdentityAdminRole calls tenantService to user is not null" () {
        given:
        def context = null

        when:
        def result = service.hasIdentityAdminRole(context)

        then:
        result == false
    }

    def "hasIdentityAdminRole calls tenantService to verify role does not exist" () {
        given:
        def context = createAuthContext()

        when:
        def result = service.hasIdentityAdminRole(context)

        then:
        result == false
    }

    def "hasIdentityAdminRole calls tenantService to verify role exist"() {
        given:
        def context = createAuthContext(["1"])

        when:
        def result = service.hasIdentityAdminRole(context)

        then:
        result == true
    }


    def "hasServiceAdminRole calls tenantService to user is not null" () {
        given:
        def context = null

        when:
        def result = service.hasServiceAdminRole(context)

        then:
        result == false
    }

    def "hasServiceAdminRole calls tenantService to verify role does not exist" () {
        given:
        def context = createAuthContext()

        when:
        def result = service.hasServiceAdminRole(context)

        then:
        result == false
    }

    def "hasServiceAdminRole calls tenantService to verify role exist"() {
        given:
        def context = createAuthContext(["1"])

        when:
        def result = service.hasServiceAdminRole(context)

        then:
        result == true
    }

    def "hasSameDomain checks if user is in domain"() {
        when:
        def user1 = entityFactory.createUser().with {
            it.domainId = domain1
            it
        }
        def user2 = entityFactory.createUser().with {
            it.domainId = domain2
            it
        }
        def result = service.hasSameDomain(user1, user2)

        then:
        result == expectedResult

        where:
        expectedResult  | domain1   | domain2
        true            | "domain1" | "domain1"
        false           | "domain1" | "domain2"
        false           | null      | "domain2"
        false           | "domain1" | null
        false           | null      | null
    }

    def "authorizeScopeAccess gets users roles if not expired"() {
        given:
        def scopeAccess = createScopeAccess()

        when:
        def result = service.getAuthorizationContext(scopeAccess)

        then:
        1 * userService.getUserByScopeAccess(_)
        1 * tenantService.getTenantRolesForUserNoDetail(_) >> [].asList()

        result.roles != null
        result.scopeAccess != null
    }

    def "authorizeScopeAccess does not get users roles if expired"() {
        given:
        def scopeAccess = expireScopeAccess(createScopeAccess())

        when:
        def result = service.getAuthorizationContext(scopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        0 * userService.getUserByScopeAccess(_)
        0 * tenantService.getTenantRolesForUser(_) >> [].asList()

        result.roles != null
        result.scopeAccess != null
    }

    def "authorizeScopeAccess returns list of roles in context"() {
        given:
        def scopeAccess = createScopeAccess()

        when:
        def result = service.getAuthorizationContext(scopeAccess)

        then:
        1 * userService.getUserByScopeAccess(_)
        1 * tenantService.getTenantRolesForUserNoDetail(_) >> [entityFactory.createTenantRole()].asList()

        result.roles.contains("1")
    }

    def "authorize verifies scopeAccess and user roles"() {
        given:
        def context = new AuthorizationContext().with {
            it.scopeAccess = scopeAccess
            it.roles = roles
            it
        }

        when:
        def result = service.authorize(context, clientRoles)

        then:
        result == expectedResult

        where:
        expectedResult  | scopeAccess                               | roles     | clientRoles
        false           | expireScopeAccess(createScopeAccess())    | []        | []
        false           | createScopeAccess()                       | ["notId"] | [entityFactory.createClientRole()]
        true            | createScopeAccess()                       | ["id"]    | [entityFactory.createClientRole()]
    }

    def "verifyRoleAccess validates scopeAccess and user roles"() {
        given:
        def context = new AuthorizationContext().with {
            it.scopeAccess = scopeAccess
            it.roles = roles
            it
        }

        when:
        def exceptionThrown = false
        try {
            service.verifyRoleAccess(context, clientRoles)
        } catch (Exception e) {
            exceptionThrown = true
        }

        then:
        exceptionThrown == expectedResult

        where:
        expectedResult  | scopeAccess                               | roles     | clientRoles
        true            | expireScopeAccess(createScopeAccess())    | []        | []
        true            | createScopeAccess()                       | ["notId"] | [entityFactory.createClientRole()]
        false           | createScopeAccess()                       | ["id"]    | [entityFactory.createClientRole()]
    }

    def retrieveAccessControlRoles() {
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole().with {
            it.id = "1"
            it
        }
        service.retrieveAccessControlRoles()
    }
}
