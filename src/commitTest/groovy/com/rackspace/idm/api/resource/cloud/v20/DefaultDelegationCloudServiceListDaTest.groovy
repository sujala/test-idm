package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.BaseUserToken
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
/**
 * Tests the list delegation agreements service. High level authorization tests for service are located in DefaultDelegationCloudServiceTest
 * to allow for tests common to multiple delegation services.
 */
class DefaultDelegationCloudServiceListDaTest extends RootServiceTest {

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
     * Verifies the relationship is case insensitive for delegate and that the proper search params are provided to
     * back end service.
     */
    @Unroll
    def "Searches only for delegates when relationship=#relationship"() {
        setup:
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
        service.listAgreements(tokenStr, relationship)

        then:
        1 * delegationService.findDelegationAgreements(_) >> {args ->
            FindDelegationAgreementParams params = args[0]
            assert params != null
            assert params.delegate == caller
            assert params.principal == null

            Collections.emptyList()
        }

        where:
        relationship << ["delegate", "DELEGATE", "dElEgAte"]
    }

    /**
     * Verifies the relationship is case insensitive for principal and that the proper search params are provided to
     * back end service.
     */
    @Unroll
    def "Searches only for principals when relationship=#relationship"() {
        setup:
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
        service.listAgreements(tokenStr, relationship)

        then:
        1 * delegationService.findDelegationAgreements(_) >> {args ->
            FindDelegationAgreementParams params = args[0]
            assert params != null
            assert params.delegate == null
            assert params.principal == caller

            Collections.emptyList()
        }

        where:
        relationship << ["principal", "PRINCIPAL", "PrinCipaL"]
    }

    @Unroll
    def "Searches for principals and delegates when relationship='#relationship'"() {
        setup:
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
        service.listAgreements(tokenStr, relationship)

        then:
        1 * delegationService.findDelegationAgreements(_) >> {args ->
            FindDelegationAgreementParams params = args[0]
            assert params != null
            assert params.delegate == caller
            assert params.principal == caller

            Collections.emptyList()
        }

        where:
        relationship << [null, "", "  "]
    }

    @Unroll
    def "Throws BadRequestException when relationship not recognized: relationship='#relationship'"() {
        setup:
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
        service.listAgreements(tokenStr, relationship)

        then:
        0 * delegationService.findDelegationAgreements(_)
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception ex = args[0]
            IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, "The specified relationship is invalid")

            // Just return anything here. Not testing the response
            return Response.ok()
        }

        where:
        relationship << ["a", "invalid", "both"]
    }

    def "Authorized users search DAs by principalDomains"() {
        given:
        def domainId = "domainId"
        // Create the entities used for the test
        Domain domain = entityFactory.createDomain(domainId).with {
            it.rackspaceCustomerNumber = "RCN-123-123-123"
            it
        }
        def otherDomainId = "domainId2"
        Domain otherDomain = entityFactory.createDomain(otherDomainId).with {
            it.rackspaceCustomerNumber = "RCN-123-123-123"
            it
        }
        def da = entityFactory.createDelegationAgreement(otherDomainId)
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = domain.domainId
            it
        }

        def tokenStr = "callerTokenStr"
        def token = Mock(BaseUserToken)

        // Authorization mocks
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(tokenStr) >> token
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller


        when: "caller is an rcn-admin"
        service.listAgreements(tokenStr, null)

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                Collections.singletonList(IdentityRole.RCN_ADMIN.getRoleName())) >> true
        1 * requestContext.getEffectiveCallerDomain() >> domain
        1 * domainService.getDomainsByRCN(domain.getRackspaceCustomerNumber()) >> [domain, otherDomain]
        1 * delegationService.findDelegationAgreements(_) >> { args ->
            FindDelegationAgreementParams params = args[0]
            assert params.principalDomains.size() == 2
            assert params.principalDomains.contains(domain)
            assert params.principalDomains.contains(otherDomain)
            []
        }

        when: "caller is an user-admin"
        service.listAgreements(tokenStr, null)

        then:
        3 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(_) >>> [false, true, false]
        1 * requestContext.getEffectiveCallerDomain() >> domain
        0 * domainService.getDomainsByRCN(_)
        1 * delegationService.findDelegationAgreements(_) >> { args ->
            FindDelegationAgreementParams params = args[0]
            assert params.principalDomains.contains(domain)
            []
        }

        when: "caller is an user-manage"
        service.listAgreements(tokenStr, null)

        then:
        3 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(_) >>> [false, true, true]
        1 * requestContext.getEffectiveCallerDomain() >> domain
        0 * domainService.getDomainsByRCN(_)
        1 * delegationService.findDelegationAgreements(_) >> { args ->
            FindDelegationAgreementParams params = args[0]
            assert params.principalDomains.contains(domain)
            []
        }
    }
}


