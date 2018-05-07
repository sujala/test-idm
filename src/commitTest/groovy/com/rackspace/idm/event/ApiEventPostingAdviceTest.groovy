package com.rackspace.idm.event

import com.rackspace.idm.Constants
import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.sun.jersey.spi.container.ContainerRequest
import org.apache.commons.lang.StringUtils
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.MDC
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static com.rackspace.idm.event.ApiEventPostingAdvice.DATA_UNAVAILABLE

class ApiEventPostingAdviceTest extends RootServiceTest {
    ApiEventPostingAdvice advice
    JoinPoint joinPoint
    MethodSignature signature
    ContainerRequest containerRequest = Mock(ContainerRequest)

    def setup() {
        advice = new ApiEventPostingAdvice()

        mockIdentityConfig(advice)
        mockRequestContextHolder(advice)
        mockApplicationEventPublisher(advice)

        joinPoint = Mock(JoinPoint)
        signature = Mock(MethodSignature)
        joinPoint.getSignature() >> signature

        requestContext.getContainerRequest() >> containerRequest
    }

    def "Does not publish if disabled"() {
        when:
        advice.postEvent(joinPoint)

        then:
        0 * signature.getMethod()
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> false

        and: "no event is published"
        0 * applicationEventPublisher.publishEvent(_)

        and: "no error is thrown"
        notThrown()
    }

    def "AuthApi event: Publishes auth events when auth resource"() {
        signature.getMethod() >> TestClass.getMethod(TestClass.AUTH_METHOD)

        when:
        advice.postEvent(joinPoint)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof AuthApiEvent
            assert event.getEvent().getResourceType() == ApiResourceType.AUTH
        }
    }

    def "AuthApi event: is still sent even with no data available and indicates data is not available"() {
        setup:
        prepCommonEventData()

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            AuthApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.AUTH
            assertCommonEventData(event)
            assert event.userName == DATA_UNAVAILABLE
        }
    }

    @Unroll
    def "AuthApi event: Populates auth event based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName; userName: #userName"() {
        setup:
        prepCommonEventData(TestClass.AUTH_METHOD, eventId, remoteIp, forwardedForIp, nodeName)
        authenticationContext.getUsername() >> userName

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            AuthApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.AUTH
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
            assert event.userName == StringUtils.defaultString(userName, DATA_UNAVAILABLE)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName, userName] << [[null, null, null, null, null]
                                                                    , ["", "", "", "", ""]
                                                                    , [" ", " ", " ", " ", " "]
                                                                    , ["requestId", "rIp", "fIp", "node", "username"]]
    }

    def "Private event: Published when private resource"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)
        containerRequest.getRequestUri() >> new URI("http://request")
        containerRequest.getPath() >> new URI("http://request")

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof PrivateApiEvent
        }
    }

    def "Private event: is still sent even with no data available and indicates data is not available"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.caller != null
            assert event.caller.id == DATA_UNAVAILABLE
            assert event.caller.username == DATA_UNAVAILABLE
            assert event.caller.userType == null
            assert event.callerToken == DATA_UNAVAILABLE
            assert event.effectiveCaller != null
            assert event.effectiveCaller.id == DATA_UNAVAILABLE
            assert event.effectiveCaller.username == DATA_UNAVAILABLE
            assert event.effectiveCaller.userType == null
            assert event.effectiveCallerToken == DATA_UNAVAILABLE
        }
    }

    @Unroll
    def "Private event: populated w/ common data based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD, eventId, remoteIp, forwardedForIp, nodeName)

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName] << [[null, null, null, null]
                                                          , ["", "", "", ""]
                                                          , [" ", " ", " ", " "]
                                                          , ["requestId", "rIp", "fIp", "node"]]
    }

    @Unroll
    def "Private event: effective caller data populated w/ effective caller data in sec context"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        authorizationContext.getIdentityUserType() >> userType

        when: "SecContext effectiveCaller is populated"
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = id
            it.username = username
            it
        }

        advice.postEvent(joinPoint)

        then: "Populated data from effective caller as-is"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.effectiveCaller != null
            assert event.effectiveCaller.id == id
            assert event.effectiveCaller.username == username
            assert event.effectiveCaller.userType == userType
        }

        where:
        [id, username, userType, rawToken] << [[null, null, null]
                                               , ["", "", null]
                                               , [" ", " ", null]
                                               , ["id", "username", IdentityUserTypeEnum.USER_MANAGER]]
    }

    @Unroll
    def "Private event: effective caller userId falls back to effectiveToken in sec context if effectiveCaller missing"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.userRsId = userId
            it
        }

        when: "SecContext effectiveCaller is populated"
        advice.postEvent(joinPoint)

        then: "Populated with effective caller data"
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = "otherId"
            it.username = "userName"
            it
        }
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.effectiveCaller.id == "otherId"
            assert event.effectiveCaller.username == "userName"
        }

        when: "SecContext effectiveCaller is not populated"
        advice.postEvent(joinPoint)

        then: "Falls back to sec context effectiveCallerToken"
        securityContext.getEffectiveCaller() >> null
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.effectiveCaller.id == userId
            assert event.effectiveCaller.username == DATA_UNAVAILABLE
        }

        where:
        userId << [null, "id"]
    }

    @Unroll
    def "Private event: effective caller token set appropriately"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = rawToken
            it
        }

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.effectiveCallerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, DATA_UNAVAILABLE]
                                            , ["", DATA_UNAVAILABLE]
                                            , [" ", DATA_UNAVAILABLE]
                                            , ["token", "token"]
        ]
    }

    @Unroll
    def "Private event: caller data populated w/ effectiveCaller data in sec context when tokens match"() {
        setup:
        //blank out common data for this test. Tested in other test
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = "token"
            it
        }
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = id
            it.username = username
            it
        }
        authorizationContext.getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER

        when: "caller token matches effective caller"
        securityContext.getCallerToken() >> securityContext.getEffectiveCallerToken()
        advice.postEvent(joinPoint)

        then: "Caller data populated with effectiveCaller"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.caller != null
            assert event.caller.id == event.effectiveCaller.id
            assert event.caller.username == event.effectiveCaller.username
            assert event.caller.userType == event.effectiveCaller.userType
        }

        where:
        [id, username, userType, rawToken] << [[null, null, null]
                                               , ["", "", null]
                                               , [" ", " ", null]
                                               , ["id", "username",]]
    }

    @Unroll
    def "Private event: caller data falls back to callerToken when doesn't match effectiveCaller token"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)
        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = "token"
            it
        }
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = "eCallerId"
            it.username = "eUserName"
            it
        }
        authorizationContext.getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER

        securityContext.getCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = "differentToken"
            it.userRsId = id
            it
        }

        when:
        advice.postEvent(joinPoint)

        then: "only userId populated"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.caller != null
            assert event.caller.id == id
            assert event.caller.username == DATA_UNAVAILABLE
            assert event.caller.userType == null
        }

        where:
        id << [null, "", " ", "userId"]
    }

    @Unroll
    def "Private event: caller token set appropriately when caller token in sec context"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)

        when:
        securityContext.getCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = rawToken
            it
        }

        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.callerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, DATA_UNAVAILABLE]
                                            , ["", DATA_UNAVAILABLE]
                                            , [" ", DATA_UNAVAILABLE]
                                            , ["token", "token"]
        ]
    }

    @Unroll
    def "Private event: caller token falls back to header when not in sec context"() {
        setup:
        prepCommonEventData(TestClass.PRIVATE_METHOD)
        securityContext.getCallerToken() >> null

        when:
        advice.postEvent(joinPoint)

        then:
        1 * containerRequest.getHeaderValue(Constants.X_AUTH_TOKEN) >> rawToken
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PrivateApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PRIVATE
            assertCommonEventData(event)
            assert event.callerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, DATA_UNAVAILABLE]
                                            , ["", DATA_UNAVAILABLE]
                                            , [" ", DATA_UNAVAILABLE]
                                            , ["token", "token"]
        ]
    }

    def "Public event: Publishes public event when public resource"() {
        setup:
        prepCommonEventData(TestClass.PUBLIC_METHOD)
        containerRequest.getRequestUri() >> new URI("http://request")
        containerRequest.getPath() >> new URI("http://request")

        when:
        advice.postEvent(joinPoint)

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof PublicApiEvent
        }
    }

    @Unroll
    def "Public event: populated w/ common data based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName"() {
        setup:
        prepCommonEventData(TestClass.PUBLIC_METHOD, eventId, remoteIp, forwardedForIp, nodeName)

        when:
        advice.postEvent(joinPoint)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            PublicApiEvent event = args[0].event
            assert event.resourceType == ApiResourceType.PUBLIC
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName] << [[null, null, null, null]
                                                          , ["", "", "", ""]
                                                          , [" ", " ", " ", " "]
                                                          , ["requestId", "rIp", "fIp", "node"]]
    }

    void prepCommonEventData(String methodName = TestClass.AUTH_METHOD, String eventId = null, String remoteIp = null, String forwardedForIp = null, String nodeName = null) {
        // Wipe out MDC first
        MDC.clear()
        if (remoteIp != null) {
            MDC.put(Audit.REMOTE_IP, remoteIp)
        }
        if (eventId != null) {
            MDC.put(Audit.GUUID, eventId)
        }
        if (forwardedForIp != null) {
            MDC.put(Audit.X_FORWARDED_FOR, forwardedForIp)
        }

        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        signature.getMethod() >> TestClass.getMethod(methodName)
        reloadableConfig.getAENodeNameForSignoff() >> nodeName
    }

    void assertCommonEventData(ApiEvent event, eventId = null, remoteIp = null, forwardedForIp = null, nodeName = null) {
        assert event.requestId == StringUtils.defaultString(eventId, DATA_UNAVAILABLE)
        assert event.remoteIp == StringUtils.defaultString(remoteIp, DATA_UNAVAILABLE)
        assert event.forwardedForIp == StringUtils.defaultString(forwardedForIp, DATA_UNAVAILABLE)
        assert event.nodeName == StringUtils.defaultString(nodeName, DATA_UNAVAILABLE)
    }

    class TestClass {
        static String AUTH_METHOD = "authApiMethod"
        static String PRIVATE_METHOD = "privateApiMethod"
        static String PUBLIC_METHOD = "publicApiMethod"

        @IdentityApi(apiResourceType = ApiResourceType.AUTH, name = "Test Call")
        void authApiMethod() {}

        @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "Test Call")
        void privateApiMethod() {}

        @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name = "Test Call")
        void publicApiMethod() {}
    }
}
