package com.rackspace.idm.api.resource.cloud.devops

import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.ScopeAccess
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import testHelpers.RootServiceTest

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

    def setupMocks() {
        mockAuthorizationService(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockCacheableKeyCzarCrypterLocator(service)
        mockRequestContextHolder(service)
        mockAeTokenService(service)
        mockAeTokenRevocationService(service)
    }
}
