package com.rackspace.idm.api.filter

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.OpenTracingConfiguration
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.ForbiddenException
import com.sun.jersey.spi.container.ContainerRequest
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.HttpMethod

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
        request.getHeaderValue(GlobalConstants.X_AUTH_TOKEN) >> authTokenString

        httpServletRequest = Mock(HttpServletRequest)
        httpServletRequest.getRemoteAddr() >> "remoteIp"
        httpServletRequest.getLocalAddr() >> "hostIp"
        httpServletRequest.getHeader(GlobalConstants.X_FORWARDED_FOR) >> "forwardedIp"
        filter.req = httpServletRequest

        mockIdentityUserService(filter)
        mockScopeAccessService(filter)
        mockRequestContextHolder(filter)
        mockIdentityConfig(filter)


        reloadableConfig.getOpenTracingAuthFilterSpanEnabled() >> false
    }

    def "security context tokens not set on auth"() {
        given:
        request.getPath() >> AUTH_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        0 * scopeAccessService.getScopeAccessByAccessToken(_)
        request == returnedRequest
        0 * securityContext.setCallerTokens(_,_)
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
        1 * securityContext.setCallerTokens(scopeAccess, scopeAccess)
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
        1 * securityContext.setCallerTokens(scopeAccess, scopeAccess)
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
        1 * securityContext.setCallerTokens(scopeAccess, scopeAccess)
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
        1 * securityContext.setCallerTokens(scopeAccess, scopeAccess)
        0 * scopeAccessService.isSetupMfaScopedToken(scopeAccess)

        notThrown(ForbiddenException)
    }

    @Unroll
    def "startOpenTracingSpan - build span correctly : method = #method, path = #path, operation name = #operationName"() {
        given:
        // Setup Mocks
        def containerRequest = Mock(ContainerRequest)
        def openTracingConfiguration = Mock(OpenTracingConfiguration)
        def tracer = Mock(Tracer)
        def spanContext = Mock(SpanContext)
        def spanBuilder = Mock(Tracer.SpanBuilder)

        filter.openTracingConfiguration = openTracingConfiguration

        containerRequest.getMethod() >> method
        containerRequest.getPath() >> path

        when:
        filter.startOpenTracingSpan(containerRequest)

        then:
        2 * openTracingConfiguration.globalTracer >> tracer
        1 * tracer.extract(_, _) >> spanContext
        1 * tracer.buildSpan(operationName) >> spanBuilder
        1 * spanBuilder.asChildOf(spanContext) >> spanBuilder
        1 * spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT) >> spanBuilder
        1 * spanBuilder.startActive(true)

        where:
        method            | path                                  | operationName
        HttpMethod.POST   | "cloud/v2.0/tokens"                   | "POST cloud/v2.0/tokens" // authenticate
        HttpMethod.GET    | "cloud/v2.0/tokens/tokenId"           | "GET cloud/v2.0/tokens/****enId" // v2.0 validate token
        HttpMethod.DELETE | "cloud/v2.0/tokens/tokenId"           | "DELETE cloud/v2.0/tokens/****enId" // v2.0 revoke token
        HttpMethod.GET    | "cloud/v2.0/tokens/tokenId/endpoints" | "GET cloud/v2.0/tokens/****enId/endpoints" // v2.0 token endpoints
        HttpMethod.GET    | "cloud/v2.0/users/userId"             | "GET cloud/v2.0/users/userId" // v2.0 Get user by id
        HttpMethod.GET    | "cloud/v1.1/token/tokenId"            | "GET cloud/v1.1/token/****enId" // v1.1 validate token
        HttpMethod.DELETE | "cloud/v1.1/token/tokenId"            | "DELETE cloud/v1.1/token/****enId" // v1.1 revoke token
        HttpMethod.GET    | "cloud/v1.1/users/username"           | "GET cloud/v1.1/users/username" // v1.1 get user by name
    }

    @Unroll
    def "test feature flag 'feature.enable.open.tracing.auth.filter.span' - enabled = #enabled"() {
        given:
        mockIdentityConfig(filter)
        reloadableConfig.getOpenTracingAuthFilterSpanEnabled() >> enabled

        // Setup Mocks
        def containerRequest = Mock(ContainerRequest)
        def openTracingConfiguration = Mock(OpenTracingConfiguration)
        def tracer = Mock(Tracer)
        def spanContext = Mock(SpanContext)
        def spanBuilder = Mock(Tracer.SpanBuilder)

        filter.openTracingConfiguration = openTracingConfiguration

        containerRequest.getMethod() >> HttpMethod.POST
        containerRequest.getPath() >> "cloud/v2.0/tokens"

        when:
        filter.filter(containerRequest)

        then:
        if (enabled) {
            2 * openTracingConfiguration.globalTracer >> tracer
            1 * tracer.extract(_, _) >> spanContext
            1 * tracer.buildSpan("POST cloud/v2.0/tokens") >> spanBuilder
            1 * spanBuilder.asChildOf(spanContext) >> spanBuilder
            1 * spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT) >> spanBuilder
            1 * spanBuilder.startActive(true)
        } else {
            0 * openTracingConfiguration.globalTracer
            0 * tracer.extract(_, _)
            0 * tracer.buildSpan("POST cloud/v2.0/tokens")
        }

        where:
        enabled << [true, false]
    }
}
