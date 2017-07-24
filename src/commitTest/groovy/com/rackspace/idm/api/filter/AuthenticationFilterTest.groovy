package com.rackspace.idm.api.filter
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.sun.jersey.spi.container.ContainerRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class AuthenticationFilterTest extends Specification {
    static final String AUTH_URL = "cloud/v2.0/tokens"
    static final String TOKEN_VALIDATE_URL_UUID = "cloud/v2.0/tokens/12235"
    static final String TOKEN_VALIDATE_URL_AE = "cloud/v2.0/tokens/asdflwqenoiu-wkjnrqwer_nk32jwe"
    static final String TOKEN_ENDPOINT_URL = "cloud/v2.0/tokens/12235/endpoints"

    static final String DOMAIN_MFA_URL = "cloud/v2.0/RAX-AUTH/domains/{domainId}/multi-factor"
    static final String USERS_MFA_URL = "cloud/v2.0/users/{userId}/RAX-AUTH/multi-factor"

    @Shared def request
    @Shared def scopeAccessService
    @Shared def headers
    @Shared def authTokenString = "token"
    @Shared def scopeAccess
    @Shared def filter
    @Shared def mfaService
    @Shared def requestContextHolder
    @Shared def identityUserService
    @Shared UserService userService
    @Shared def requestContext
    @Shared def securityContext
    @Shared AuthorizationService authorizationService

    @Shared SecurityContext capturedSecurityContext

    def setup() {
        filter = new AuthenticationFilter()
        request = Mock(ContainerRequest)
        identityUserService = Mock(IdentityUserService)
        filter.identityUserService = identityUserService

        userService = Mock(UserService)
        filter.userService = userService

        scopeAccessService = Mock(ScopeAccessService)
        filter.scopeAccessService = scopeAccessService
        request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER) >> authTokenString
        scopeAccess = new UserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(authTokenString) >> scopeAccess
        mfaService = Mock(MultiFactorCloud20Service)
        filter.multiFactorCloud20Service = mfaService

        authorizationService = Mock(AuthorizationService)
        filter.authorizationService = authorizationService

        requestContext = Mock(RequestContext)
        requestContextHolder = Mock(RequestContextHolder)
        requestContextHolder.getRequestContext() >> requestContext

        securityContext = Mock(SecurityContext)
        requestContext.getSecurityContext() >> securityContext
        securityContext.getEffectiveCallerToken() >> scopeAccess

        requestContext.setSecurityContext(_) >> {args -> capturedSecurityContext = args[0];}

        filter.requestContextHolder = requestContextHolder
    }

    def "security context tokens not set on auth"() {
        given:
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
