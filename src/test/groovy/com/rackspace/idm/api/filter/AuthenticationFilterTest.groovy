package com.rackspace.idm.api.filter
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.sun.jersey.spi.container.ContainerRequest
import org.springframework.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

class AuthenticationFilterTest extends Specification {

    @Shared def request
    @Shared def appCtx
    @Shared def userService
    @Shared def scopeAccessService
    @Shared def headers
    @Shared def authTokenString = "token"
    @Shared def scopeAccess
    @Shared def filter
    @Shared def mfaService

    def setup() {
        request = Mock(ContainerRequest)
        appCtx = Mock(ApplicationContext)
        userService = Mock(UserService)
        appCtx.getBean(UserService) >> userService
        scopeAccessService = Mock(ScopeAccessService)
        appCtx.getBean(ScopeAccessService) >> scopeAccessService
        request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER) >> authTokenString
        scopeAccess = new UserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        mfaService = Mock(MultiFactorCloud20Service)
        appCtx.getBean(MultiFactorCloud20Service) >> mfaService
        filter = new AuthenticationFilter()
        filter.setApplicationContext(appCtx)
    }

    def "when multi-factor feature flag is false all requests to multi-factor are unmodified"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        def returnedRequest = filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> false
        request == returnedRequest
        noExceptionThrown()
    }

    def "when multi-factor feature flag is true and user does not have MFA access throws 401"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> true
        1 * mfaService.isMultiFactorEnabledForUser(_) >> false
        WebApplicationException exception = thrown()
        exception.response.status == 401
    }

    def "when multi-factor feature flag is true and user does have MFA request is unmodified"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        def response = filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> true
        1 * mfaService.isMultiFactorEnabledForUser(_) >> true
        response == request
    }

}
