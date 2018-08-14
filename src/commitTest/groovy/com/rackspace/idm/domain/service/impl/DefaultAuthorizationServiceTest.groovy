package com.rackspace.idm.domain.service.impl

import com.google.common.collect.Sets
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.api.security.AuthorizationContext
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.api.security.ImmutableTenantRole
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.DN
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
        mockIdentityUserService(service)
        mockRequestContextHolder(service)

        applicationService.getCachedClientRoleByName(_) >> new ImmutableClientRole(new ClientRole())

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

    def "test role has implicit role"() {
        expect:
        service.getImplicitRolesForRole(TEST_ROLE.name).size() == 1
    }

    def "verifyEffectiveCallerHasManagementAccessToUser: verify service authorization requires user-manage+"() {
        given:
        mockRequestContextHolder(service)
        mockPrecedenceValidator(service)

        def domainId = "domainId"
        def userId = "userId"
        def user = new User().with {
            it.id = userId
            it.domainId = domainId
            it
        }
        def caller = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }

        requestContext.getEffectiveCaller() >> caller

        TenantRole tenantRole = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }
        ImmutableTenantRole immutableTenantRole = new ImmutableTenantRole(tenantRole)
        def authorizationContext = new AuthorizationContext([immutableTenantRole],[])


        when:
        service.verifyEffectiveCallerHasManagementAccessToUser(userId)

        then:
        1 * identityConfig.staticConfig.getIdentityUserManagerRoleName() >> IdentityUserTypeEnum.USER_MANAGER.roleName
        1 * userService.checkAndGetUserById(userId) >> user
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        1 * requestContext.getEffectiveCallersUserType() >>  IdentityUserTypeEnum.USER_MANAGER
    }

    def "isCallerAuthorizedToManageDelegationAgreement: verify authorization for DA's effective principal"() {
        given:
        def domainId = "domainId"
        def groupId = "groupId"
        def caller = new User().with {
            it.id = "callerId"
            it.uniqueId = "rsId=callerId"
            it.enabled = true
            it.domainId = domainId
            it.userGroupDNs = [new DN(String.format("rsId=%s", groupId))]
            it
        }
        def daEntity = new DelegationAgreement().with {
            it.principal =  Mock(DelegationPrincipal)
            it
        }
        daEntity.setPrincipalDN(caller.dn)

        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER
        daEntity.principal.domainId >> caller.domainId

        def userGroup = new UserGroup().with {
            it.domainId = caller.domainId
            it.id = groupId
            it
        }

        requestContext.getEffectiveCaller() >> caller

        when: "principal is a effective caller"
        boolean isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * identityUserService.getEndUserById(_)
        0 * domainService.doDomainsShareRcn(_, _)
        0 * requestContext.getEffectiveCallerAuthorizationContext()

        when: "principal is a user group"
        daEntity.principal.getId() >> userGroup.id
        daEntity.principal.principalType >> PrincipalType.USER_GROUP
        daEntity.principal.domainId >> userGroup.domainId

        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * identityUserService.getEndUserById(_)
        0 * domainService.doDomainsShareRcn(_, _)
        0 * requestContext.getEffectiveCallerAuthorizationContext()
    }

    def "isCallerAuthorizedToManageDelegationAgreement: verify authorization for a rcn:admin caller"() {
        given:
        def domainId = "domainId"
        def userId = "userId"
        def user = new User().with {
            it.id = userId
            it.domainId = domainId
            it
        }
        def caller = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }
        def daEntity = new DelegationAgreement().with {
            it.principal =  Mock(DelegationPrincipal)
            it
        }

        daEntity.principal.getId() >> user.id
        daEntity.principal.principalType >> PrincipalType.USER
        daEntity.principal.domainId >> user.domainId

        def userGroup = new UserGroup().with {
            it.domainId = user.domainId
            it.id = "groupId"
            it
        }

        requestContext.getEffectiveCaller() >> caller

        TenantRole tenantRole = new TenantRole().with {
            it.name = IdentityRole.RCN_ADMIN.roleName
            it
        }
        ImmutableTenantRole immutableTenantRole = new ImmutableTenantRole(tenantRole)
        def authorizationContext = new AuthorizationContext([immutableTenantRole],[])

        when: "principal is a user"
        boolean isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        1 * domainService.doDomainsShareRcn(caller.domainId, user.domainId) >> true
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext

        when: "principal is a user group"
        daEntity.principal.getId() >> userGroup.id
        daEntity.principal.principalType >> PrincipalType.USER_GROUP
        daEntity.principal.domainId >> userGroup.domainId

        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        1 * domainService.doDomainsShareRcn(caller.domainId, user.domainId) >> true
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
    }

    def "isCallerAuthorizedToManageDelegationAgreement: verify authorization for a userAdmin caller"() {
        given:
        def domainId = "domainId"
        def userId = "userId"
        def user = new User().with {
            it.id = userId
            it.domainId = domainId
            it
        }
        def caller = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }
        def daEntity = new DelegationAgreement().with {
            it.principal =  Mock(DelegationPrincipal)
            it
        }

        daEntity.principal.getId() >> user.id
        daEntity.principal.principalType >> PrincipalType.USER
        daEntity.principal.domainId >> user.domainId

        def userGroup = new UserGroup().with {
            it.domainId = user.domainId
            it.id = "groupId"
            it
        }

        requestContext.getEffectiveCaller() >> caller

        TenantRole tenantRole = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_ADMIN.roleName
            it
        }
        ImmutableTenantRole immutableTenantRole = new ImmutableTenantRole(tenantRole)
        def authorizationContext = new AuthorizationContext([immutableTenantRole],[])

        when: "principal is a user"
        boolean isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * domainService.doDomainsShareRcn(_, _)
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext

        when: "principal is a user group"
        daEntity.principal.getId() >> userGroup.id
        daEntity.principal.principalType >> PrincipalType.USER_GROUP
        daEntity.principal.domainId >> userGroup.domainId

        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * domainService.doDomainsShareRcn(caller.domainId, user.domainId)
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
    }

    def "isCallerAuthorizedToManageDelegationAgreement: verify authorization for a userManage caller"() {
        given:
        def domainId = "domainId"
        def userId = "userId"
        def user = new User().with {
            it.id = userId
            it.domainId = domainId
            it
        }
        def caller = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }
        def daEntity = new DelegationAgreement().with {
            it.principal =  Mock(DelegationPrincipal)
            it
        }

        daEntity.principal.getId() >> user.id
        daEntity.principal.principalType >> PrincipalType.USER
        daEntity.principal.domainId >> user.domainId

        def userGroup = new UserGroup().with {
            it.domainId = user.domainId
            it.id = "groupId"
            it
        }

        requestContext.getEffectiveCaller() >> caller

        TenantRole tenantRole = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }
        ImmutableTenantRole immutableTenantRole = new ImmutableTenantRole(tenantRole)
        def authorizationContext = new AuthorizationContext([immutableTenantRole],[])

        when: "principal is a user"
        boolean isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * domainService.doDomainsShareRcn(_, _)
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * tenantService.doesUserContainTenantRole(_, _) >> false
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext

        when: "principal is a user group"
        daEntity.principal.getId() >> userGroup.id
        daEntity.principal.principalType >> PrincipalType.USER_GROUP
        daEntity.principal.domainId >> userGroup.domainId

        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        isAuthorized

        0 * domainService.doDomainsShareRcn(_, _)
        0 * tenantService.doesUserContainTenantRole(_, _)
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
    }

    def "isCallerAuthorizedToManageDelegationAgreement: error check and invalid cases"() {
        given:
        def domainId = "domainId"
        def userId = "userId"
        def user = new User().with {
            it.id = userId
            it.domainId = domainId
            it
        }
        def caller = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }
        def daEntity = new DelegationAgreement().with {
            it.principal =  Mock(DelegationPrincipal)
            it
        }

        daEntity.principal.getId() >> user.id
        daEntity.principal.principalType >> PrincipalType.USER
        daEntity.principal.domainId >> user.domainId

        requestContext.getEffectiveCaller() >> caller

        // Tenant Roles
        TenantRole rcnAdminTr = new TenantRole().with {
            it.name = IdentityRole.RCN_ADMIN.roleName
            it
        }
        TenantRole userAdminTr = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_ADMIN.roleName
            it
        }
        TenantRole userManageTr = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }
        def authorizationContext = new AuthorizationContext([new ImmutableTenantRole(rcnAdminTr)],[])

        when: "da is null"
        service.isCallerAuthorizedToManageDelegationAgreement(null)

        then:
        thrown(IllegalArgumentException)

        when: "da's principal is null"
        service.isCallerAuthorizedToManageDelegationAgreement(new DelegationAgreement())

        then:
        thrown(IllegalArgumentException)

        when: "rcnAdmin caller has different RCN"
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        def isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        !isAuthorized

        0 * tenantService.doesUserContainTenantRole(_, _)
        1 * domainService.doDomainsShareRcn(_, _) >> false

        when: "userAdmin caller belongs to different domain"
        daEntity.principal.domainId >> "otherDomainId"
        authorizationContext = new AuthorizationContext([new ImmutableTenantRole(userAdminTr)],[])
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        !isAuthorized

        0 * tenantService.doesUserContainTenantRole(_, _)
        0 * domainService.doDomainsShareRcn(_, _)

        when: "userManage caller attempting to update DA with userAdmin principal"
        daEntity.principal.domainId >> user.domainId
        authorizationContext = new AuthorizationContext([new ImmutableTenantRole(userManageTr)],[])
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        !isAuthorized

        0 * tenantService.doesUserContainTenantRole(_, _)
        0 * domainService.doDomainsShareRcn(_, _)
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def retrieveIdentityRoles() {
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()
        roleService.getAllIdentityRoles() >> [TEST_ROLE, entityFactory.createClientRole(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())]

        staticConfig.getImplicitRolesForRole(TEST_ROLE.name) >> Sets.newHashSet(IdentityRole.GET_USER_ROLES_GLOBAL);

        service.retrieveAccessControlRoles()
    }
}
