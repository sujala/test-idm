package com.rackspace.idm.api.resource.cloud.v20

import com.google.common.collect.Sets
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.ClientRole
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
        mockPrecedenceValidator(service)
    }

    /**
     * Verifies service calls standard authorization services to:
     * 1. Verify token is still valid
     * 2. Caller is authorized to call the service
     * 3. Caller is authorized to call the service on the specified user
     */
    def "listEffectiveRolesForUser: Calls appropriate authorization services and exception handler"() {
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
        def params = new ListEffectiveRolesForUserParams(null)

        when:
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * precedenceValidator.verifyCallerCanListRolesForUser(caller, user) >> { args -> throw new ForbiddenException() }
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * requestContext.getEffectiveCaller() >> caller
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "listEffectiveRolesForUser: Calls appropriate processing services"() {
        given:
        def user = new User().with {
            it.id = "targetId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def params = new ListEffectiveRolesForUserParams(null)

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

    def "listEffectiveRolesForUser: Calls appropriate processing services when using onTenantId query param"() {
        given:
        def user = new User().with {
            it.id = "targetId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def tenantId = "tenantId"

        // Create source role assignments
        SourcedRoleAssignments assignments = new SourcedRoleAssignments(user)

        ClientRole clientRole = entityFactory.createClientRole()
        ImmutableClientRole immutableClientRole = new ImmutableClientRole(clientRole)

        assignments.addUserSourcedAssignment(immutableClientRole, SourcedRoleAssignments.AssignmentType.TENANT, Sets.newHashSet("t1", tenantId))

        // Standard mocks to get past authorization
        securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        requestContext.getAndVerifyEffectiveCallerIsEnabled()
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        userService.checkAndGetUserById(user.id) >> user
        requestContext.getEffectiveCaller() >> user

        when: "valid tenantId"
        def params = new ListEffectiveRolesForUserParams(tenantId)
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(_) >> { SourcedRoleAssignments sra ->
            Set<SourcedRoleAssignments.SourcedRoleAssignment> sraSet = sra.getSourcedRoleAssignments()
            assert sraSet.size() == 1
            SourcedRoleAssignments.SourcedRoleAssignment roleAssignment = sraSet.iterator().next();
            assert roleAssignment.tenantIds.size() == 1
            assert roleAssignment.tenantIds.iterator().next() == tenantId
            assert roleAssignment.role.id == clientRole.id

            new RoleAssignments()
        }

        when: "invalid tenantId"
        params = new ListEffectiveRolesForUserParams("invalid")
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(_) >> { SourcedRoleAssignments sra ->
            Set<SourcedRoleAssignments.SourcedRoleAssignment> sraSet = sra.getSourcedRoleAssignments()
            assert sraSet.size() == 0
            new RoleAssignments()
        }
    }
}
