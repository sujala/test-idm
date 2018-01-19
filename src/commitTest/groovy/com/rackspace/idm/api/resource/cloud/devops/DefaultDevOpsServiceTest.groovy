package com.rackspace.idm.api.resource.cloud.devops

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TokenAnalysis
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.NotFoundException
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/26/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultDevOpsServiceTest extends RootServiceTest{
    @Shared DevOpsService service

    def setupSpec(){
        service = new DefaultDevOpsService()
    }

    def "Verify that encrypt users is only allowed for a service admin" () {
        given:
        setupMocks()
        allowUserAccess()

        when:
        service.encryptUsers("token")

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_)
        1 * userService.reEncryptUsers()
    }

    def "Verify that reset key metadata is invoked"() {
        given:
        setupMocks()
        allowUserAccess()

        when:
        service.resetKeyMetadata("token")

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_)
        1 * cacheableKeyCzarCrypterLocator.resetCache()
    }

    def "verify analyze token"() {
        given:
        setupMocks()
        allowUserAccess()

        when:

        def scopeAccess = createScopeAccess().with {
            it.clientId = "clientId"
            return it
        }

        aeTokenService.unmarshallToken(_) >> scopeAccess
        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(_)
        1 * securityContext.getAndVerifyEffectiveCallerToken(_)
        1 * aeTokenService.unmarshallToken(_)
    }

    def "verify analyze token with deleted user does not get domain"() {
        given:
        def user = new User().with {
            it.id = 'id'
            it
        }

        setupMocks()
        allowUserAccess()

        when:
        def scopeAccess = createUserScopeAccess().with {
            it.clientId = "clientId"
            return it
        }


        aeTokenService.unmarshallToken(_) >> scopeAccess
        userService.getUserByScopeAccess(scopeAccess, false) >> { throw new NotFoundException()}
        aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(_) >> [].asList()

        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        0 * domainService.getDomain(_) >> user
    }

    def "verify analyze impersonated token with deleted user does not get domain"() {
        given:
        setupMocks()
        allowUserAccess()

        when:
        def scopeAccess = createImpersonatedScopeAccess().with {
            it.clientId = "clientId"
            it.rsImpersonatingRsId = "id"
            return it
        }

        def user = new User().with {
            it.id = 'id'
            it
        }

        aeTokenService.unmarshallToken(_) >> scopeAccess
        userService.getUserByScopeAccess(scopeAccess, false) >> user
        identityUserService.getEndUserById("id") >> null
        aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(_) >> [].asList()

        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        0 * domainService.getDomain(_)
    }

    def "getDeletedUserByScopeAccess with impersonated scopeAccess and racker id returns racker"() {
        given:
        def rackerId = "id"
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess().with {
            it.rackerId = rackerId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof Racker
        user.id == rackerId
    }

    def "getDeletedUserByScopeAccess with impersonated scopeAccess and user id returns user"() {
        given:
        def userId = "id"
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess().with {
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof User
        user.id == userId
    }

    def "getDeletedUserByScopeAccess with user scopeAccess and federated auth by returns federated user"() {
        given:
        def userId = "id"
        UserScopeAccess scopeAccess = new UserScopeAccess().with {
            it.authenticatedBy =[GlobalConstants.AUTHENTICATED_BY_FEDERATION].asList()
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof FederatedUser
        user.id == userId
    }

    def "getDeletedUserByScopeAccess with user scopeAccess and not federated auth by returns user"() {
        given:
        def userId = "id"
        UserScopeAccess scopeAccess = new UserScopeAccess().with {
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof User
        user.id == userId
    }


    def setupMocks() {
        mockAuthorizationService(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockCacheableKeyCzarCrypterLocator(service)
        mockRequestContextHolder(service)
        mockAeTokenService(service)
        mockAeTokenRevocationService(service)
        mockIdentityUserService(service)
    }
}
