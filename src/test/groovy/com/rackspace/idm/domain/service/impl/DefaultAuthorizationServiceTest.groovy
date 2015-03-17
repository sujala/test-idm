package com.rackspace.idm.domain.service.impl

import com.google.common.collect.Sets
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.ClientRole
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultAuthorizationServiceTest extends RootServiceTest {
    @Shared DefaultAuthorizationService service

    static ClientRole TEST_ROLE;

    def setupSpec() {
        TEST_ROLE = entityFactory.createClientRole().with {
            it.name = "identity:test"
            it.id = "102341"
            it
        }
    }

    def setup() {
        service = new DefaultAuthorizationService()
        mockConfiguration(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockApplicationService(service)
        mockUserService(service)
        mockDomainService(service)
        mockRoleService(service)
        mockIdentityConfig(service)
        retrieveIdentityRoles()
    }

    def "authorizeRacker verifies the scopeAccess"() {
        when:
        def result = service.authorizeRacker(scopeAccess)

        then:
        result == expectedResult
        scopeAccessService.isScopeAccessExpired(scopeAccess) >> expired
        userService.getUserByScopeAccess(scopeAccess) >> entityFactory.createUser()
        tenantService.doesUserContainTenantRole(_, _) >> hasTenantRole

        where:
        expectedResult  | expired   | hasTenantRole | scopeAccess
        false           | true      | false         | null
        false           | true      | false         | createRackerScopeAcccss()
        false           | false     | false         | createRackerScopeAcccss()
        true            | false     | true          | createRackerScopeAcccss()
    }

    def "authorizeRacker verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createRackerScopeAcccss())

        when:
        def result = service.authorizeRacker(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeRacker verifies scopeAccess belongs to a racker"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeRacker(userScopeAccess)

        then:
        result == false
    }

    def "authorizeRacker allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createRackerScopeAcccss()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeRacker(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "authorizeCloudIdentityAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudIdentityAdmin(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudIdentityAdmin verifies scopeAccess belongs to a cloudIdentityAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def nonIdentityAdminResult = service.authorizeCloudIdentityAdmin(userScopeAccess)

        then:
        nonIdentityAdminResult == false
    }

    def "authorizeCloudIdentityAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeCloudIdentityAdmin(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "authorizeCloudServiceAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeCloudServiceAdmin(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeCloudServiceAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def nonServiceAdminResult = service.authorizeCloudServiceAdmin(userScopeAccess)

        then:
        nonServiceAdminResult == false
    }

    def "authorizeCloudServiceAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeCloudServiceAdmin(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "authorizeCloudUserAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUserAdmin(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        1 * userService.getUserByScopeAccess(expiredScopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        result == false
    }

    def "authorizeCloudUserAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUserAdmin(userScopeAccess)

        then:
        1 * userService.getUserByScopeAccess(userScopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        result == false
    }

    def "authorizeCloudUserAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUserAdmin(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "authorizeCloudUser verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUser(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        1 * userService.getUserByScopeAccess(expiredScopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        result == false
    }

    def "authorizeCloudUser verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUser(userScopeAccess)

        then:
        result == false
        1 * scopeAccessService.isScopeAccessExpired(userScopeAccess) >> false
        1 * userService.getUserByScopeAccess(userScopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * tenantService.doesUserContainTenantRole(user, _) >> false
    }

    def "authorizeCloudUser allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()

        when:
        def result = service.authorizeCloudUser(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "authorizeIdmSuperAdmin verifies the scopeAccess is not expired"() {
        given:
        def expiredScopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.authorizeIdmSuperAdmin(expiredScopeAccess)

        then:
        1 * scopeAccessService.isScopeAccessExpired(_) >> true
        result == false
    }

    def "authorizeIdmSuperAdmin verifies scopeAccess belongs to a cloudServiceAdmin"() {
        given:
        def userScopeAccess = createUserScopeAccess()

        when:
        def result = service.authorizeIdmSuperAdmin(userScopeAccess)

        then:
        result == false
    }

    def "authorizeIdmSuperAdmin allows access with valid role and non expired token"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.authorizeIdmSuperAdmin(scopeAccess)

        then:
        result == true
        1 * scopeAccessService.isScopeAccessExpired(scopeAccess) >> false
        1 * userService.getUserByScopeAccess(scopeAccess) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
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
        def user = entityFactory.createUser()

        when:
        def result = service.hasDefaultUserRole(user)

        then:
        result == false
        1 * tenantService.doesUserContainTenantRole(_, _) >> false
    }

    def "hasDefaultUserRole calls tenantService to verify role exist"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasDefaultUserRole(user)

        then:
        result == true
        1 * tenantService.doesUserContainTenantRole(_, _) >> true
    }

    def "hasUserAdminRole calls tenantService to user is not null" () {
        given:
        def user = null

        when:
        def result = service.hasUserAdminRole(user)

        then:
        result == false
    }

    def "hasUserAdminRole calls tenantService to verify role does not exist" () {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasUserAdminRole(user)

        then:
        result == false
        1 * tenantService.doesUserContainTenantRole(_, _) >> false
    }

    def "hasUserAdminRole calls tenantService to verify role exist"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasUserAdminRole(user)

        then:
        result == true
        1 * tenantService.doesUserContainTenantRole(_, _) >> true
    }

    def "hasUserManageRole call tenantService to verify role exists" () {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasUserManageRole(user)

        then:
        1 * tenantService.doesUserContainTenantRole(_, _)
    }

    def "hasIdentityAdminRole calls tenantService to user is not null" () {
        given:
        def user = null

        when:
        def result = service.hasIdentityAdminRole(user)

        then:
        result == false
    }

    def "hasIdentityAdminRole calls tenantService to verify role does not exist" () {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasIdentityAdminRole(user)

        then:
        result == false
        1 * tenantService.doesUserContainTenantRole(_, _) >> false
    }

    def "hasIdentityAdminRole calls tenantService to verify role exist"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasIdentityAdminRole(user)

        then:
        result == true
        1 * tenantService.doesUserContainTenantRole(_, _) >> true
    }


    def "hasServiceAdminRole calls tenantService to user is not null" () {
        given:
        def user = null

        when:
        def result = service.hasServiceAdminRole(user)

        then:
        result == false
    }

    def "hasServiceAdminRole calls tenantService to verify role does not exist" () {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasServiceAdminRole(user)

        then:
        result == false
        1 * tenantService.doesUserContainTenantRole(_, _) >> false
    }

    def "hasServiceAdminRole calls tenantService to verify role exist"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.hasServiceAdminRole(user)

        then:
        result == true
        1 * tenantService.doesUserContainTenantRole(_, _) >> true
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

    def "can retrieve identity role by name and id"() {
        expect:
        service.getCachedIdentityRoleById(TEST_ROLE.id).name == TEST_ROLE.name
        service.getCachedIdentityRoleByName(TEST_ROLE.name).id == TEST_ROLE.id
    }

    def "test role has implicit role"() {
        expect:
        service.getImplicitRolesForRole(TEST_ROLE.name).size() == 1
    }

    def retrieveIdentityRoles() {
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()
        roleService.getAllIdentityRoles() >> [TEST_ROLE, entityFactory.createClientRole(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())]

        staticConfig.implicitRoleSupportEnabled() >> true
        staticConfig.getImplicitRolesForRole(TEST_ROLE.name) >> Sets.newHashSet(IdentityRole.GET_USER_ROLES_GLOBAL);

        service.retrieveAccessControlRoles()
    }
}
