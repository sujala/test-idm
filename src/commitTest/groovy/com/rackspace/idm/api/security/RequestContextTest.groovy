package com.rackspace.idm.api.security

import com.rackspace.idm.api.security.AuthorizationContext
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerSourcedRoleAssignmentsBuilder
import com.rackspace.idm.domain.entity.SourcedRoleAssignments
import com.rackspace.idm.domain.entity.User
import testHelpers.RootServiceTest

class RequestContextTest extends RootServiceTest {
    RequestContext requestContext

    def setup() {
        requestContext = new RequestContext()
        securityContext = Mock(SecurityContext)
        requestContext.setSecurityContext(securityContext)

        mockUserService(requestContext)
        mockDomainService(requestContext)
        mockTenantService(requestContext)
        mockAuthorizationService(requestContext)
        mockApplicationService(requestContext)
        mockIdentityConfig(requestContext)
    }

    def "getEffectiveCallerAuthorizationContext: when racker caller, populates context with racker roles"() {
        given:
        Racker racker = entityFactory.createRacker("rackerId")
        ImmutableClientRole icr = new ImmutableClientRole(new ClientRole().with {
            it.name = "Racker"
            it.id = "Racker"
            it
        })
        SourcedRoleAssignments sourcedRoleAssignments = new RackerSourcedRoleAssignmentsBuilder(racker).addIdentitySystemSourcedAssignment(icr).build()
        def methodSetAuthContext;

        when:
        AuthorizationContext authorizationContext = requestContext.getEffectiveCallerAuthorizationContext()

        then:
        1 * securityContext.getEffectiveCallerAuthorizationContext() >> null
        1 * securityContext.getEffectiveCaller() >> racker
        1 * tenantService.getSourcedRoleAssignmentsForRacker(racker) >> sourcedRoleAssignments
        1 * securityContext.setEffectiveCallerAuthorizationContext(_) >> {args ->
            methodSetAuthContext = args[0]
            return
        }

        and:
        authorizationContext.hasRoleWithName(icr.name)
        authorizationContext.hasRoleWithId(icr.id)
        authorizationContext.getIdentityUserType() == null // Rackers don't have a user type

        and:
        methodSetAuthContext == authorizationContext
    }

    def "getEffectiveCallerAuthorizationContext: Context only populated if does not exist in security context"() {
        given:
        AuthorizationContext context = new AuthorizationContext(new SourcedRoleAssignments(new User()))

        when:
        AuthorizationContext returnedContext = requestContext.getEffectiveCallerAuthorizationContext()

        then:
        1 * securityContext.getEffectiveCallerAuthorizationContext() >> context
        0 * securityContext.getEffectiveCaller()

        and: "returns the same context object"
        returnedContext == context
    }
}
