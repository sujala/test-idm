package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.DelegateType
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.Token
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.rackspace.idm.validation.Validator20
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_NOT_FOUND

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
    }

    /**
     * Verifies the request is authorized appropriately. Fails the check to verify domain to test for
     * standard exception handling.
     *
     * @return
     */
    def "addAgreement: Verifies token and caller with standard exception handling"() {
        DelegationAgreement daWeb = new DelegationAgreement()
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it
        }
        Domain callerDomain = new Domain().with {
            it.rackspaceCustomerNumber = "myRcn"
            it
        }
        UriInfo uriInfo = Mock()
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        def capturedException
        exceptionHandler.exceptionResponse(_ as ForbiddenException) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        when:
        service.addAgreement(uriInfo, tokenStr, daWeb)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getEndUserById(caller.id) >> caller
        1 * requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        1 * reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> false

        and: "Appropriate exception thrown"
        IdmExceptionAssert.assertException(capturedException, ForbiddenException, ErrorCodes.ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN, IdmExceptionAssert.PATTERN_ALL)
    }

    def "addAgreement: Verifies token is not scoped or delegate token"() {
        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true

        DelegationAgreement daWeb = new DelegationAgreement()
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it
        }
        Domain callerDomain = new Domain().with {
            it.rackspaceCustomerNumber = "myRcn"
            it
        }

        UriInfo uriInfo = Mock()
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(caller.id) >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        when: "Delegation token"
        service.addAgreement(uriInfo, tokenStr, daWeb)

        then:
        token.getScope() >> ""
        1 * token.isDelegationToken() >> true

        and: "Appropriate exception thrown"
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }

        when: "Scoped token"
        service.addAgreement(uriInfo, tokenStr, daWeb)

        then:
        1 * token.getScope() >> "Something"
        token.isDelegationToken() >> true

        and: "Appropriate exception thrown"
        exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
            Response.status(SC_FORBIDDEN)
        }
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

    def "addAgreement: Success when specified USER principal is same as caller"() {
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
            it.delegateId = caller.id // Set delegate to caller
            it
        }
        def response = service.addAgreement(uriInfo, token, daValidWeb)

        then:
        1 * identityUserService.getEndUserById(caller.id) >> caller
        1 * delegationService.getDelegateByReference(_) >> {args ->
            DelegateReference delegateReference = args[0]
            assert delegateReference.id == caller.id
            assert delegateReference.delegateType == DelegateType.USER
            caller
        }
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationAgreementConverter.toDelegationAgreementWeb(_) >> new DelegationAgreement()
        response.status == SC_CREATED
    }

    @Unroll
    def "addAgreement Error: Error when specified USER principal not same as caller"() {
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
            it.delegateId = caller.id // Set delegate to caller
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
            it.delegateId = caller.id // Set delegate to caller
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
            it.delegateId = caller.id // Set delegate to caller
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
            it.delegateId = caller.id // Set delegate to caller
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
     * Tests logic verifying the principal can delegate to the specified user. The delegate is generally required to be within
     * the same RCN as the user (principal and delegate domains have the same non-blank RCN value). However, when rcns
     * are globally enabled, we also allow the principal and delegate to belong to the same domain even if the RCN for
     * the domain is blank.
     *
     * Other tests verify when RCNs are NOT globally enabled, that the domains must contain RCNs. This test assumes that
     * the callers RCN is allowed to create DAs (because it's in the list, or globally enabled).
     *
     * This test focuses on the valid cases
     *
     * @return
     */
    @Unroll
    def "createDelegationAgreement: Verify allowable cases of delegate to principal - callerDomainId: '#callerDomainId'; callerRcn: '#callerRcn'; delegateDomainId: '#delegateDomainId'; delegateRcn: '#delegateRcn'"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement daWeb = new DelegationAgreement().with {
            it.delegateId = "delegateId"
            it
        }
        def token = "token"

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)

        User caller = new User().with {
            it.id = "callerId"
            it.domainId = callerDomainId
            it
        }
        1 * identityUserService.getEndUserById(caller.id) >> caller

        User delegate = new User().with {
            it.id = daWeb.delegateId
            it.domainId = delegateDomainId
            it
        }

        Domain callerDomain = new Domain().with {
            it.domainId = callerDomainId
            it.rackspaceCustomerNumber = callerRcn
            it
        }

        Domain delegateDomain = new Domain().with {
            it.domainId = delegateDomainId
            it.rackspaceCustomerNumber = delegateRcn
            it
        }
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement()

        when:
        def response = service.addAgreement(uriInfo, token, daWeb)

        then:
        response.status == SC_CREATED
        1 * delegationService.getDelegateByReference(_) >> {args ->
            DelegateReference delegateReference = args[0]
            assert delegateReference.id == delegate.id
            assert delegateReference.delegateType == DelegateType.USER
            delegate
        }
        domainService.getDomain(delegate.domainId) >> delegateDomain
        domainService.getDomain(caller.domainId) >> callerDomain
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(daWeb) >> daEntity
        1 * delegationService.addDelegationAgreement(daEntity)
        1 * delegationAgreementConverter.toDelegationAgreementWeb(daEntity) >> daWeb

        where:
        callerDomainId | callerRcn | delegateDomainId | delegateRcn
        "123"          | ""        | "123"            | ""
        "123"          | null      | "123"            | null
        "123"          | "abc"     | "123"            | "abc"
        "123"          | "abc"     | "456"            | "abc"
    }

    /**
     * Tests logic verifying the principal can delegate to the specified user. The delegate is generally required to be within
     * the same RCN as the user (principal and delegate domains have the same non-blank RCN value). However, when rcns
     * are globally enabled, we also allow the principal and delegate to belong to the same domain even if the RCN for
     * the domain is blank.
     *
     * Other tests verify when RCNs are NOT globally enabled, that the domains must contain RCNs. This test assumes that
     * the callers RCN is allowed to create DAs (because it's in the list, or globally enabled).
     *
     * This test focuses on the invalid cases
     *
     * @return
     */
    @Unroll
    def "createDelegationAgreement: Verify disallowed cases of delegate to principal - callerDomainId: '#callerDomainId'; callerRcn: '#callerRcn'; delegateDomainId: '#delegateDomainId'; delegateRcn: '#delegateRcn'"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement daWeb = new DelegationAgreement().with {
            it.delegateId = "delegateId"
            it
        }
        def token = "token"

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)

        User caller = new User().with {
            it.id = "callerId"
            it.domainId = callerDomainId
            it
        }

        User delegate = new User().with {
            it.id = "delegateId"
            it.domainId = delegateDomainId
            it
        }

        Domain callerDomain = new Domain().with {
            it.domainId = callerDomainId
            it.rackspaceCustomerNumber = callerRcn
            it
        }

        Domain delegateDomain = new Domain().with {
            it.domainId = delegateDomainId
            it.rackspaceCustomerNumber = delegateRcn
            it
        }
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(caller.id) >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true

        def capturedException
        exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_NOT_FOUND) }

        when:
        def response = service.addAgreement(uriInfo, token, daWeb)

        then:
        1 * delegationService.getDelegateByReference(_) >> {args ->
            DelegateReference delegateReference = args[0]
            assert delegateReference.id == delegate.id
            assert delegateReference.delegateType == DelegateType.USER
            delegate
        }
        domainService.getDomain(delegate.domainId) >> delegateDomain
        domainService.getDomain(caller.domainId) >> callerDomain
        response.status == SC_NOT_FOUND
        capturedException != null

        and: "Appropriate exception thrown"
        IdmExceptionAssert.assertException(capturedException, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, IdmExceptionAssert.PATTERN_ALL)

        where:
        callerDomainId | callerRcn | delegateDomainId | delegateRcn
        "123"          | ""        | "456"            | ""
        "123"          | "abc"     | "456"            | ""
        "123"          | "abc"     | "456"            | "def"
        "123"          | ""        | "456"            | "abc"
        "123"          | null      | "456"            | null
        "123"          | "abc"     | "456"            | null
        "123"          | null      | "456"            | "abc"
    }
}


