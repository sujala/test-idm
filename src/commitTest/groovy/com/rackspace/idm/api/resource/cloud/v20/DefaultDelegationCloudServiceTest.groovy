package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import static org.apache.http.HttpStatus.SC_CREATED
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
    }

    /**
     * Verifies the request is authorized appropriately. Fails the check to verify domain to test for
     * standard exception handling.
     *
     * @return
     */
    def "addAgreement: Verifies token and caller with standard exception handling"() {
        DelegationAgreement daWeb = new DelegationAgreement()
        User caller = new User()
        Domain callerDomain = new Domain().with {
            it.rackspaceCustomerNumber = "myRcn"
            it
        }
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"

        def capturedException
        exceptionHandler.exceptionResponse(_ as ForbiddenException) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        when:
        service.addAgreement(uriInfo, token, daWeb)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> tokenScopeAccess
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        1 * reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> false

        and: "Appropriate exception thrown"
        IdmExceptionAssert.assertException(capturedException, ForbiddenException, ErrorCodes.ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN, IdmExceptionAssert.PATTERN_ALL)
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

        securityContext.getAndVerifyEffectiveCallerToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)

        User caller = new User().with {
            it.id = "callerId"
            it.domainId = callerDomainId
            it
        }

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
        requestContext.getEffectiveCaller() >> caller
        identityUserService.getProvisionedUserById(delegate.id)
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true
        def capturedException
        exceptionHandler.exceptionResponse(_ as ForbiddenException) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement()

        when:
        def response = service.addAgreement(uriInfo, token, daWeb)

        then:
        response.status == SC_CREATED
        1 * identityUserService.getProvisionedUserById(delegate.id) >> delegate
        domainService.getDomain(delegate.domainId) >> delegateDomain
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

        securityContext.getAndVerifyEffectiveCallerToken(token) >> tokenScopeAccess
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
        requestContext.getEffectiveCaller() >> caller
        identityUserService.getProvisionedUserById(delegate.id)
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true

        def capturedException
        exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_NOT_FOUND) }

        when:
        def response = service.addAgreement(uriInfo, token, daWeb)

        then:
        1 * identityUserService.getProvisionedUserById(delegate.id) >> delegate
        domainService.getDomain(delegate.domainId) >> delegateDomain
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


