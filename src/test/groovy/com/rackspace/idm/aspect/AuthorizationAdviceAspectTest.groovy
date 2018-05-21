package com.rackspace.idm.aspect

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.DefaultRequestContextHolder
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.event.ApiKeyword
import com.rackspace.idm.event.ApiResourceType
import com.rackspace.idm.event.IdentityApi
import com.rackspace.idm.exception.ForbiddenException
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.annotation.Annotation

class AuthorizationAdviceAspectTest extends Specification {

    @Unroll
    def "validateDelegationTokenAllowed with delegate token does not throw exception for allowed services - serviceName=#serviceName"() {
        given:
        AuthorizationAdviceAspect adviceAspect = new AuthorizationAdviceAspect();
        adviceAspect.requestContextHolder = getRequestContextHolder("id")
        adviceAspect.identityConfig = getIdentityConfig(true)

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << AuthorizationAdviceAspect.allowedDelegationTokenServices
    }

    @Unroll
    def "validateDelegationTokenAllowed with non delegate token does not throw exception - serviceName=#serviceName"() {
        given:
        AuthorizationAdviceAspect adviceAspect = new AuthorizationAdviceAspect();
        adviceAspect.requestContextHolder = getRequestContextHolder()
        adviceAspect.identityConfig = getIdentityConfig(true)

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"] + AuthorizationAdviceAspect.allowedDelegationTokenServices
    }

    @Unroll
    def "validateDelegationTokenAllowed with delegate token throws exception - serviceName=#serviceName"() {
        given:
        AuthorizationAdviceAspect adviceAspect = new AuthorizationAdviceAspect();
        adviceAspect.requestContextHolder = getRequestContextHolder("id")
        adviceAspect.identityConfig = getIdentityConfig(true)

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi)

        then:
        thrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"]
    }

    @Unroll
    def "validateDelegationTokenAllowed with delegate token does not throw exception - serviceName=#serviceName"() {
        given:
        AuthorizationAdviceAspect adviceAspect = new AuthorizationAdviceAspect();
        adviceAspect.requestContextHolder = getRequestContextHolder("id")
        adviceAspect.identityConfig = getIdentityConfig(false)

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"]
    }

    def getRequestContextHolder(delegationAgreementId=null) {
        RequestContext requestContext = Mock(RequestContext)
        RequestContextHolder requestContextHolder = Mock(DefaultRequestContextHolder)
        SecurityContext securityContext = Mock(SecurityContext)

        requestContextHolder.getRequestContext() >> requestContext
        requestContext.getSecurityContext() >> securityContext

        UserScopeAccess userScopeAccess = new UserScopeAccess()
        userScopeAccess.delegationAgreementId = delegationAgreementId

        securityContext.getEffectiveCallerToken() >> userScopeAccess

        return requestContextHolder
    }

    def getIdentityConfig(enabled) {
        IdentityConfig identityConfig = Mock(IdentityConfig)
        IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig.getReloadableConfig() >> reloadableConfig
        reloadableConfig.getAuthorizationAdviceAspectEnabled() >> enabled

        return identityConfig
    }

    IdentityApi getInstanceOfAnnotation(final String name) {
        IdentityApi annotation = new IdentityApi() {

            @Override
            ApiResourceType apiResourceType() {

            }

            @Override
            ApiKeyword[] keywords() {

            }

            @Override
            String name() {
                return name
            }

            @Override
            Class<? extends Annotation> annotationType() {

            }
        }

        return annotation;
    }
}


