package com.rackspace.idm.api.filter

import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
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
        tenantService = Mock(TenantService)
        appCtx.getBean(TenantService) >> tenantService
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

        when:
        filter.filter(request)

        then:
        1 * config.getString("multifactor.services.enabled") >> "OFF"
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

    def "when multi-factor feature flag is set to BETA users without identity-feature:mfa role get 404"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        filter.filter(request)

        then:
        1 * config.getString("multifactor.services.enabled") >> "BETA"
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

    def "when multi-factor feature flag is set to BETA users with identity-feature:mfa role are allowed to access API"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path
        config.getString("multifactor.services.enabled") >> "BETA"
        def mfaBetaRoleRsId = 123
        config.getString("cloudAuth.multiFactorBetaRoleRsId") >> mfaBetaRoleRsId
        def userGlobalRoles = []
        userGlobalRoles << new TenantRole().with {
            it.roleRsId = mfaBetaRoleRsId
            it
        }
        def user = new BaseUser()

        when:
        def response = filter.filter(request)

        then:
        1 * getUserService().getUserByScopeAccess(scopeAccess) >> user
        1 * getTenantService().getGlobalRolesForUser(user) >> userGlobalRoles
        response == request
    }

    def "when multi-factor feature flag is set to FULL all users are allowed to access the MFA URLs"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        def response = filter.filter(request)

        then:
        1 * config.getString("multifactor.services.enabled") >> "FULL"
        response == request
    }

    def "when multi-factor feature flag is invalid all requests to multi-factor URLs get 404"() {
        given:
        def path = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"
        request.getPath() >> path

        when:
        filter.filter(request)

        then:
        1 * config.getString("multifactor.services.enabled") >> "NOT A VALID VALUE"
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

}
