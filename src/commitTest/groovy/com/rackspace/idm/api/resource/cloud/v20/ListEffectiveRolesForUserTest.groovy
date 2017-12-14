package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.domain.entity.SourcedRoleAssignments
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import org.opensaml.core.config.InitializationService
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

class ListEffectiveRolesForUserTest extends RootServiceTest {
    DefaultCloud20Service service

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        //service being tested
        service = new DefaultCloud20Service()

        mockRequestContextHolder(service)
        mockUserService(service)
        mockAuthorizationService(service)
        mockPrecedenceValidator(service)
        mockTenantService(service)
        mockExceptionHandler(service)
        mockRoleAssignmentConverter(service)
    }

    /**
     * Verifies service calls standard authorization services to:
     * 1. Verify token is still valid
     * 2. Caller is authorized to call the service
     * 3. Caller is authorized to call the service on the specified user
     */
    def "grantRoleAssignments: Calls appropriate authorization services and exception handler"() {
        given:
        def user = new User().with {
            it.id = "targetId"
            it
        }

        def caller = new User().with {
            it.id = "callerId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def params = new ListEffectiveRolesForUserParams()

        when:
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * requestContext.getEffectiveCaller() >> caller
        precedenceValidator.verifyCallerPrecedenceOverUser(caller, user) >> { throw new ForbiddenException() }
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "grantRoleAssignments: When calling on self, the precedence validator is not called"() {
        given:
        def user = new User().with {
            it.id = "userId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def params = new ListEffectiveRolesForUserParams()

        when:
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * requestContext.getEffectiveCaller() >> user
        0 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        0 * idmExceptionHandler.exceptionResponse(_)
    }

    def "grantRoleAssignments: Calls appropriate processing services"() {
        given:
        def user = new User().with {
            it.id = "targetId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def params = new ListEffectiveRolesForUserParams()

        SourcedRoleAssignments assignments = new SourcedRoleAssignments()

        // Standard mocks to get past authorization
        securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        requestContext.getAndVerifyEffectiveCallerIsEnabled()
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        userService.checkAndGetUserById(user.id) >> user
        requestContext.getEffectiveCaller() >> user

        when:
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(assignments) >> new RoleAssignments()
    }
}
