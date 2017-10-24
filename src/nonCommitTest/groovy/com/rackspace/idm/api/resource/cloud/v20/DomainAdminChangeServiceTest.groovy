package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
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
import testHelpers.RootIntegrationTest
import testHelpers.SingletonReloadableConfiguration

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

        defaultCloud20Service = new DefaultCloud20Service()
        defaultCloud20Service.authorizationService = authorizationService
        defaultCloud20Service.exceptionHandler = exceptionHandler
        defaultCloud20Service.requestContextHolder = requestContextHolder
        defaultCloud20Service.identityUserService = identityUserService
        defaultCloud20Service.tenantService = tenantService
        defaultCloud20Service.applicationService = applicationService
        defaultCloud20Service.atomHopperClient = atomHopperClient

        identityConfig = Mock(IdentityConfig)
        reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig.getReloadableConfig() >> reloadableConfig
        defaultCloud20Service.identityConfig = identityConfig

        // Common settings for the client role lookups. Based on convention for role names.
        authorizationService.getCachedIdentityRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
        authorizationService.getCachedIdentityRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)

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
        1 * securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()

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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById(_) >> {new User().with{
            it.enabled=true
            it.domainId="domainx" // Same domain
            it
        }}

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getRbacRolesForUserPerformant(_) >> userRoles
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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getRbacRolesForUserPerformant(promoteUser) >> [createTenantRole(USER_MANAGER.roleName)]
        tenantService.getRbacRolesForUserPerformant(demoteUser) >> userRoles
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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        tenantService.getRbacRolesForUserPerformant(promoteUser) >> promoteUserRoles
        tenantService.getRbacRolesForUserPerformant(demoteUser) >> demoteUserRoles
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
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest)

        then:
        1 * tenantService.getRbacRolesForUserPerformant(promoteUser) >> promoteUserRoles
        1 * tenantService.getRbacRolesForUserPerformant(demoteUser) >> demoteUserRoles
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

    def "Positive Test: Promoting user-manage to user-admin. No RBAC deletions"() {
        given:
        reloadableConfig.getCacheRolesWithoutApplicationRestartFlag() >> flag
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser")
        def demoteUser = createUser("demoteUser")

        tenantService.getRbacRolesForUserPerformant(promoteUser) >> [createTenantRole(DEFAULT_USER.roleName)]
        tenantService.getRbacRolesForUserPerformant(demoteUser) >> [createTenantRole(USER_ADMIN.roleName)]

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        def response = defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest).build()

        then: "Promote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(promoteUser, {it.name == USER_ADMIN.roleName})
        1 * tenantService.deleteTenantRole({it.name == DEFAULT_USER.roleName})

        and: "Demote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(demoteUser, {it.name == DEFAULT_USER.roleName})
        1 * tenantService.deleteTenantRole({it.name == USER_ADMIN.roleName})

        response.status == HttpStatus.SC_NO_CONTENT

        and: "verify if cached role name is retrieved from applicationService/authorizationService"
        if (flag) {
            1 * applicationService.getCachedClientRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
            1 * applicationService.getCachedClientRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
            0 * authorizationService.getCachedIdentityRoleByName(_)
            0 * authorizationService.getCachedIdentityRoleByName(_)
        } else {
            1 * authorizationService.getCachedIdentityRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
            1 * authorizationService.getCachedIdentityRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
            0 * applicationService.getCachedClientRoleByName(_)
            0 * applicationService.getCachedClientRoleByName(_)
        }

        where:
        flag << [true, false]
    }

    def "Positive Test: Promoting user-manage to user-admin. Deletes RBAC and user-classification roles from promote and demote users"() {
        given:
        reloadableConfig.getCacheRolesWithoutApplicationRestartFlag() >> flag
        def callerToken = "atoken"
        def promoteUser = createUser("promoteUser")
        def demoteUser = createUser("demoteUser")
        tenantService.getTenantRolesForUserPerformant(promoteUser) >> [createTenantRole("promoteRBAC"), createTenantRole("promote"),createTenantRole(DEFAULT_USER.roleName),createTenantRole(USER_MANAGER.roleName)]
        tenantService.getTenantRolesForUserPerformant(demoteUser) >> [createTenantRole("demoteRBAC"), createTenantRole("demote"), createTenantRole(USER_ADMIN.roleName)]

        DomainAdministratorChange changeRequest = new DomainAdministratorChange().with{
            it.promoteUserId = "promoteUser"
            it.demoteUserId = "demoteUser"
            it
        }
        securityContext.getAndVerifyEffectiveCallerToken(callerToken) >> new ScopeAccess()
        identityUserService.checkAndGetEndUserById("promoteUser") >> promoteUser
        identityUserService.checkAndGetEndUserById("demoteUser") >> demoteUser

        when:
        def response = defaultCloud20Service.modifyDomainAdministrator(callerToken, "domainx", changeRequest).build()

        then: "Promote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(promoteUser, {it.name == USER_ADMIN.roleName})
        1 * tenantService.deleteTenantRole({it.name == "promoteRBAC"})
        1 * tenantService.deleteTenantRole({it.name == DEFAULT_USER.roleName})
        1 * tenantService.deleteTenantRole({it.name == USER_MANAGER.roleName})
        0 * tenantService.deleteTenantRole({it.name == "promote"})

        and: "Demote users roles are properly modified"
        1 * tenantService.addTenantRoleToUser(demoteUser, {it.name == DEFAULT_USER.roleName})
        1 * tenantService.deleteTenantRole({it.name == "demoteRBAC"})
        1 * tenantService.deleteTenantRole({it.name == USER_ADMIN.roleName})
        0 * tenantService.deleteTenantRole({it.name == "demote"})

        and:
        response.status == HttpStatus.SC_NO_CONTENT

        and: "atom hopper client called to send feed events"
        1 * atomHopperClient.asyncPost(promoteUser, AtomHopperConstants.ROLE)
        1 * atomHopperClient.asyncPost(demoteUser, AtomHopperConstants.ROLE)

        and: "verify if cached role name is retrieved from applicationService/authorizationService"
        if (flag) {
            1 * applicationService.getCachedClientRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
            1 * applicationService.getCachedClientRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
            0 * authorizationService.getCachedIdentityRoleByName(_)
            0 * authorizationService.getCachedIdentityRoleByName(_)
        } else {
            1 * authorizationService.getCachedIdentityRoleByName(USER_ADMIN.roleName) >> createImmutableClientRole(USER_ADMIN.roleName, USER_ADMIN.levelAsInt)
            1 * authorizationService.getCachedIdentityRoleByName(DEFAULT_USER.roleName) >> createImmutableClientRole(DEFAULT_USER.roleName, DEFAULT_USER.levelAsInt)
            0 * applicationService.getCachedClientRoleByName(_)
            0 * applicationService.getCachedClientRoleByName(_)
        }

        where:
        flag << [true, false]
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
}
