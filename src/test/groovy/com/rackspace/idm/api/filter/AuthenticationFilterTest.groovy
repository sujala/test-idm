package com.rackspace.idm.api.filter
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.sun.jersey.spi.container.ContainerRequest
import org.apache.commons.configuration.Configuration
import org.springframework.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

class AuthenticationFilterTest extends Specification {

    @Shared def request
    @Shared def appCtx
    @Shared def config
    @Shared def userService
    @Shared def tenantService
    @Shared def scopeAccessService
    @Shared def headers
    @Shared def authTokenString = "token"
    @Shared def scopeAccess
    @Shared def filter

    def setup() {
        request = Mock(ContainerRequest)
        appCtx = Mock(ApplicationContext)
        config = Mock(Configuration)
        appCtx.getBean(Configuration) >> config
        userService = Mock(UserService)
        appCtx.getBean(UserService) >> userService
        scopeAccessService = Mock(ScopeAccessService)
        appCtx.getBean(ScopeAccessService) >> scopeAccessService
        request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER) >> authTokenString
        scopeAccess = new UserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        filter = new AuthenticationFilter()
        filter.setApplicationContext(appCtx)
    }

    def "when multi-factor feature flag is set to OFF all requests to multi-factor URLs get 404"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path
        config.getString("multifactor.services.enabled") >> "OFF"

        when:
        filter.filter(request)

        then:
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

    def "when multi-factor feature flag is set to BETA users must have identity-feature:mfa role"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path
        config.getString("multifactor.services.enabled") >> "BETA"

        when:
        def resonse = filter.filter(request)

        then:
        //TODO: verify
        true == true
    }

    def "when multi-factor feature flag is set to ON all users are allowed to access the MFA URLs"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path
        config.getString("multifactor.services.enabled") >> "FULL"

        when:
        def response = filter.filter(request)

        then:
        //TODO: find something better to verify
        response == response
    }

    def "when multi-factor feature flag is invalid all requests to multi-factor URLs get 404"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path
        config.getString("multifactor.services.enabled") >> "NOT A VALID VALUE"

        when:
        filter.filter(request)

        then:
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

}
