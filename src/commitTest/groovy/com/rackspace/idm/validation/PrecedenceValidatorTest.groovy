package com.rackspace.idm.validation

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.ForbiddenException
import org.apache.commons.lang.RandomStringUtils
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
        mockAuthorizationService(service)
        mockConfiguration(service)
        mockRoleService(service)
        mockRequestContextHolder(service)
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

    def "verifyCallerCanListRolesForUser does not require the user to meet any other security requirements if the caller is the same as the target user"() {
        given:
        User caller = entityFactory.createUser()

        when:
        service.verifyCallerCanListRolesForUser(caller, caller)

        then:
        0 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())
        0 * authorizationService.getIdentityTypeRoleAsEnum(caller)
        0 * applicationService.getUserIdentityRole(caller)
        noExceptionThrown() // the user is allowed access
    }

    def "verifyCallerCanListRolesForUser allows access if the user has the identity:get-user-roles-global role"() {
        given:
        User caller = entityFactory.createUser().with { it.id = RandomStringUtils.randomAlphanumeric(8); it }
        User user = entityFactory.createUser().with { it.id = RandomStringUtils.randomAlphanumeric(8); it }

        when:
        service.verifyCallerCanListRolesForUser(caller, user)

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName()) >> true
        0 * authorizationService.getIdentityTypeRoleAsEnum(caller)
        0 * applicationService.getUserIdentityRole(caller)
        0 * applicationService.getUserIdentityRole(user)
        noExceptionThrown() // the user is allowed access
    }

    @Unroll
    def "verifyCallerCanListRolesForUser validates access for #callerType to list roles for #userType - allowAccess = #allowAccess, sameDomain = #sameDomain"() {
        given:
        User caller = entityFactory.createUser().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        User user = entityFactory.createUser().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            if (sameDomain) {
                it.domainId = caller.domainId
            } else {
                it.domainId = RandomStringUtils.randomAlphanumeric(8)
            }
            it
        }
        ClientRole callerClientRole = entityFactory.createClientRole().with {
            it.rsWeight = callerType.level.levelAsInt
            it.name = callerType.roleName
            it
        }
        ClientRole userClientRole = entityFactory.createClientRole().with {
            it.rsWeight = userType.level.levelAsInt
            it.name = userType.roleName
            it
        }

        when:
        def exceptionThrown = null
        try {
            service.verifyCallerCanListRolesForUser(caller, user)
        } catch (e) {
            exceptionThrown = e
        }

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName()) >> false
        authorizationService.getIdentityTypeRoleAsEnum(caller) >> callerType
        1 * applicationService.getUserIdentityRole(caller) >> callerClientRole
        1 * applicationService.getUserIdentityRole(user) >> userClientRole
        if (allowAccess) {
            assert exceptionThrown == null
        } else {
            assert exceptionThrown.class == ForbiddenException
        }

        where:
        callerType                          | userType                            | sameDomain | allowAccess
        IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.IDENTITY_ADMIN | false      | true
        IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_ADMIN     | false      | true
        IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_MANAGER   | false      | true
        IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.DEFAULT_USER   | false      | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.IDENTITY_ADMIN | false      | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_ADMIN     | false      | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_MANAGER   | false      | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.DEFAULT_USER   | false      | true
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.IDENTITY_ADMIN | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_ADMIN     | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_MANAGER   | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.DEFAULT_USER   | false      | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_ADMIN     | true       | false
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_MANAGER   | true       | true
        IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.DEFAULT_USER   | true       | true
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.IDENTITY_ADMIN | false      | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_ADMIN     | false      | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_MANAGER   | false      | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.DEFAULT_USER   | false      | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_ADMIN     | true       | false
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_MANAGER   | true       | true
        IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.DEFAULT_USER   | true       | true
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.SERVICE_ADMIN  | false      | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.IDENTITY_ADMIN | false      | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_ADMIN     | false      | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_MANAGER   | false      | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.DEFAULT_USER   | false      | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_ADMIN     | true       | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_MANAGER   | true       | false
        IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.DEFAULT_USER   | true       | false
    }

    @Unroll
    def "verifyEffectiveCallerPrecedenceOverUser: calls correct services - userType = #userType"() {
        given:
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def fedCaller = entityFactory.createFederatedUser()
        def user = entityFactory.createUser()
        ClientRole defaultUserRole = entityFactory.createClientRole(IdentityUserTypeEnum.DEFAULT_USER.roleName, IdentityUserTypeEnum.DEFAULT_USER.levelAsInt)

        when: "provisioned user caller"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerUserType
        1 * applicationService.getUserIdentityRole((EndUser) user) >> defaultUserRole

        if (((IdentityUserTypeEnum)callerUserType).isDomainBasedAccessLevel()) {
            1 * authorizationService.verifyDomain(caller, user)
        } else {
            0 * authorizationService.verifyDomain(caller, user)
        }

        when: "federated user caller"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> fedCaller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerUserType
        1 * applicationService.getUserIdentityRole((EndUser) user) >> defaultUserRole

        if (((IdentityUserTypeEnum)callerUserType).isDomainBasedAccessLevel()) {
            1 * authorizationService.verifyDomain(fedCaller, user)
        } else {
            0 * authorizationService.verifyDomain(fedCaller, user)
        }

        where:
        callerUserType << [IdentityUserTypeEnum.SERVICE_ADMIN, IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    def "verifyEffectiveCallerPrecedenceOverUser: user-manage has precedence over another user-manage in same domain"() {
        given:
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def user = entityFactory.createUser()
        ClientRole userManageRole = entityFactory.createClientRole(IdentityUserTypeEnum.USER_MANAGER.roleName, IdentityUserTypeEnum.USER_MANAGER.levelAsInt)

        when: "provisioned user caller"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        noExceptionThrown()

        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 * applicationService.getUserIdentityRole((EndUser) user) >> userManageRole
        1 * authorizationService.verifyDomain(caller, user)
    }

    def "verifyEffectiveCallerPrecedenceOverUser: error check"() {
        given:
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def user = entityFactory.createUser()

        ClientRole defaultUserRole = entityFactory.createClientRole(IdentityUserTypeEnum.DEFAULT_USER.roleName, IdentityUserTypeEnum.DEFAULT_USER.levelAsInt)
        ClientRole userAdminRole = entityFactory.createClientRole(IdentityUserTypeEnum.USER_ADMIN.roleName, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        when: "caller is an racker"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        thrown(ForbiddenException)

        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createRacker()

        when: "caller type is null"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        thrown(ForbiddenException)

        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> null

        when: "caller does not have precedence over user"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        thrown(ForbiddenException)

        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 * applicationService.getUserIdentityRole((EndUser) user) >> userAdminRole

        when: "caller and target user have different domains"
        service.verifyEffectiveCallerPrecedenceOverUser(user)

        then:
        thrown(ForbiddenException)

        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * securityContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 * applicationService.getUserIdentityRole((EndUser) user) >> defaultUserRole
        1 * authorizationService.verifyDomain(caller, user) >> {throw new ForbiddenException()}
    }

    def mockRoleService(service) {
        mockRoleService = Mock()
        service.roleService = mockRoleService;
    }
}
