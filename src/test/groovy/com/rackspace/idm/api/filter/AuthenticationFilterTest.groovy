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

        filter.requestContextHolder = requestContextHolder
    }

    def "when multi-factor feature flag is false all requests to user based multi-factor are unmodified"() {
        given:
        request.getPath() >> USERS_MFA_URL

        when:
        def returnedRequest = filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> false
        request == returnedRequest
        noExceptionThrown()
    }

    def "when multi-factor feature flag is true and target user does not have MFA access throws 404"() {
        given:
        request.getPath() >> USERS_MFA_URL
        User targetUser = new User()

        when:
        filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> true
        1 * identityUserService.getEndUserById(_) >> targetUser
        1 * mfaService.isMultiFactorEnabledForUser(targetUser) >> false
        WebApplicationException exception = thrown()
        exception.response.status == 404
    }

    def "when multi-factor feature flag is true and user does have MFA request is unmodified"() {
        given:
        request.getPath() >> USERS_MFA_URL
        User targetUser = new User()

        when:
        def response = filter.filter(request)

        then:
        1 * mfaService.isMultiFactorEnabled() >> true
        1 * identityUserService.getEndUserById(_) >> targetUser
        1 * mfaService.isMultiFactorEnabledForUser(targetUser) >> true
        response == request
    }

    def "when multi-factor feature flag is false, domain MFA services get WebApp exception"() {
        given:
        request.getPath() >> DOMAIN_MFA_URL

        when:
        filter.filter(request)

        then:
        mfaService.isMultiFactorEnabled() >> false
        thrown(WebApplicationException)
    }

    @Unroll
    def "when multi-factor feature flag is true, and #callerType, domain MFA call should be denied if does not have beta role"() {
        given:
        request.getPath() >> DOMAIN_MFA_URL
        User caller = new User()

        mfaService.isMultiFactorEnabled() >> true
        requestContext.getEffectiveCaller() >> caller
        mfaService.isMultiFactorEnabledForUser(caller) >> false
        authorizationService.getIdentityTypeRoleAsEnum(caller) >> callerType

        when:
        filter.filter(request)

        then:
        WebApplicationException exception = thrown()
        exception.response.status == 404

        where:
        callerType                              | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }

    @Unroll
    def "when multi-factor feature flag is true, and #callerType, domain MFA call should be allowed if has beta role"() {
        given:
        request.getPath() >> DOMAIN_MFA_URL
        User caller = new User()

        mfaService.isMultiFactorEnabled() >> true
        requestContext.getEffectiveCaller() >> caller
        mfaService.isMultiFactorEnabledForUser(caller) >> true
        authorizationService.getIdentityTypeRoleAsEnum(caller) >> callerType

        when:
        filter.filter(request)

        then:
        notThrown(WebApplicationException)

        where:
        callerType                              | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }

    @Unroll
    def "when multi-factor feature flag is true and #callerType, domain MFA call should be allowed with or without beta role (hasBetaRole - #hasBetaRole)"() {
        given:
        request.getPath() >> DOMAIN_MFA_URL
        User caller = new User()

        mfaService.isMultiFactorEnabled() >> true
        requestContext.getEffectiveCaller() >> caller
        mfaService.isMultiFactorEnabledForUser(caller) >> hasBetaRole
        authorizationService.getIdentityTypeRoleAsEnum(caller) >> callerType

        when:
        filter.filter(request)

        then:
        notThrown(WebApplicationException)

        where:
        callerType                              | hasBetaRole
        IdentityUserTypeEnum.IDENTITY_ADMIN     | true
        IdentityUserTypeEnum.SERVICE_ADMIN      | true
        IdentityUserTypeEnum.IDENTITY_ADMIN     | false
        IdentityUserTypeEnum.SERVICE_ADMIN      | false
    }

}
