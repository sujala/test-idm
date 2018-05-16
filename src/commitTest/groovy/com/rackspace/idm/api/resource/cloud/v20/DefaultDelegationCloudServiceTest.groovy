package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.DelegationPrincipal
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
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
                ["addAgreement", {token -> service.addAgreement(Mock(UriInfo), token, new DelegationAgreement())}, NotFoundException,"GEN-004"]
                , ["getAgreement", {token -> service.getAgreement(token, "id")}, NotFoundException, "GEN-004"]
                , ["deleteAgreement", {token -> service.deleteAgreement(token, "id")}, NotFoundException, "GEN-004"]
                , ["addDelegate", {token -> service.addDelegate(token, "id", new EndUserDelegateReference("user"))}, NotFoundException, "GEN-004"]
                , ["deleteDelegate", {token -> service.deleteDelegate(token, "id", new EndUserDelegateReference("user"))}, NotFoundException, "GEN-004"]
                , ["grantRolesToAgreement", {token -> service.grantRolesToAgreement(token, "id", new RoleAssignments())}, NotFoundException, "GEN-004"]
                , ["revokeRoleFromAgreement", {token -> service.revokeRoleFromAgreement(token, "id", "roleId")}, NotFoundException, "GEN-004"]
                , ["listRoleAssignmentsOnAgreement", {token -> service.listRoleAssignmentsOnAgreement(Mock(UriInfo), token, "id", new DelegationAgreementRoleSearchParams(new PaginationParams()))}, NotFoundException, "GEN-004"]
                , ["listDelegationAgreements", {token -> service.listAgreements(token, "invalidRelationship")}, BadRequestException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE]
                , ["listDelegates", {token -> service.listDelegates(token, "id")}, NotFoundException, "GEN-004"]
                , ["updateAgreement", {token -> service.updateAgreement(token, new DelegationAgreement())}, NotFoundException, "GEN-004"]
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

    @Unroll
    def "addAgreement: Validates name and description length"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        DelegationAgreement daInvalidWeb = new DelegationAgreement()

        def invalidName = RandomStringUtils.randomAlphabetic(33)
        def validName = RandomStringUtils.randomAlphabetic(32)
        def invalidDescription = RandomStringUtils.randomAlphabetic(256)

        when: "Don't Provide name"
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", null, 32) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide name exceeding 32"
        daInvalidWeb.setName(invalidName)
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", invalidName, 32) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide description exceeding 255"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription(invalidDescription)
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateStringMaxLength("description", invalidDescription, 255) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
    }

    def "addAgreement: Success when set the USER principal to be the same user as the caller"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        def capturedException
        exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        when: "add agreement with principal as caller"
        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = caller.id
            it
        }
        def response = service.addAgreement(uriInfo, token, daValidWeb)

        then:
        1 * identityUserService.getEndUserById(caller.id) >> caller
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationAgreementConverter.toDelegationAgreementWeb(_) >> new DelegationAgreement()
        response.status == SC_CREATED
    }

    @Unroll
    def "addAgreement Error: Error when specify a USER principal that is other than caller"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        when:
        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = principalUser.id
            it
        }
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * identityUserService.getEndUserById(principalUser.id) >> principalUser
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }

        where:
        principalUser << [new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        },new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domain2"
            it
        },new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = null
            it
        }
        ]
    }

    def "addAgreement Error: Error when specified USER principal does not exist"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = RandomStringUtils.randomAlphabetic(10)
            it
        }

        when:
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * identityUserService.getEndUserById(daInvalidWeb.principalId) >> null
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }
    }

    def "addAgreement: Can specify user group principal when caller is member"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5

        UserGroup ug = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "group=${RandomStringUtils.randomAlphanumeric(8)}"
            it
        }

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.getUserGroupDNs().add(ug.getGroupDn())
            it.domainId = ug.domainId
            it
        }

        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        delegationService.getDelegateByReference(_) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = ug.id
            it
        }

        when: "add agreement with user group principal that member of"
        def response = service.addAgreement(uriInfo, token, daValidWeb)

        then: "created successfully"
        1 * userGroupService.getGroupById(ug.id) >> ug
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationAgreementConverter.toDelegationAgreementWeb(_) >> new DelegationAgreement()
        response.status == SC_CREATED
    }

    @Unroll
    def "addAgreement user group errors: Verifies USER GROUP principal must be valid for caller"() {
        def principalId = userGroup != null ? userGroup.id : "group=agroupOther"

        userGroupService.getGroupById(principalId) >> userGroup

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it.userGroupDNs.add("group=agroup)")
            it
        }

        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        when: "add agreement with user group principal that user is not member of"
        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = principalId
            it
        }
        service.addAgreement(uriInfo, token, daValidWeb)

        then: "Get error"
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }

        where:
        userGroup << [
                // Group in different domain
                new UserGroup().with {
                    it.id = RandomStringUtils.randomAlphabetic(10)
                    it.domainId = RandomStringUtils.randomAlphabetic(10)
                    it.uniqueId = "group=agroup"
                    it
                },
                // Different group
                new UserGroup().with {
                    it.id = RandomStringUtils.randomAlphabetic(10)
                    it.domainId = "callerDomain"
                    it.uniqueId = "group=agroupOther"
                    it
                },
                // group doesn't exist
                null
        ]
    }

    /**
     * Tests that appropriate authorization is performed that the principal is allowed to delegate to the specified delegate
     * by calling the standard check.
     *
     * @return
     */
    @Unroll
    def "createDelegationAgreement: Verify authorization for specified delegate is performed"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement daWeb = new DelegationAgreement().with {
            it
        }
        def token = "token"
        User caller = new User().with {
            it.id = "callerId"
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        identityUserService.getEndUserById(caller.id) >> caller

        User delegate = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        def callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement()

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5
        delegationService.getDelegateByReference(_) >> delegate

        when: "Delegate belongs to same RCN"
        def response = service.addAgreement(uriInfo, token, daWeb)

        then:
        response.status == SC_CREATED
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(daWeb) >> daEntity
        1 * delegationService.addDelegationAgreement(daEntity)
        1 * delegationAgreementConverter.toDelegationAgreementWeb(daEntity) >> daWeb
    }

    def "addDelegationAgreement: Error when maximum number of DAs are created for principal"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement daWeb = new DelegationAgreement().with {
            it
        }
        def token = "token"
        User caller = new User().with {
            it.id = "callerId"
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        identityUserService.getEndUserById(caller.id) >> caller

        def callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 1

        when:
        service.addAgreement(uriInfo, token, daWeb)

        then:
        1 * delegationService.countNumberOfDelegationAgreementsByPrincipal(_) >> 1
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0]
                    , BadRequestException
                    , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                    , "Maximum number of delegation agreements has been reached for principal")
            return Response.status(SC_BAD_REQUEST)
        }
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
        1 * delegationService.replaceRoleAssignmentsOnDelegationAgreement(daEntity, assignments)
        1 * delegationService.getRoleAssignmentsOnDelegationAgreement(daEntity, _) >> []
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(_)
    }

    def "grantRolesToAgreement: authorized caller is not an effective principal on the DA"() {
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
            it.principalDN = new DN("rsId=otherDn")
            it
        }
        daEntity.principal.getId() >> "otherId"
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
        1 * exceptionHandler.exceptionResponse(_ as BadRequestException) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, BadRequestException, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Must supply a set of assignments")
            Response.status(SC_BAD_REQUEST)
        }
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

    def "updateAgreement: calls appropriate services"() {
        given:
        def token = "token"
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement da = new DelegationAgreement()
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it
        }


        when:
        service.updateAgreement(token, da)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(_) >> daEntity
        1 * delegationService.updateDelegationAgreement(daEntity)
    }

    def "updateAgreement: error check"() {
        given:
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it
        }
        delegationService.getDelegationAgreementById(_) >> daEntity

        DelegationAgreement daInvalidWeb = new DelegationAgreement()

        def invalidName = RandomStringUtils.randomAlphabetic(33)
        def validName = RandomStringUtils.randomAlphabetic(32)
        def invalidDescription = RandomStringUtils.randomAlphabetic(256)
        def validDescription = RandomStringUtils.randomAlphabetic(32)

        when: "name exceeding 32"
        daInvalidWeb.setName(invalidName)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", invalidName, 32) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "description exceeding 255"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription(invalidDescription)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateStringMaxLength("description", invalidDescription, 255) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * delegationService.updateDelegationAgreement(_)

        when: "da not found"
        daInvalidWeb.setDescription(validDescription)
        daInvalidWeb.setId("invalid")
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * delegationService.getDelegationAgreementById("invalid") >> null
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        0 * validator20.validateStringMaxLength("description", invalidDescription, 255)
        0 * delegationService.updateDelegationAgreement(_)

        when: "caller is a delegate"
        daEntity.setPrincipalDN(new DN("rsId=other"))
        service.updateAgreement(token, new DelegationAgreement())

        then:
        1 * delegationService.getDelegationAgreementById(_) >> daEntity
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        0 * validator20.validateStringMaxLength("description", invalidDescription, 255)
        0 * delegationService.updateDelegationAgreement(_)
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
        1 * delegationService.getDelegateByReference(_) >> caller
        1 * domainService.doDomainsShareRcn(_, _) >> true
        1 * exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_BAD_REQUEST) }
        IdmExceptionAssert.assertException(capturedException, BadRequestException, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED, IdmExceptionAssert.PATTERN_ALL)
    }
}


