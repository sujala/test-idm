package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

import static org.apache.http.HttpStatus.SC_CONFLICT
import static org.apache.http.HttpStatus.SC_NO_CONTENT

class DefaultDelegationCloudServiceAddDelegateTest extends RootServiceTest {

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

    @Unroll
    def "Throws 409 when delegate already exists on DA"() {
        setup:
        DelegateReference delegateReference = delegate.getDelegateReference()

        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId =
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement mockDa = Mock(com.rackspace.idm.domain.entity.DelegationAgreement)
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(_) >> true
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        domainService.doDomainsShareRcn(_, _) >> true
        delegationService.getDelegateByReference(_) >> delegate

        // Service specific business logic
        delegationService.getDelegationAgreementById(_) >> mockDa
        mockDa.isEffectivePrincipal(caller) >> true

        when: "Delegate already a delegate"
        def response = service.addDelegate(tokenStr, "daId", delegateReference)

        then:
        response.status == SC_CONFLICT
        1 * mockDa.isExplicitDelegate(delegate) >> true
        0 * delegationService.addDelegate(_, _)
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            def exception = args[0]
            IdmExceptionAssert.assertException(exception, DuplicateException, ErrorCodes.ERROR_CODE_DELEGATE_ALREADY_EXISTS, IdmExceptionAssert.PATTERN_ALL)
            Response.status(SC_CONFLICT)
        }

        where:
        delegate << [
                new User().with {
                    it.domainId = "123"
                    it.id = "abc"
                    it
                }, new UserGroup().with {
                    it.domainId = "123"
                    it.id = "abc"
                    it
                }]
    }

    @Unroll
    def "Adds delegate when not already delegate on DA"() {
        setup:
        DelegateReference delegateReference = delegate.getDelegateReference()

        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId =
                    it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement mockDa = Mock(com.rackspace.idm.domain.entity.DelegationAgreement)
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(_) >> true
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        domainService.doDomainsShareRcn(_, _) >> true
        delegationService.getDelegateByReference(_) >> delegate

        // Service specific business logic
        delegationService.getDelegationAgreementById(_) >> mockDa
        mockDa.isEffectivePrincipal(caller) >> true

        when: "Delegate not a delegate "
        def response = service.addDelegate(tokenStr, "daId", delegateReference)

        then:
        1 * mockDa.isExplicitDelegate(delegate) >> false
        1 * delegationService.addDelegate(mockDa, delegate)
        response.status == SC_NO_CONTENT

        where:
        delegate << [
                new User().with {
                    it.domainId = "123"
                    it.id = "abc"
                    it
                }, new UserGroup().with {
                    it.domainId = "123"
                    it.id = "abc"
                    it
        }]
    }
}


