package com.rackspace.idm.api.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.Domain
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
        ctx.setCallerToken(scopeAccessService.getScopeAccessByAccessToken(iaToken))
        requestContext.getEffectiveCaller();

        then:
        thrown(IllegalStateException)

        when: "set the effective token and then try to load the effective caller"
        ctx.setEffectiveCallerToken(scopeAccessService.getScopeAccessByAccessToken(uaToken))
        BaseUser effectiveCaller = requestContext.getEffectiveCaller();

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
        requestContext.getEffectiveCaller();

        then:
        thrown(IllegalStateException)

        when: "try to load domain with only caller token set"
        ctx.setCallerToken(scopeAccessService.getScopeAccessByAccessToken(iaToken))
        requestContext.getEffectiveCaller();

        then:
        thrown(IllegalStateException)

        when: "load the domain via the request context after setting effective token"
        ctx.setEffectiveCallerToken(scopeAccessService.getScopeAccessByAccessToken(uaToken))
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
        requestContext.getEffectiveCaller();

        then:
        thrown(IllegalStateException)

        when: "try to load authorization context with only caller token set"
        ctx.setCallerToken(scopeAccessService.getScopeAccessByAccessToken(uaToken))
        requestContext.getEffectiveCallerAuthorizationContext();

        then:
        thrown(IllegalStateException)

        when: "load the authorization context via the request context after setting effective token"
        ctx.setEffectiveCallerToken(scopeAccessService.getScopeAccessByAccessToken(uaToken))
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
    }


}
