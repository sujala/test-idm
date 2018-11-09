package com.rackspace.idm.api.filter

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.service.RackerAuthenticationService
import com.sun.jersey.spi.container.ContainerRequest
import org.slf4j.MDC
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest

class IdentityRequestFilterTest extends RootServiceTest {
    static final String AUTH_URL = "cloud/v2.0/tokens"
    static final String TOKEN_VALIDATE_URL_UUID = "cloud/v2.0/tokens/12235"
    static final String TOKEN_VALIDATE_URL_AE = "cloud/v2.0/tokens/asdflwqenoiu-wkjnrqwer_nk32jwe"
    static final String TOKEN_ENDPOINT_URL = "cloud/v2.0/tokens/12235/endpoints"

    static final String DOMAIN_MFA_URL = "cloud/v2.0/RAX-AUTH/domains/{domainId}/multi-factor"
    static final String USERS_MFA_URL = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"

    @Shared ContainerRequest request
    @Shared HttpServletRequest httpServletRequest

    @Shared def authTokenString = "token"

    @Shared IdentityRequestFilter filter

    def setup() {
        filter = new IdentityRequestFilter()

        request = Mock(ContainerRequest)
        request.getHeaderValue(GlobalConstants.X_AUTH_TOKEN) >> authTokenString

        httpServletRequest = Mock(HttpServletRequest)
        httpServletRequest.getRemoteAddr() >> "remoteIp"
        httpServletRequest.getLocalAddr() >> "hostIp"
        httpServletRequest.getHeader(GlobalConstants.X_FORWARDED_FOR) >> "forwardedIp"
        filter.req = httpServletRequest

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

    def "security context is reset"() {
        given:
        request.getPath() >> AUTH_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        request == returnedRequest
        requestContext.setSecurityContext(_) >> {args ->
            SecurityContext capturedSecurityContext = args[0]
            capturedSecurityContext.callerToken == null
        }
        noExceptionThrown()
    }
 }
