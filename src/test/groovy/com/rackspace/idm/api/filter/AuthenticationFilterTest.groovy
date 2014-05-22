package com.rackspace.idm.api.filter
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.sun.jersey.spi.container.ContainerRequest
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

class AuthenticationFilterTest extends Specification {

    @Shared def request
    @Shared def userService
    @Shared def scopeAccessService
    @Shared def headers
    @Shared def authTokenString = "token"
    @Shared def scopeAccess
    @Shared def filter
    @Shared def mfaService

    def setup() {
        filter = new AuthenticationFilter()
        request = Mock(ContainerRequest)
        userService = Mock(UserService)
        filter.userService = userService
        scopeAccessService = Mock(ScopeAccessService)
        filter.scopeAccessService = scopeAccessService
        request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER) >> authTokenString
        scopeAccess = new UserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        mfaService = Mock(MultiFactorCloud20Service)
        filter.multiFactorCloud20Service = mfaService
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

    def "when multi-factor feature flag is true and user does not have MFA access throws 404"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> true
        1 * mfaService.isMultiFactorEnabledForUser(_) >> false
        WebApplicationException exception = thrown()
        exception.response.status == 404
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
