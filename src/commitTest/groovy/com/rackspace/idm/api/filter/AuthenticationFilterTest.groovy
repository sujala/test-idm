package com.rackspace.idm.api.filter


import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.exception.ForbiddenException
import com.sun.jersey.spi.container.ContainerRequest
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest

class AuthenticationFilterTest extends RootServiceTest {
    static final String AUTH_URL = "cloud/v2.0/tokens"
    static final String TOKEN_VALIDATE_URL_AE = "cloud/v2.0/tokens/asdflwqenoiu-wkjnrqwer_nk32jwe"
    static final String TOKEN_ENDPOINT_URL = "cloud/v2.0/tokens/12235/endpoints"

    static final String DOMAIN_MFA_URL = "cloud/v2.0/RAX-AUTH/domains/{domainId}/multi-factor"
    static final String USERS_MFA_URL = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"

    @Shared ContainerRequest request
    @Shared HttpServletRequest httpServletRequest

    @Shared def authTokenString = "token"

    @Shared AuthenticationFilter filter

    def setup() {
        filter = new AuthenticationFilter()

        request = Mock(ContainerRequest)
        request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER) >> authTokenString

        httpServletRequest = Mock(HttpServletRequest)
        httpServletRequest.getRemoteAddr() >> "remoteIp"
        httpServletRequest.getLocalAddr() >> "hostIp"
        httpServletRequest.getHeader(GlobalConstants.X_FORWARDED_FOR) >> "forwardedIp"
        filter.req = httpServletRequest

        mockIdentityUserService(filter)
        mockScopeAccessService(filter)
        mockRequestContextHolder(filter)
        mockIdentityConfig(filter)
    }

    def "security context tokens not set on auth"() {
        given:
        request.getPath() >> AUTH_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        0 * scopeAccessService.getScopeAccessByAccessToken(_)
        request == returnedRequest
        0 * securityContext.setCallerToken(_)
        noExceptionThrown()
    }

    def "security context tokens set on endpoints"() {
        given:
        def scopeAccess = new UserScopeAccess()
        request.getPath() >> TOKEN_ENDPOINT_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        1 * securityContext.setCallerToken(scopeAccess)
        request == returnedRequest

        noExceptionThrown()
    }

    def "security context tokens set on validate"() {
        def scopeAccess = new UserScopeAccess()

        when:
        request.getPath() >> TOKEN_VALIDATE_URL_AE
        def returnedRequest = filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        1 * securityContext.setCallerToken(scopeAccess)
        1 * securityContext.setEffectiveCallerToken(scopeAccess)
        0 * scopeAccessService.isSetupMfaScopedToken(scopeAccess)
        request == returnedRequest
        noExceptionThrown()
    }

    /**
     * This is really just a test that the feature flag encapsulates the existing code. It is not meant to exhaustively
     * test the MFA code within the AuthenticationFilter.
     * @return
     */
    def "MFA validation occurs when flag disabled"() {
        reloadableConfig.useAspectForMfaAuthorization() >> false
        def scopeAccess = new UserScopeAccess().with {
            it.scope = TokenScopeEnum.SETUP_MFA
            it
        }

        when: "A non MFA request is sent using a setup MFA token"
        request.getPath() >> TOKEN_ENDPOINT_URL
        filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        1 * securityContext.setCallerToken(scopeAccess)
        1 * securityContext.setEffectiveCallerToken(scopeAccess)
        1 * scopeAccessService.isSetupMfaScopedToken(scopeAccess) >> true

        thrown(ForbiddenException)
    }

    /**
     * This is really just a test that the feature flag encapsulates the existing code. It is not meant to exhaustively
     * test the MFA code within the AuthenticationFilter.
     * @return
     */
    def "MFA validation does not occur when flag enabled"() {
        reloadableConfig.useAspectForMfaAuthorization() >> true

        def scopeAccess = new UserScopeAccess().with {
            it.scope = TokenScopeEnum.SETUP_MFA
            it
        }

        when: "A non MFA request is sent using a setup MFA token"
        request.getPath() >> TOKEN_ENDPOINT_URL
        filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        1 * securityContext.setCallerToken(scopeAccess)
        1 * securityContext.setEffectiveCallerToken(scopeAccess)
        0 * scopeAccessService.isSetupMfaScopedToken(scopeAccess)

        notThrown(ForbiddenException)
    }
}
