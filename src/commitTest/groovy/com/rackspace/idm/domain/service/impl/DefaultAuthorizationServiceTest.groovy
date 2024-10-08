package com.rackspace.idm.domain.service.impl

import com.google.common.collect.Sets
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.api.security.AuthorizationContext
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.api.security.ImmutableTenantRole
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.DN
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.IdmExceptionAssert
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
        1 * applicationService.getUserIdentityRole(user) >> entityFactory.createClientRole(IdentityUserTypeEnum.SERVICE_ADMIN.roleName)
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

        def authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityUserTypeEnum.USER_MANAGER.roleName))

        when:
        service.verifyEffectiveCallerHasManagementAccessToUser(userId)

        then:
        1 * userService.checkAndGetUserById(userId) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        (1.._) * requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
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
        def authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityRole.RCN_ADMIN.roleName))

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

    SourcedRoleAssignments genAssignmentsForRoleWithName(BaseUser user, String roleName, RoleAssignmentType assignmentType = RoleAssignmentType.DOMAIN) {
        ImmutableClientRole icr = new ImmutableClientRole(new ClientRole().with {
            it.name =roleName
            it.id = roleName + "id"
            it
        })
        SourcedRoleAssignments assignments = new SourcedRoleAssignments(user)
        assignments.addUserSourcedAssignment(icr, assignmentType, Collections.EMPTY_SET)
        return assignments
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

        def authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityUserTypeEnum.USER_ADMIN.roleName))

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

        def authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityUserTypeEnum.USER_MANAGER.roleName))

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
        TenantRole userAdminTr = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_ADMIN.roleName
            it
        }
        TenantRole userManageTr = new TenantRole().with {
            it.name = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }
        def authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityRole.RCN_ADMIN.roleName))

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
        authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityUserTypeEnum.USER_ADMIN.roleName))
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        !isAuthorized

        0 * tenantService.doesUserContainTenantRole(_, _)
        0 * domainService.doDomainsShareRcn(_, _)

        when: "userManage caller attempting to update DA with userAdmin principal"
        daEntity.principal.domainId >> user.domainId
        authorizationContext = new AuthorizationContext(genAssignmentsForRoleWithName(user, IdentityUserTypeEnum.USER_MANAGER.roleName))
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
        isAuthorized = service.isCallerAuthorizedToManageDelegationAgreement(daEntity)

        then:
        !isAuthorized

        0 * tenantService.doesUserContainTenantRole(_, _)
        0 * domainService.doDomainsShareRcn(_, _)
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * tenantService.doesUserContainTenantRole(user, _) >> true
    }

    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature disabled, default not set"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> false
        def user = new User().with {
            it.id = "anId"
            it.domainId = domainId
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd")

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        request.domainId == null
    }

    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default not set if domainId already provided"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def requestedDomain = "aDifferentDomain"
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd").with {
            it.domainId = requestedDomain
            it
        }

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        request.domainId == requestedDomain
    }

    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default pulled from user if no tenant"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd")

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        request.domainId == user.domainId
    }

    /**
     * Verifies that if tenantId is specified, the default is pulled from tenant name
     * @return
     */
    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default pulled from tenantid if exists"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd").with {
            it.tenantId = "tenantId"
            it
        }
        Tenant tenant = entityFactory.createTenant("tenantid", "tenantname").with {
            it.domainId = "tenantdomain"
            it
        }

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        1 * tenantService.getTenant(request.tenantId) >> tenant
        request.domainId == tenant.domainId
    }

    /**
     * Verifies that if tenantName is specified, the default is pulled from tenant name
     * @return
     */
    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default pulled from tenant name if exists"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd").with {
            it.tenantName = "tenantName"
            it
        }
        Tenant tenant = entityFactory.createTenant("tenantid", "tenantname").with {
            it.domainId = "tenantdomain"
            it
        }

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        1 * tenantService.getTenantByName(request.tenantName) >> tenant
        request.domainId == tenant.domainId
    }

    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default pulled from user if tenant does not have domain"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd").with {
            it.tenantId = "tenantId"
            it
        }
        Tenant tenant = entityFactory.createTenant("tenantid", "tenantname")

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        1 * tenantService.getTenant(request.tenantId) >> tenant
        request.domainId == user.domainId
    }

    def "updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary: When feature enabled, default pulled from user if tenant does not exist"() {
        given:
        repositoryConfig.shouldSetDefaultAuthorizationDomain() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }
        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest("user", "pwd").with {
            it.tenantId = "tenantId"
            it
        }

        when:
        service.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, request)

        then:
        1 * tenantService.getTenant(request.tenantId) >> null
        request.domainId == user.domainId
    }

    def "verifyUserAuthorizedToAuthenticateOnDomain: If feature disabled, no exception thrown when not authorized"() {
        repositoryConfig.shouldVerifyAuthorizationDomains() >> false
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }

        when:
        service.verifyUserAuthorizedToAuthenticateOnDomain(user, "otherDomain")

        then:
        notThrown(NotAuthorizedException)
    }

    def "verifyUserAuthorizedToAuthenticateOnDomain: If feature enabled, exception thrown when not authorized"() {
        repositoryConfig.shouldVerifyAuthorizationDomains() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }

        when:
        service.verifyUserAuthorizedToAuthenticateOnDomain(user, "otherDomain")

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotAuthorizedException, "AUTH-006", "Not authorized for the domain.")
    }

    @Unroll
    def "verifyUserAuthorizedToAuthenticateOnDomain: If feature enabled and '#domainId' domain passed, exception not thrown"() {
        repositoryConfig.shouldVerifyAuthorizationDomains() >> true
        def user = new User().with {
            it.id = "anId"
            it.domainId = "adomain"
            it
        }

        when:
        service.verifyUserAuthorizedToAuthenticateOnDomain(user, domainId)

        then:
        notThrown(NotAuthorizedException)

        where:
        domainId << [null, "", "   "]
    }

    def retrieveIdentityRoles() {
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()
        roleService.getAllIdentityRoles() >> [TEST_ROLE, entityFactory.createClientRole(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())]

        staticConfig.getImplicitRoleMap() >> [(TEST_ROLE.name):[IdentityRole.GET_USER_ROLES_GLOBAL.roleName] as Set]

        service.retrieveAccessControlRoles()
    }
}
