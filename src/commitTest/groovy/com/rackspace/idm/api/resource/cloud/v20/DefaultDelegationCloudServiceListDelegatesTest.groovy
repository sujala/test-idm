package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateReferences
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

/**
 * Tests the list delegation agreements service. High level authorization tests for service are located in DefaultDelegationCloudServiceTest
 * to allow for tests common to multiple delegation services.
 */
class DefaultDelegationCloudServiceListDelegatesTest extends RootServiceTest {

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

    def "Throws NotFoundException when da does not exist"() {
        setup:
        def daId = "daId"
        DelegationAgreement da = Mock(DelegationAgreement)
        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domainId"
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        service.listDelegates(tokenStr, daId)

        then:
        1 * delegationService.getDelegationAgreementById(daId) >> null

        and:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception ex = args[0]
            IdmExceptionAssert.assertException(ex, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, IdmExceptionAssert.PATTERN_ALL)

            // Just return anything here. Not testing the response
            return Response.ok()
        }
    }

    def "Throws NotFoundException when caller is not an effective principal or delegate on da"() {
        setup:
        def daId = "daId"
        DelegationAgreement da = Mock(DelegationAgreement)
        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domainId"
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        service.listDelegates(tokenStr, daId)

        then:
        1 * delegationService.getDelegationAgreementById(daId) >> da
        1 * da.isEffectivePrincipal(caller) >> false
        1 * da.isEffectiveDelegate(caller) >> false

        and:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception ex = args[0]
            IdmExceptionAssert.assertException(ex, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, IdmExceptionAssert.PATTERN_ALL)

            // Just return anything here. Not testing the response
            return Response.ok()
        }
    }

    def "Lists delegates when caller is an effective principal on DA"() {
        setup:
        def daId = "daId"
        DelegationAgreement da = Mock(DelegationAgreement)
        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domainId"
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        def response = service.listDelegates(tokenStr, daId)

        then:
        1 * delegationService.getDelegationAgreementById(daId) >> da
        1 * da.isEffectivePrincipal(caller) >> true
        1 * delegationService.getDelegates(da) >> []
        1 * delegationAgreementConverter.toDelegatesWeb(_) >> new DelegateReferences()

        and:
        response.status == HttpStatus.SC_OK
    }

    def "Lists delegates when caller is an effective delegate on DA"() {
        setup:
        def daId = "daId"
        DelegationAgreement da = Mock(DelegationAgreement)
        // Create the entities used for the test
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domainId"
            it
        }
        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        def response = service.listDelegates(tokenStr, daId)

        then:
        1 * delegationService.getDelegationAgreementById(daId) >> da
        1 * da.isEffectivePrincipal(caller) >> false
        1 * da.isEffectiveDelegate(caller) >> true
        1 * delegationService.getDelegates(da) >> []
        1 * delegationAgreementConverter.toDelegatesWeb(_) >> new DelegateReferences()

        and:
        response.status == HttpStatus.SC_OK
    }
}


