package com.rackspace.idm.aspect


import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.event.ApiKeyword
import com.rackspace.idm.event.ApiResourceType
import com.rackspace.idm.event.IdentityApi
import com.rackspace.idm.exception.ForbiddenException
import com.sun.jersey.spi.container.ContainerRequest
import org.aspectj.lang.ProceedingJoinPoint
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
import java.lang.annotation.Annotation

class AuthorizationAdviceAspectTest extends RootServiceTest  {
    AuthorizationAdviceAspect adviceAspect
    static final String TOKEN_ENDPOINT_URL = "cloud/v2.0/tokens/12235/endpoints"


    void setup() {
        adviceAspect = new AuthorizationAdviceAspect()
        mockIdentityConfig(adviceAspect)
        mockRequestContextHolder(adviceAspect)
        mockScopeAccessService(adviceAspect)
        mockExceptionHandler(adviceAspect)
    }

    @Unroll
    def "validateDelegationTokenAllowed with delegate token does not throw exception for allowed services - serviceName=#serviceName"() {
        given:
        def scopeAccess = mockEffectiveCallerToken("id")
        reloadableConfig.getAuthorizationAdviceAspectEnabled() >> true

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi, scopeAccess)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << AuthorizationAdviceAspect.allowedDelegationTokenServices
    }

    @Unroll
    def "validateDelegationTokenAllowed with non delegate token does not throw exception - serviceName=#serviceName"() {
        given:
        def scopeAccess = mockEffectiveCallerToken()
        reloadableConfig.getAuthorizationAdviceAspectEnabled() >> true

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi, scopeAccess)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"] + AuthorizationAdviceAspect.allowedDelegationTokenServices
    }

    @Unroll
    def "validateDelegationTokenAllowed with delegate token throws exception - serviceName=#serviceName"() {
        given:
        def scopeAccess = mockEffectiveCallerToken("id")
        reloadableConfig.getAuthorizationAdviceAspectEnabled() >> true

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi, scopeAccess)

        then:
        thrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"]
    }

    @Unroll
    def "validateDelegationTokenAllowed with delegate token does not throw exception - serviceName=#serviceName"() {
        given:
        def scopeAccess = mockEffectiveCallerToken("id")
        reloadableConfig.getAuthorizationAdviceAspectEnabled() >> false

        when:
        IdentityApi identityApi = getInstanceOfAnnotation(serviceName)
        adviceAspect.validateDelegationTokenAllowed(identityApi, scopeAccess)

        then:
        notThrown(ForbiddenException)

        where:
        serviceName << ["v2.0 List tenants for domain", "v2.0 List users by domain"]
    }

    /**
     * This is just a test that the feature flag encapsulates the code. It is not meant to exhaustively
     * test the MFA code within the AuthenticationFilter.
     * @return
     */
    def "MFA validation does not occur when flag disabled"() {
        reloadableConfig.useAspectForMfaAuthorization() >> false
        def scopeAccess = mockEffectiveCallerToken("id").with {
            it.scope = TokenScopeEnum.SETUP_MFA
            it
        }
        ProceedingJoinPoint joinPoint = Mock()
        IdentityApi identityApi = getInstanceOfAnnotation("notUsed")
        ContainerRequest containerRequest = Mock()
        requestContext.getContainerRequest() >> containerRequest
        containerRequest.getPath() >> TOKEN_ENDPOINT_URL

        when: "A non MFA request is sent using a setup MFA token"
        adviceAspect.validateAnnotatedField(joinPoint, identityApi)

        then:
        0 * scopeAccessService.isSetupMfaScopedToken(scopeAccess)
        notThrown(ForbiddenException)
    }

    /**
     * This is just a test that the feature flag encapsulates the code. It is not meant to exhaustively
     * test the MFA code within the AuthenticationFilter.
     * @return
     */
    def "MFA validation occurs when flag enabled"() {
        reloadableConfig.useAspectForMfaAuthorization() >> true
        def scopeAccess = mockEffectiveCallerToken("id").with {
            it.scope = TokenScopeEnum.SETUP_MFA
            it
        }
        ProceedingJoinPoint joinPoint = Mock()
        IdentityApi identityApi = getInstanceOfAnnotation("notUsed")
        ContainerRequest containerRequest = Mock()
        requestContext.getContainerRequest() >> containerRequest
        containerRequest.getPath() >> TOKEN_ENDPOINT_URL

        when: "A non MFA request is sent using a setup MFA token"
        adviceAspect.validateAnnotatedField(joinPoint, identityApi)

        then:
        1 * scopeAccessService.isSetupMfaScopedToken(scopeAccess) >> true
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception ex = args[0]
            IdmExceptionAssert.assertException(ex, ForbiddenException, null, "The scope of this token does not allow access to this resource")

            // Just return anything here. Not testing the response
            return Response.ok()
        }    }

    def mockEffectiveCallerToken(delegationAgreementId=null) {
        UserScopeAccess userScopeAccess = new UserScopeAccess()
        userScopeAccess.delegationAgreementId = delegationAgreementId
        securityContext.getEffectiveCallerToken() >> userScopeAccess
        return userScopeAccess
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


