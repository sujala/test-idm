package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.*
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class AuthWithDelegationCredentialsTest extends RootServiceTest {

    @Shared AuthWithDelegationCredentials service

    def setup() {
        service = new AuthWithDelegationCredentials()
        mockDelegationService(service)
        mockIdentityConfig(service)
        mockScopeAccessService(service)
        mockIdentityUserService(service)
        mockRequestContextHolder(service)
        mockCreateSubUserService(service)
        mockValidator20(service)
        mockDomainService(service)
    }

    @Unroll
    def "authenticate validates token is not blank; throws validator exception"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest(val, "id")
        def exceptionMsg = "exception"

        when:
        service.authenticate(authRequest)

        then:
        1 * validator20.validateStringNotBlank("token", val) >> {throw new BadRequestException(exceptionMsg)}

        // thrown when token is null
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, null, exceptionMsg)

        where:
        val << [null, "", "  "]
    }

    @Unroll
    def "authenticate validates delegation agreement is not blank; throws validator exception"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", val)
        def exceptionMsg = "exception"

        when:
        service.authenticate(authRequest)

        then:
        1 * validator20.validateStringNotBlank(_, _)
        1 * validator20.validateStringNotBlank("delegationAgreementId", val) >> {throw new BadRequestException(exceptionMsg)}

        // thrown when token is null
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, null, exceptionMsg)

        where:
        val << [null, "", "  "]
    }

    @Unroll
    def "authenticate throws NotAuthenticated when token not found or expired"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        validator20.validateStringNotBlank(_ as String, _ as String)

        when:
        service.authenticate(authRequest)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_) >> val

        // thrown when token is null
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotAuthenticatedException, null, AuthWithDelegationCredentials.ERROR_MSG_INVALID_TOKEN)

        where:
        val << [null
                , new UserScopeAccess().with {it.accessTokenExp = new DateTime().minusDays(1).toDate(); it}
        ]
    }

    @Unroll
    def "authenticate throws Forbidden when Racker, impersonated, User with scope, or delegation token"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        validator20.validateStringNotBlank(_ as String, _ as String)

        when:
        service.authenticate(authRequest)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_) >> val

        // thrown when token is null
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, null, IdmExceptionAssert.PATTERN_ALL)

        where:
        val << [entityFactory.createRackerScopeAccess().with {
                    it.accessTokenExp = new DateTime().plusDays(1).toDate()
                    it}
                , new ImpersonatedScopeAccess().with {
                    it.accessTokenExp = new DateTime().plusDays(1).toDate()
                    it.accessTokenString = "token"
                    it}
                , entityFactory.createUserToken().with {
                    it.scope = "someScope"
                    it}
                , entityFactory.createUserToken().with {
                    it.delegationAgreementId = "agreement"
                    it}
        ]
    }

    def "authenticate throws NotAuthenticated when token associated with non-existent user"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        UserScopeAccess token = entityFactory.createUserToken()
        scopeAccessService.getScopeAccessByAccessToken(_) >> token

        when:
        service.authenticate(authRequest)

        then:
        1 * identityUserService.getEndUserById(token.userRsId) >> null

        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotAuthenticatedException, null, IdmExceptionAssert.PATTERN_ALL)
    }

    def "authenticate throws NotFoundException when delegation agreement not found"() {
        given:
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        UserScopeAccess token = entityFactory.createUserToken()
        scopeAccessService.getScopeAccessByAccessToken(_) >> token
        identityUserService.getEndUserById(token.userRsId) >> entityFactory.createUser()

        when:
        service.authenticate(authRequest)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotFoundException, null, AuthWithDelegationCredentials.ERROR_MSG_MISSING_AGREEMENT)
    }

    def "authenticate throws NotFoundException when caller not a delegate on agreement"() {
        given:
        def agreement = Mock(DelegationAgreement)
        def caller = entityFactory.createUser()
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        UserScopeAccess token = entityFactory.createUserToken()
        scopeAccessService.getScopeAccessByAccessToken(_) >> token
        identityUserService.getEndUserById(token.userRsId) >> caller
        delegationService.getDelegationAgreementById("id") >> agreement

        when:
        service.authenticate(authRequest)

        then:
        1 * agreement.isEffectiveDelegate(caller) >> false
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotFoundException, null, AuthWithDelegationCredentials.ERROR_MSG_MISSING_AGREEMENT)
    }

    @Unroll
    def "authenticate throws Forbidden when delegation domain is missing or disabled"() {
        given:
        def agreement = Mock(DelegationAgreement)
        def caller = entityFactory.createUser()
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        UserScopeAccess token = entityFactory.createUserToken()
        scopeAccessService.getScopeAccessByAccessToken(_) >> token
        identityUserService.getEndUserById(token.userRsId) >> caller
        delegationService.getDelegationAgreementById("id") >> agreement

        when:
        service.authenticate(authRequest)

        then:
        1 * agreement.isEffectiveDelegate(caller) >> true
        1 * agreement.getDomainId() >> "1"
        1 * domainService.getDomain("1") >> domain
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, null, AuthWithDelegationCredentials.ERROR_MSG_DISABLED_OR_MISSING_DOMAIN)

        where:
        domain << [
                null,
                new Domain().with {
                    it.domainId = "1"
                    it.enabled = false
                    it
                }
        ]
    }

    @Unroll
    def "authenticate throws Forbidden when getting defaults throws an error"() {
        given:
        def agreement = Mock(DelegationAgreement)
        def caller = entityFactory.createUser()
        def authRequest = v2Factory.createTokenDelegationAuthenticationRequest("id", "id")
        UserScopeAccess token = entityFactory.createUserToken()
        scopeAccessService.getScopeAccessByAccessToken(_) >> token
        identityUserService.getEndUserById(token.userRsId) >> caller
        delegationService.getDelegationAgreementById("id") >> agreement
        agreement.isEffectiveDelegate(caller) >> true
        agreement.getDomainId() >> "1"
        domainService.getDomain(_) >> new Domain().with {
            it.domainId = "1"
            it.enabled = true
            it
        }

        when:
        service.authenticate(authRequest)

        then:
        1 * createSubUserService.calculateDomainSubUserDefaults(agreement.getDomainId()) >> {throw new DomainDefaultException("msg", "code")}

        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, "code", AuthWithDelegationCredentials.ERROR_MSG_DISABLED_OR_MISSING_DOMAIN)
    }
}