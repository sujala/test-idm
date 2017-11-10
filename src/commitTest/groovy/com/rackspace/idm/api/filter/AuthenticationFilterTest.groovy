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
    static final String TOKEN_VALIDATE_URL_UUID = "cloud/v2.0/tokens/12235"
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
        mockMultiFactorCloud20Service(filter)
        mockAuthorizationService(filter)
        mockRequestContextHolder(filter)
        mockIdentityConfig(filter)
    }

    def "filter: MDC audit value set to guid when property 'feature.enable.use.repose.request.id' set to false"() {
        given:
        request.getPath() >> AUTH_URL
        reloadableConfig.isFeatureUseReposeRequestIdEnabled() >> false

        when:
        filter.filter(request)

        then:
        0 * request.getHeaderValue(GlobalConstants.X_REQUEST_ID)
        MDC.get(Audit.GUUID) != null
        UUID.fromString(MDC.get(Audit.GUUID)) //parsable as GUID
    }

    def "filter: MDC audit value set to guid when property 'feature.enable.use.repose.request.id' set to true, but no header value"() {
        given:
        request.getPath() >> AUTH_URL
        reloadableConfig.isFeatureUseReposeRequestIdEnabled() >> true

        when:
        filter.filter(request)

        then:
        1 * request.getHeaderValue(GlobalConstants.X_REQUEST_ID) >> null
        MDC.get(Audit.GUUID) != null
        UUID.fromString(MDC.get(Audit.GUUID)) //parsable as GUID
    }

    def "filter: MDC audit value uses provided request id header when property 'feature.enable.use.repose.request.id' set to true"() {
        given:
        def requestId = "aRequestId"
        request.getPath() >> AUTH_URL
        reloadableConfig.isFeatureUseReposeRequestIdEnabled() >> true

        when:
        filter.filter(request)

        then:
        1 * request.getHeaderValue(GlobalConstants.X_REQUEST_ID) >> requestId
        MDC.get(Audit.GUUID) != null
        MDC.get(Audit.GUUID) == requestId
    }

    def "filter: MDC audit value truncates x-request-id header as necessary when property 'feature.enable.use.repose.request.id' set to true"() {
        given:
        def requestId = "aRequestId"
        request.getPath() >> AUTH_URL
        reloadableConfig.isFeatureUseReposeRequestIdEnabled() >> true

        when:
        filter.filter(request)

        then:
        1 * request.getHeaderValue(GlobalConstants.X_REQUEST_ID) >> x_request_id
        MDC.get(Audit.GUUID) != null
        MDC.get(Audit.GUUID) == expected_audit

        where:
        x_request_id | expected_audit
        "a" * 40 | "a" * 40
        "a" * 65 | "a" * 64
        "a" * 1000 | "a" * 64
    }

    def "security context tokens not set on auth"() {
        given:
        def scopeAccess = new UserScopeAccess()
        SecurityContext capturedSecurityContext
        securityContext.getEffectiveCallerToken() >> scopeAccess
        requestContext.setSecurityContext(_) >> {args -> capturedSecurityContext = args[0]}
        request.getPath() >> AUTH_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        0 * scopeAccessService.getScopeAccessByAccessToken(_)
        request == returnedRequest
        capturedSecurityContext.callerToken == null
        noExceptionThrown()
    }

    def "security context tokens set on endpoints"() {
        given:
        def scopeAccess = new UserScopeAccess()
        SecurityContext capturedSecurityContext
        securityContext.getEffectiveCallerToken() >> scopeAccess
        requestContext.setSecurityContext(_) >> {args -> capturedSecurityContext = args[0]}
        request.getPath() >> TOKEN_ENDPOINT_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        request == returnedRequest
        capturedSecurityContext.callerToken == scopeAccess
        capturedSecurityContext.effectiveCallerToken == scopeAccess

        noExceptionThrown()
    }

    def "security context tokens set on validate"() {
        def scopeAccess = new UserScopeAccess()
        SecurityContext capturedSecurityContext
        securityContext.getEffectiveCallerToken() >> scopeAccess
        requestContext.setSecurityContext(_) >> {args -> capturedSecurityContext = args[0]}

        when:
        request.getPath() >> TOKEN_VALIDATE_URL_UUID
        def returnedRequest = filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        request == returnedRequest
        capturedSecurityContext.callerToken == scopeAccess
        capturedSecurityContext.effectiveCallerToken == scopeAccess
        noExceptionThrown()

        when:
        request.getPath() >> TOKEN_VALIDATE_URL_AE
        returnedRequest = filter.filter(request)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        request == returnedRequest
        capturedSecurityContext.callerToken == scopeAccess
        capturedSecurityContext.effectiveCallerToken == scopeAccess
        noExceptionThrown()
    }



}
