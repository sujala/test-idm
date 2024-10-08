package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.*
import com.rackspace.idm.exception.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.Response

import static com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum.PROPAGATE
import static com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum.STANDARD
import static com.rackspace.idm.domain.service.IdentityUserTypeEnum.*

class DomainAdminChangeServiceTest extends Specification {
    @Shared RequestContextHolder requestContextHolder
    @Shared IdentityUserService identityUserService
    @Shared UserService userService
    @Shared def requestContext
    @Shared def securityContext
    @Shared AuthorizationService authorizationService
    @Shared ExceptionHandler exceptionHandler
    @Shared TenantService tenantService
    @Shared ApplicationService applicationService
    @Shared AtomHopperClient atomHopperClient
    @Shared DefaultCloud20Service defaultCloud20Service
    @Shared DomainService domainService

    @Shared def identityConfig
    @Shared def reloadableConfig

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        authorizationService = Mock(AuthorizationService)

        requestContext = Mock(RequestContext)
        requestContextHolder = Mock(RequestContextHolder)
        requestContextHolder.getRequestContext() >> requestContext
        securityContext = Mock(SecurityContext)
        requestContext.getSecurityContext() >> securityContext
        exceptionHandler = Mock(ExceptionHandler)
        identityUserService = Mock(IdentityUserService)
        tenantService = Mock(TenantService)
        applicationService = Mock(ApplicationService)
        atomHopperClient = Mock(AtomHopperClient)
        domainService = Mock(DomainService)

        defaultCloud20Service = new DefaultCloud20Service()
        defaultCloud20Service.authorizationService = authorizationService
        defaultCloud20Service.exceptionHandler = exceptionHandler
        defaultCloud20Service.requestContextHolder = requestContextHolder
        defaultCloud20Service.identityUserService = identityUserService
        defaultCloud20Service.tenantService = tenantService
        defaultCloud20Service.applicationService = applicationService
        defaultCloud20Service.atomHopperClient = atomHopperClient
        defaultCloud20Service.domainService = domainService

        identityConfig = Mock(IdentityConfig)
        reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig.getReloadableConfig() >> reloadableConfig
        defaultCloud20Service.identityConfig = identityConfig

        // Common settings for the client role lookups. Based on convention for role names.
        applicationService.getCachedClientRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
        applicationService.getCachedClientRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
        applicationService.getCachedClientRoleById(_) >> {String id -> createImmutableClientRole(id, id.endsWith("RBAC") ? RoleLevelEnum.LEVEL_1000.levelAsInt : RoleLevelEnum.LEVEL_500.levelAsInt)}
    }

    def "Caller token is validated"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then: "Verifies the provided token"
        1 * securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
    }

    def "Negative Test: Invalid caller token passes thrown exception to exceptionhandler as-is"() {
        given:
        def exceptionToThrow = new NotAuthorizedException();
        DomainAdministratorChange changeRequest = new DomainAdministratorChange()

        when:
        defaultCloud20Service.modifyDomainAdministrator("atoken", "adomain", changeRequest)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(_) >> { throw exceptionToThrow}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            args[0] == exceptionToThrow
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }
    }

    def "Caller is validated to have appropriate role"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange()
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then: "Verifies the caller token"
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(Constants.IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_NAME)
    }

    @Unroll
    def "Negative Test: When caller role verification fails with #exception.class.name, exception handler called"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange()
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then: "Verifies the caller token"
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(Constants.IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_NAME) >> {throw exception}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            args[0] == exception
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        exception                    | _
        new ForbiddenException()     | _
        new NotAuthorizedException() | _
        new NotFoundException()      | _
    }

    @Unroll
    def "Negative Test: Promote and demote user must both be specified: promoteUser: #promoteUser; demoteUser: #demoteUser"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = promoteUser
            it.demoteUserId = demoteUser
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], BadRequestException, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Both promote and demote userIds must be provided")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUser | demoteUser
        null    | null
        null    | "auser"
        "auser" | null
    }

    def "Negative Test: Promote and demote user must be different"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "auser"
            it.demoteUserId = "auser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Must specify different users to promote and demote")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }
    }

    @Unroll
    def "Negative Test: Promote and demote must exist; exception handler provided exception: promoteUser: #promoteUser, demoteUser: #demoteUser"() {
        given:
        def notFoundExceptionToThrow = new NotFoundException()
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = promoteUser
            it.demoteUserId = demoteUser
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then:
        identityUserService.checkAndGetEndUserById("exist") >> new User()
        1 * identityUserService.checkAndGetEndUserById(!"exist") >> {throw notFoundExceptionToThrow}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            args[0] == notFoundExceptionToThrow
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUser | demoteUser
        "notexist"  | "exist"
        "exist"     | "notexist"
        "notexist"  | "notexist2"
    }

    @Unroll
    def "Negative Test: Promote and demote must be in same non-blank domain and match that in url: promoteUserDomain: '#promoteUserDomain', demoteUserDomain: '#demoteUserDomain'"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, urlDomain, changeRequest)

        then:
        identityUserService.checkAndGetEndUserById("promoteUser") >> new User().with{
            it.enabled = true
            it.domainId = promoteUserDomain
            it
        }
        identityUserService.checkAndGetEndUserById(!"enabled") >> new User().with{
            it.enabled = true
            it.domainId = demoteUserDomain
            it
        }
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Both the promote and demote users must belong to the same domain as the domain in the url")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUserDomain | demoteUserDomain | urlDomain
        "domainX"  | "domainY" | "domainX"
        "domainX"  | "domainY" | "domainY"
        "domainX"  | "domainX" | "domainY"
        "domainX"     | null | "domainX"
        null  | "domainY" | "domainY"
        null | null | "adomain"
        ""   | "domainY" | "something"
        "domainX"   | "" | "domainX"
        "" | "" | "domain"
    }

    @Unroll
    def "Negative Test: Promote and demote must be enabled; exception handler provided exception: promoteUser: #promoteUser, demoteUser: #demoteUser"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = promoteUser
            it.demoteUserId = demoteUser
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        identityUserService.checkAndGetEndUserById("enabled") >> new User().with{
            it.enabled=true
            it.domainId="domainx" // Same domain
            it
        }
        identityUserService.checkAndGetEndUserById(!"enabled") >> new User().with{
            it.enabled=false
            it.domainId="domainx" // Same domain
            it
        }
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Both the promote and demote users must be enabled")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUser | demoteUser
        "enabled"  | "notenabled"
        "notenabled"     | "enabled"
        "notenabled"  | "notenabled2"
    }

    @Unroll
    def "Negative Test: Promote and demote must be provisioned users; exception handler provided exception: promoteUser: #promoteUser, demoteUser: #demoteUser"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = promoteUser
            it.demoteUserId = demoteUser
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "adomain", changeRequest)

        then:
        identityUserService.checkAndGetEndUserById("provisioned") >> new User().with{
            it.enabled=true
            it.domainId="domainx" // Same domain
            it
        }
        identityUserService.checkAndGetEndUserById(!"provisioned") >> new FederatedUser().with{
            it.domainId="domainx" // Same domain
            it
        }
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Both the promote and demote users must be Rackspace managed users")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUser | demoteUser
        "provisioned"  | "notprovisioned"
        "notprovisioned"     | "provisioned"
        "notprovisioned"  | "notprovisioned2"
    }

    @Unroll
    def "Negative Test: Promote user must not have user-admin role"() {
        given:
        def callerToken = "atoken"
        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "useradmin"
            it.demoteUserId = "useradmin2"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById(_) >> {new User().with{
            it.enabled=true
            it.domainId="domainx" // Same domain
            it
        }}

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(_) >> userRoles
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Promote user is already an admin")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        userRoles | _
        [createTenantRole(USER_ADMIN.roleName)] | _
        [createTenantRole("a"), createTenantRole(USER_ADMIN.roleName)]  | _
        [createTenantRole("a"), createTenantRole(USER_ADMIN.roleName), createTenantRole("b")]  | _
    }

    @Unroll
    def "Negative Test: Demote user must have user-admin role"() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser")
        def demoteUser = createUser("demoteUser")

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser) >> [createTenantRole(USER_MANAGER.roleName)]
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser) >> userRoles
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "Demote user is not an admin")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        userRoles | _
        [createTenantRole(USER_MANAGER.roleName)] | _
        [createTenantRole("a"), createTenantRole(USER_MANAGER.roleName)]  | _
        [createTenantRole("a"), createTenantRole(DEFAULT_USER.roleName), createTenantRole("b")]  | _
    }

    @Unroll
    def "Negative Test Global Prop Roles: Promote and demote users must have same propagating roles"() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser")
        def demoteUser = createUser("demoteUser")

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser) >> promoteUserRoles
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser) >> demoteUserRoles
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "The promote and demote users must have the same propagating roles")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUserRoles | demoteUserRoles
        [createTenantRole("a", PROPAGATE), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("b", PROPAGATE), createTenantRole(USER_ADMIN.roleName)]
    }

    /**
     * Test different scenarios of tenant assigned propagating roles between the users. The propagating roles must
     * match exactly (exact same tenants) for exact same roles.
     * @return
     */
    @Unroll
    def "Negative Test Tenant Assigned Prop Roles: Promote and demote users must have same propagating roles"() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser")
        def demoteUser = createUser("demoteUser")

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        1 * tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser) >> promoteUserRoles
        1 * tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser) >> demoteUserRoles
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "The promote and demote users must have the same propagating roles")
            Response.ok() //just always return ok. Return value here isn't important as we trust exceptionHandler works
        }

        where:
        promoteUserRoles | demoteUserRoles
        [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("b", PROPAGATE, ["1"] as Set), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE, ["2"] as Set), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE, ["1","2"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE, ["1", "3"] as Set), createTenantRole(USER_ADMIN.roleName)]
        [createTenantRole("a", PROPAGATE, ["1","2"] as Set), createTenantRole(USER_MANAGER.roleName)] | [createTenantRole("a", PROPAGATE, ["1"] as Set), createTenantRole(USER_ADMIN.roleName)]
    }

    def "Negative Test : Unverified users should not be allowed "() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUnverifiedUser("promoteUser")
        def demoteUser = createUnverifiedUser("demoteUser")

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
            Response.ok()
        }
    }

    def "Positive Test: Promoting user-manage to user-admin. No RBAC deletions"() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser").with {
            it.uniqueId = "rsId=1"
            it
        }
        def demoteUser = createUser("demoteUser").with {
            it.uniqueId = "rsId=2"
            it
        }
        def domain = new Domain().with {
            it.userAdminDN = demoteUser.getDn()
            it
        }

        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser) >> [createTenantRole(DEFAULT_USER.roleName)]
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser) >> [createTenantRole(USER_ADMIN.roleName)]

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        def response = defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest).build()

        then: "Promote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(promoteUser, {it.name == USER_ADMIN.roleName}, true)
        1 * tenantService.deleteTenantRole({it.name == DEFAULT_USER.roleName})

        and: "Domain's userAdminDN is properly updated"
        1 * domainService.getDomain(promoteUser.domainId) >> domain
        1 * domainService.updateDomain(domain) >> {arg ->
            def domainArg = (Domain) arg[0]
            assert domainArg.userAdminDN == promoteUser.getDn()
        }

        and: "Demote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(demoteUser, {it.name == DEFAULT_USER.roleName}, true)
        1 * tenantService.deleteTenantRole({it.name == USER_ADMIN.roleName})

        response.status == HttpStatus.SC_NO_CONTENT

        and: "verify if cached role name is retrieved from applicationService"
        1 * applicationService.getCachedClientRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
        1 * applicationService.getCachedClientRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
    }

    def "Positive Test: Promoting user-manage to user-admin. Deletes RBAC and user-classification roles from promote and demote users"() {
        given:
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser").with {
            it.uniqueId = "rsId=1"
            it
        }
        def demoteUser = createUser("demoteUser").with {
            it.uniqueId = "rsId=2"
            it
        }
        def domain = new Domain().with {
            it.userAdminDN = demoteUser.getDn()
            it
        }
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser) >> [createTenantRole("promoteRBAC"), createTenantRole("promote"),createTenantRole(DEFAULT_USER.roleName),createTenantRole(USER_MANAGER.roleName)]
        tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser) >> [createTenantRole("demoteRBAC"), createTenantRole("demote"), createTenantRole(USER_ADMIN.roleName)]

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new UserScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        def response = defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest).build()

        then: "Promote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(promoteUser, {it.name == USER_ADMIN.roleName}, true)
        1 * tenantService.deleteTenantRole({it.name == "promoteRBAC"})
        1 * tenantService.deleteTenantRole({it.name == DEFAULT_USER.roleName})
        1 * tenantService.deleteTenantRole({it.name == USER_MANAGER.roleName})
        0 * tenantService.deleteTenantRole({it.name == "promote"})

        and: "Domain's userAdminDN is properly updated"
        1 * domainService.getDomain(promoteUser.domainId) >> domain
        1 * domainService.updateDomain(domain) >> {arg ->
            def domainArg = (Domain) arg[0]
            assert domainArg.userAdminDN == promoteUser.getDn()
        }

        and: "Demote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(demoteUser, {it.name == DEFAULT_USER.roleName}, true)
        1 * tenantService.deleteTenantRole({it.name == "demoteRBAC"})
        1 * tenantService.deleteTenantRole({it.name == USER_ADMIN.roleName})
        0 * tenantService.deleteTenantRole({it.name == "demote"})

        and:
        response.status == HttpStatus.SC_NO_CONTENT

        and: "verify if cached role name is retrieved from applicationService"
        1 * applicationService.getCachedClientRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
        1 * applicationService.getCachedClientRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
    }

    def createTenantRole(String name, RoleTypeEnum roleType = STANDARD, Set<String> tenantIds = [] as Set) {
        return new TenantRole().with {
            it.name = name
            it.roleRsId = name // Just set id == name since fake roles anyway
            it.roleType = roleType
            it.tenantIds = tenantIds
            it
        }
    }

    def createImmutableClientRole(String name, int weight = 1000) {
        return new ImmutableClientRole(new ClientRole().with {
            it.name = name
            it.id = name // Just set id == name since fake roles anyway
            it.roleType = STANDARD
            it.rsWeight = weight
            it
        })
    }

    def createUser(String id = RandomStringUtils.randomAlphanumeric(10), String domain = "domainx", boolean enabled = true) {
        return new User().with{
            it.id = id
            it.enabled = enabled
            it.domainId = domain
            it
        }
    }

    def createUnverifiedUser(String id = RandomStringUtils.randomAlphanumeric(10), String domain = "domainx") {
        return new User().with{
            it.id = id
            it.enabled = false
            it.domainId = domain
            it.unverified = true
            it
        }
    }
}
