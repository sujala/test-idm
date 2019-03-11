package com.rackspace.idm.api.security

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import org.openstack.docs.identity.api.v2.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import testHelpers.RootIntegrationTest

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class RequestContextIntegrationTest extends RootIntegrationTest {

    @Autowired
    RequestContext requestContext

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ApplicationService applicationService

    def "can load effective caller from request context when token set on security context"() {
        given:
        utils.createUserAdmin()
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()

        def uaToken = utils.getToken(userAdmin.username)
        def iaToken = utils.getToken(users[0].username)

        SecurityContext ctx = new SecurityContext()
        requestContext.setSecurityContext(ctx)

        when: "try to load user with no token set"
        requestContext.getEffectiveCaller();

        then:
        thrown(IllegalStateException)

        when: "try to load effective user with only caller token set"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(iaToken), null)
        requestContext.getEffectiveCaller()

        then:
        thrown(IllegalStateException)

        when: "set the effective token and then try to load the effective caller"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(iaToken), scopeAccessService.getScopeAccessByAccessToken(uaToken))
        BaseUser effectiveCaller = requestContext.getEffectiveCaller()

        then: "get the appropriate user"
        effectiveCaller.id == userAdmin.id

        and: "security context is now populated"
        ctx.effectiveCaller == effectiveCaller

        and: "security context reports impersonation"
        ctx.isImpersonatedRequest()

        cleanup:
        utils.deleteUsers(users)
    }

    def "can load effective caller domain from request context when token set on security context"() {
        given:
        utils.createUserAdmin()
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()

        def uaToken = utils.getToken(userAdmin.username)
        def iaToken = utils.getToken(users[0].username)

        SecurityContext ctx = new SecurityContext()
        requestContext.setSecurityContext(ctx)

        when: "try to load domain with no token set"
        requestContext.getEffectiveCaller()

        then:
        thrown(IllegalStateException)

        when: "try to load domain with only caller token set"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(iaToken), null)
        requestContext.getEffectiveCaller()

        then:
        thrown(IllegalStateException)

        when: "load the domain via the request context after setting effective token"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(iaToken), scopeAccessService.getScopeAccessByAccessToken(uaToken))
        Domain domain = requestContext.getEffectiveCallerDomain()

        then: "get the appropriate user"
        domain.domainId == userAdmin.domainId

        and: "security context is now populated"
        ctx.effectiveCallerDomain == domain

        and: "security context has user"
        ctx.effectiveCaller.id == userAdmin.id

        cleanup:
        utils.deleteUsers(users)
    }

    def "can load effective caller roles from request context when token set on security context"() {
        given:
        utils.createUserAdmin()
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()

        def uaToken = utils.getToken(userAdmin.username)
        def iaToken = utils.getToken(users[0].username)

        SecurityContext ctx = new SecurityContext()
        requestContext.setSecurityContext(ctx)

        when: "try to load authorization context with no token set"
        requestContext.getEffectiveCaller()

        then:
        thrown(IllegalStateException)

        when: "try to load authorization context with only caller token set"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(uaToken), null)
        requestContext.getEffectiveCallerAuthorizationContext()

        then:
        thrown(IllegalStateException)

        when: "load the authorization context via the request context after setting effective token"
        ctx.setCallerTokens(scopeAccessService.getScopeAccessByAccessToken(uaToken), scopeAccessService.getScopeAccessByAccessToken(uaToken))
        AuthorizationContext authCtx = requestContext.getEffectiveCallerAuthorizationContext()

        then: "get the authCtx"
        authCtx != null

        and: "security context is now populated"
        ctx.effectiveCallerAuthorizationContext == authCtx

        when: "get user roles via list roles call"
        List<Role> roleList = utils.listUserGlobalRoles(iaToken, userAdmin.id).role

        then: "ctx shows effective caller has same identity roles"
        for (Role grantedRole : roleList) {
            if (grantedRole.name.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)) {
                authCtx.hasRoleWithName(grantedRole.name) != null
                authCtx.hasRoleWithName(grantedRole.id) != null
            }
        }

        cleanup:
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    def "getEffectiveCallerAuthorizationContext: Provisioned users receive implicit roles"() {
        given:
        utils.createUserAdmin()
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()
        def reposeRole = applicationService.getCachedClientRoleByName(IdentityRole.REPOSE_STANDARD.getRoleName())

        def uaToken = utils.getToken(userAdmin.username)
        def sa = scopeAccessService.getScopeAccessByAccessToken(uaToken)
        requestContext.setSecurityContext(createSecurityContext(sa, sa))

        when:
        AuthorizationContext authCtx = requestContext.getEffectiveCallerAuthorizationContext()

        then: "Does not have the roles implicitly assigned"
        !authCtx.hasRoleWithName(IdentityRole.VALIDATE_TOKEN_GLOBAL.roleName)
        !authCtx.hasRoleWithName(IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.roleName)
        !authCtx.hasRoleWithName(IdentityRole.GET_USER_GROUPS_GLOBAL.roleName)
        !authCtx.hasRoleWithName(IdentityRole.GET_USER_ROLES_GLOBAL.roleName)

        when: "add repose-standard role and reset"
        requestContext.setSecurityContext(createSecurityContext(sa, sa))
        utils.addRoleToUser(userAdmin, reposeRole.id)
        authCtx = requestContext.getEffectiveCallerAuthorizationContext()

        then: "implicit tokens are populated"
        authCtx.hasRoleWithName(IdentityRole.VALIDATE_TOKEN_GLOBAL.roleName)
        authCtx.hasRoleWithName(IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.roleName)
        authCtx.hasRoleWithName(IdentityRole.GET_USER_GROUPS_GLOBAL.roleName)
        authCtx.hasRoleWithName(IdentityRole.GET_USER_ROLES_GLOBAL.roleName)

        cleanup:
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    def "getEffectiveCallerAuthorizationContext: Rackers without applicable group don't get implicit roles"() {
        given:
        def rackerNoGroupsToken = utils.authenticateRacker(Constants.RACKER_NOGROUP, Constants.RACKER_NOGROUP_PASSWORD).token.id
        def rackerNoGroupsSa = scopeAccessService.getScopeAccessByAccessToken(rackerNoGroupsToken)
        requestContext.setSecurityContext(createSecurityContext(rackerNoGroupsSa, rackerNoGroupsSa))

        when:
        AuthorizationContext authCtx = requestContext.getEffectiveCallerAuthorizationContext()

        then: "Does not have the roles implicitly assigned to impersonator AD group"
        authCtx.hasRoleWithName(GlobalConstants.ROLE_NAME_RACKER)
        !authCtx.hasRoleWithName(IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
    }

    def "getEffectiveCallerAuthorizationContext: Rackers with mapped group receive implicit roles"() {
        given:
        def rackerImpersonateGroupToken = utils.authenticateRacker(Constants.RACKER_IMPERSONATE, Constants.RACKER_IMPERSONATE_PASSWORD).token.id
        def rackerImpersonateGroupSa = scopeAccessService.getScopeAccessByAccessToken(rackerImpersonateGroupToken)

        requestContext.setSecurityContext(createSecurityContext(rackerImpersonateGroupSa, rackerImpersonateGroupSa))

        when:
        AuthorizationContext authCtx = requestContext.getEffectiveCallerAuthorizationContext()

        then: "implicit tokens are populated"
        authCtx.hasRoleWithName(GlobalConstants.ROLE_NAME_RACKER)
        authCtx.hasRoleWithName(IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
    }

    def createSecurityContext(ScopeAccess callerToken, ScopeAccess effectiveCallerToken) {
        SecurityContext ctx = new SecurityContext()
        ctx.setCallerTokens(callerToken, effectiveCallerToken)
        return ctx
    }

}
