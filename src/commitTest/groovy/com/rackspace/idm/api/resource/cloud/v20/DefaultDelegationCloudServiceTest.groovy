package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import static org.apache.http.HttpStatus.*

class DefaultDelegationCloudServiceTest extends RootServiceTest {

    @Shared
    DelegationCloudService service

    def setup() {
        service = new DefaultDelegationCloudService()

        mockIdentityConfig(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockDelegationService(service)
        mockUserGroupService(service)
        mockExceptionHandler(service)
        mockIdentityUserService(service)
        mockDelegationAgreementConverter(service)
        mockDomainService(service)
        mockIdmPathUtils(service)
        mockValidator20(service)
        mockRoleAssignmentConverter(service)
    }

    /**
     * Verifies each service performs that standard authorization on the passed in token appropriate. Each test will
     * fail for different reasons after the authorization checks. The tests also verify the standard exception handler
     * is called for each test. Each "where" entry must provide a closure that will cause the call to throw an exception.
     * The expected exception is specified to verify the method under test sends the exception to the standard
     * error handling routing.
     *
     * @return
     */
    @Unroll
    def "'#name' calls standard services to validate the provided token, the user, the user's authorization level and uses standard exception handling."() {
        given:
        reloadableConfig.isGlobalRootDelegationAgreementCreationEnabled() >> true

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it
        }
        def tokenStr = "callerTokenStr"
        def baseToken = Mock(BaseUserToken)
        def capturedException

        when:
        methodClosure(tokenStr)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> baseToken
        1 * baseToken.getScope() >> null
        1 * baseToken.isDelegationToken() >> false
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        and: "Appropriate exception thrown"
        IdmExceptionAssert.assertException(capturedException, expectedFailureExceptionClass, expectedErrorCode, IdmExceptionAssert.PATTERN_ALL)

        where:
        [name, methodClosure, expectedFailureExceptionClass, expectedErrorCode] << [
                ["addAgreement", { token -> service.addAgreement(Mock(UriInfo), token, new DelegationAgreement()) }, NotFoundException, "GEN-004"]
                , ["getAgreement", { token -> service.getAgreement(token, "id") }, NotFoundException, "GEN-004"]
                , ["deleteAgreement", { token -> service.deleteAgreement(token, "id") }, NotFoundException, "GEN-004"]
                , ["addDelegate", { token -> service.addDelegate(token, "id", new EndUserDelegateReference("user")) }, NotFoundException, "GEN-004"]
                , ["deleteDelegate", { token -> service.deleteDelegate(token, "id", new EndUserDelegateReference("user")) }, NotFoundException, "GEN-004"]
                , ["grantRolesToAgreement", { token -> service.grantRolesToAgreement(token, "id", new RoleAssignments()) }, NotFoundException, "GEN-004"]
                , ["revokeRoleFromAgreement", { token -> service.revokeRoleFromAgreement(token, "id", "roleId") }, NotFoundException, "GEN-004"]
                , ["listRoleAssignmentsOnAgreement", { token -> service.listRoleAssignmentsOnAgreement(Mock(UriInfo), token, "id", new DelegationAgreementRoleSearchParams(new PaginationParams())) }, NotFoundException, "GEN-004"]
                , ["listDelegationAgreements", { token -> service.listAgreements(token, "invalidRelationship") }, BadRequestException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE]
                , ["listDelegates", { token -> service.listDelegates(token, "id") }, NotFoundException, "GEN-004"]
                , ["updateAgreement", { token -> service.updateAgreement(token, new DelegationAgreement()) }, NotFoundException, "GEN-004"]
        ]
    }

    @Unroll
    def "'#name': Throws forbidden exception when scoped or delegate token is used"() {
        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        def tokenStr = "callerTokenStr"

        def baseToken = Mock(BaseUserToken)
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> baseToken

        when: "Delegation token"
        methodClosure(tokenStr)

        then: "rejected"
        1 * baseToken.isDelegationToken() >> true
        baseToken.getScope() >> ""
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        when: "Scoped token"
        methodClosure(tokenStr)

        then:
        1 * baseToken.getScope() >> "Something"
        baseToken.isDelegationToken() >> false
        exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        where:
        [name, methodClosure] << [
                ["addAgreement", {token -> service.addAgreement(Mock(UriInfo), token, new DelegationAgreement())}]
                , ["getAgreement", {token -> service.getAgreement(token, "id")}]
                , ["deleteAgreement", {token -> service.deleteAgreement(token, "id")}]
                , ["addDelegate", {token -> service.addDelegate(token, "id", new EndUserDelegateReference("user"))}]
                , ["deleteDelegate", {token -> service.deleteDelegate(token, "id", new EndUserDelegateReference("user"))}]
                , ["grantRolesToAgreement", {token -> service.grantRolesToAgreement(token, "id", new RoleAssignments())}]
                , ["revokeRoleFromAgreement", {token -> service.revokeRoleFromAgreement(token, "id", "roleId")}]
                , ["listRoleAssignmentsOnAgreement", {token -> service.listRoleAssignmentsOnAgreement(Mock(UriInfo), token, "id", new DelegationAgreementRoleSearchParams(new PaginationParams()))}]
                , ["listDelegationAgreements", {token -> service.listAgreements(token, "invalidRelationship")}]
                , ["listDelegates", {token -> service.listDelegates(token, "id")}]
                , ["updateAgreement", {token -> service.updateAgreement(token, new DelegationAgreement())}]
        ]
    }

    def "grantRolesToAgreement: calls appropriate services"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
                ta.tenantAssignment.add(new TenantAssignment().with {
                    it.onRole = "roleId"
                    it.forTenants.addAll("tenantId")
                    it
                })
            it.tenantAssignments = ta
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = caller.getDn()
            it
        }
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        when:
        def response = service.grantRolesToAgreement(tokenStr, daEntity.id, assignments)

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true
        1 * delegationService.replaceRoleAssignmentsOnDelegationAgreement(daEntity, assignments)
        1 * delegationService.getRoleAssignmentsOnDelegationAgreement(daEntity, _) >> []
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(_)
    }

    def "grantRolesToAgreement: error check"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
                ta.tenantAssignment.add(new TenantAssignment().with {
                    it.onRole = "roleId"
                    it.forTenants.addAll("tenantId")
                    it
                })
            it.tenantAssignments = ta
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)
        def invalidToken = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = caller.dn
            it
        }
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        when: "scoped token"
        invalidToken.getScope() >> "scope"
        service.grantRolesToAgreement(tokenStr, daEntity.id, assignments)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> invalidToken
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        when: "DA does not exist"
        service.grantRolesToAgreement(tokenStr, daEntity.id, assignments)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> null
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")
            Response.status(SC_NOT_FOUND)
        }

        when: "null roleAssignments"
        service.grantRolesToAgreement(tokenStr, daEntity.id, null)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true
        1 * exceptionHandler.exceptionResponse(_ as BadRequestException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, BadRequestException, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Must supply a set of assignments")
            Response.status(SC_BAD_REQUEST)
        }
    }

    @Unroll
    def "grantRolesToAgreement: Can only assign roles to nested agreements when enabled: enabled: #flag "() {
        reloadableConfig.canRolesBeAssignedToNestedDelegationAgreements() >> flag
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.parentDelegationAgreementId = "aParent"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = caller.dn
            it
        }
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        when: "nested agreement"
        service.grantRolesToAgreement(tokenStr, daEntity.id, null)

        then: "not allowed to assign roles"
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            if (!flag) {
                IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, DefaultDelegationCloudService.ERROR_MSG_NESTED_ROLE_ASSIGNMENT_FORBIDDEN)
            } else {
                // Fails due to missing role assignments
                IdmExceptionAssert.assertException(exception, BadRequestException, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Must supply a set of assignments")
            }
            Response.status(SC_BAD_REQUEST)
        }

        where:
        flag << [true, false]
    }

    def "revokeRoleFromAgreement: calls appropriate services"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        def roleId = "roleId"
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = caller.dn
            it
        }
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        when:
        def response = service.revokeRoleFromAgreement(tokenStr, daEntity.id, roleId)

        then:
        response.status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true
        1 * delegationService.revokeRoleAssignmentOnDelegationAgreement(daEntity, roleId)
    }

    def "revokeRoleFromAgreement: error check"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = domainId
            it
        }

        def roleId = "roleId"

        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)
        def invalidToken = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it
        }

        when: "scoped token"
        invalidToken.getScope() >> "scope"
        service.revokeRoleFromAgreement(tokenStr, daEntity.id, roleId)

        then:
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> invalidToken
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        when: "DA does not exist"
        service.revokeRoleFromAgreement(tokenStr, daEntity.id, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> null
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")
            Response.status(SC_NOT_FOUND)
        }
    }

    def "listRoleAssignmentsOnAgreement: calls appropriate services"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = caller.dn
            it
        }
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        UriInfo uriInfo = Mock()

        when:
        def response = service.listRoleAssignmentsOnAgreement(uriInfo, tokenStr, daEntity.id, new DelegationAgreementRoleSearchParams(new PaginationParams()))

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true
        1 * delegationService.getRoleAssignmentsOnDelegationAgreement(daEntity, _) >> new PaginatorContext<>()
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(_)
    }

    def "listRoleAssignmentsOnAgreement: error check"() {
        def domainId = "domainId"
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = domainId
            it
        }

        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)
        def invalidToken = Mock(BaseUserToken)

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it
        }

        UriInfo uriInfo = Mock()

        when: "scoped token"
        invalidToken.getScope() >> "scope"
        service.listRoleAssignmentsOnAgreement(uriInfo, tokenStr, daEntity.id, new DelegationAgreementRoleSearchParams(new PaginationParams()))

        then:
        daEntity.principal.getId() >> caller.id
        daEntity.principal.principalType >> PrincipalType.USER

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> invalidToken
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        when: "DA does not exist"
        service.listRoleAssignmentsOnAgreement(uriInfo, tokenStr, daEntity.id, new DelegationAgreementRoleSearchParams(new PaginationParams()))

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> null
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")
            Response.status(SC_NOT_FOUND)
        }
    }

    def "Verify the maximum allowed number of delegates for the DA to not be exceeded"() {
        given:
        reloadableConfig.getDelegationMaxNumberOfDelegatesPerDa(_) >> 1
        def domainId = RandomStringUtils.randomAlphabetic(10)
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = domainId
            it
        }

        def baseToken = Mock(BaseUserToken)
        def tokenStr = "callerTokenStr"
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "id"
            it.domainId = domainId
            it.principal = Mock(DelegationPrincipal)
            it.principalDN = new DN(caller.uniqueId)
            it.delegates = [new DN("rsId=1"), new DN("rsId=2")]
            it
        }

        DelegateReference delegateReference = new EndUserDelegateReference("user")
        def capturedException

        when:
        service.addDelegate(tokenStr, daEntity.id, delegateReference)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> baseToken
        1 * delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.isCallerAuthorizedToManageDelegationAgreement(_) >> true
        1 * delegationService.getDelegateByReference(_) >> caller
        1 * domainService.doDomainsShareRcn(_, _) >> true
        1 * exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_BAD_REQUEST) }
        IdmExceptionAssert.assertException(capturedException, BadRequestException, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED, IdmExceptionAssert.PATTERN_ALL)
    }
}


