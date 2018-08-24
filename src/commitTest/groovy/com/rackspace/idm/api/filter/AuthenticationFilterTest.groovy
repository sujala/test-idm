package com.rackspace.idm.api.filter

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.sun.jersey.spi.container.ContainerRequest
import org.apache.commons.lang.StringUtils
import org.slf4j.MDC
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

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
        request == returnedRequest
        noExceptionThrown()
    }
}
