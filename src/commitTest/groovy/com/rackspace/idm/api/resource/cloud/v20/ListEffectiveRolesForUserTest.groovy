package com.rackspace.idm.api.resource.cloud.v20

import com.google.common.collect.Sets
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.ProvisionedUserDelegate
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.SourcedRoleAssignments
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.DomainSubUserDefaults
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import org.apache.commons.lang3.RandomStringUtils
import org.opensaml.core.config.InitializationService
import testHelpers.EntityFactory
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

    def "listEffectiveRolesForUser: filter assignments using onTenantId query param"() {
        given:
        def user = new User().with {
            it.id = "targetId"
            it
        }

        def token = "token"
        def headers = Mock(HttpHeaders)
        def tenantId = "tenantId"

        // Create roles for assignments
        SourcedRoleAssignments assignments = new SourcedRoleAssignments(user)

        ClientRole clientRole = entityFactory.createClientRole().with {
            it.name = "role1"
            it.id = "id1"
            it
        }
        ImmutableClientRole immutableClientRole = new ImmutableClientRole(clientRole)

        ClientRole clientRole2 = entityFactory.createClientRole().with {
            it.name = "role2"
            it.id = "id2"
            it
        }
        ImmutableClientRole immutableClientRole2 = new ImmutableClientRole(clientRole2)

        // Add user source Assignment
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
            assert roleAssignment.sources.size() == 1
            assert roleAssignment.tenantIds.size() == 1
            assert roleAssignment.tenantIds.iterator().next() == tenantId
            assert roleAssignment.role.id == clientRole.id

            new RoleAssignments()
        }

        when: "case insensitive"
        params = new ListEffectiveRolesForUserParams(tenantId.toUpperCase())
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(_) >> { SourcedRoleAssignments sra ->
            Set<SourcedRoleAssignments.SourcedRoleAssignment> sraSet = sra.getSourcedRoleAssignments()
            assert sraSet.size() == 1
            SourcedRoleAssignments.SourcedRoleAssignment roleAssignment = sraSet.iterator().next();
            assert roleAssignment.sources.size() == 1
            assert roleAssignment.tenantIds.size() == 1
            assert roleAssignment.tenantIds.iterator().next() == tenantId
            assert roleAssignment.role.id == clientRole.id

            new RoleAssignments()
        }

        when: "role assignments having multiple sources on tenant"
        SourcedRoleAssignments.Source source = new SourcedRoleAssignments.Source(SourcedRoleAssignments.SourceType.USERGROUP, "groupId", SourcedRoleAssignments.AssignmentType.TENANT, Sets.newHashSet(tenantId))
        assignments.sourcedRoleAssignments.iterator().next().addAdditionalSource(source)
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(_) >> { SourcedRoleAssignments sra ->
            Set<SourcedRoleAssignments.SourcedRoleAssignment> sraSet = sra.getSourcedRoleAssignments()
            assert sraSet.size() == 1
            SourcedRoleAssignments.SourcedRoleAssignment roleAssignment = sraSet.iterator().next();
            assert roleAssignment.sources.size() == 2
            assert roleAssignment.sources.find {it.sourceId == user.id} != null
            assert roleAssignment.sources.find {it.sourceId == "groupId"} != null
            assert roleAssignment.tenantIds.size() == 1
            assert roleAssignment.tenantIds.iterator().next() == tenantId
            assert roleAssignment.role.id == clientRole.id

            new RoleAssignments()
        }

        when: "having multiple roles on the same tenant"
        assignments.addUserSourcedAssignment(immutableClientRole2, SourcedRoleAssignments.AssignmentType.TENANT, Sets.newHashSet("t1", tenantId))
        service.listEffectiveRolesForUser(headers, token, user.id, params)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> assignments
        1 * roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(_) >> { SourcedRoleAssignments sra ->
            Set<SourcedRoleAssignments.SourcedRoleAssignment> sraSet = sra.getSourcedRoleAssignments()
            assert sraSet.size() == 2

            SourcedRoleAssignments.SourcedRoleAssignment ra1 = sraSet.find {it.role.id == clientRole.id}
            assert ra1.tenantIds.size() == 1
            assert ra1.sources.size() == 2

            SourcedRoleAssignments.SourcedRoleAssignment ra2 = sraSet.find {it.role.id == clientRole2.id}
            assert ra2.tenantIds.size() == 1
            assert ra2.sources.size() == 1

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

    def "listEffectiveRolesForUser: lists the role assignments from the DA if user ID matches the token's user"() {
        given:
        def targetUser = entityFactory.createUser().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def otherUser = entityFactory.createUser().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def domain = entityFactory.createDomain()
        def tokenString = "token"
        def daTokenString = "daToken"
        def delegationAgreement = new DelegationAgreement().with {
            it.id = new RandomStringUtils().randomAlphanumeric(8)
            it.domainId = domain.domainId
            it
        }
        List<TenantRole> tenantRoles = []
        def subUserDefaults = new DomainSubUserDefaults(domain, targetUser.getRsGroupId(), targetUser.getRegion(), tenantRoles)
        def delegate = new ProvisionedUserDelegate(subUserDefaults, delegationAgreement, targetUser)
        def delegateToken = new UserScopeAccess().with {
            it.delegationAgreementId = delegationAgreement.id
            it.userRsId = targetUser.id
            it.accessTokenString = daTokenString
            it
        }
        def token = new UserScopeAccess().with {
            it.userRsId = targetUser.id
            it.accessTokenString = tokenString
            it
        }
        def headers = Mock(HttpHeaders)
        userService.checkAndGetUserById(targetUser.id) >> targetUser
        userService.checkAndGetUserById(otherUser.id) >> otherUser
        requestContextHolder.getRequestContext().getEffectiveCaller() >> delegate

        when: "list roles for self using DA token"
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> delegateToken
        def response = service.listEffectiveRolesForUser(headers, daTokenString, targetUser.id,  new ListEffectiveRolesForUserParams())
        def builtResponse = response.build()

        then:
        builtResponse.status == 200
        1 * tenantService.getSourcedRoleAssignmentsForUser(delegate)
        0 * tenantService.getSourcedRoleAssignmentsForUser(targetUser)

        when: "list roles for self using non-DA token"
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> token
        response = service.listEffectiveRolesForUser(headers, tokenString, targetUser.id,  new ListEffectiveRolesForUserParams())
        builtResponse = response.build()

        then:
        builtResponse.status == 200
        0 * tenantService.getSourcedRoleAssignmentsForUser(delegate)
        1 * tenantService.getSourcedRoleAssignmentsForUser(targetUser)

        when: "list roles for other user using DA token"
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> delegateToken
        response = service.listEffectiveRolesForUser(headers, daTokenString, otherUser.id,  new ListEffectiveRolesForUserParams())
        builtResponse = response.build()

        then:
        builtResponse.status == 200
        0 * tenantService.getSourcedRoleAssignmentsForUser(delegate)
        0 * tenantService.getSourcedRoleAssignmentsForUser(targetUser)
        1 * tenantService.getSourcedRoleAssignmentsForUser(otherUser)
    }

}
