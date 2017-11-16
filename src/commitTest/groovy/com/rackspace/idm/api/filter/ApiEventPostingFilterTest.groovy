package com.rackspace.idm.api.filter

import com.rackspace.idm.Constants
import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.event.*
import com.sun.jersey.spi.container.ContainerRequest
import com.sun.jersey.spi.container.ContainerResponse
import org.apache.commons.lang.StringUtils
import org.slf4j.MDC
import spock.lang.Unroll
import testHelpers.RootServiceTest

class ApiEventPostingFilterTest extends RootServiceTest {
    ApiEventPostingFilter filter

    def setup() {
        filter = new ApiEventPostingFilter()

        mockIdentityConfig(filter)
        mockRequestContextHolder(filter)
        mockIdmPathUtils(filter)
        mockApplicationEventPublisher(filter)
    }

    def "Does not publish if disabled"() {
        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> false

        and: "no event is published"
        0 * applicationEventPublisher.publishEvent(_)
        0 * idmPathUtils.isAuthenticationResource(_)

        and: "no error is thrown"
        notThrown()
    }

    def "Does not publish if processing doesn't create event"() {
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isProtectedResource(_) >> false
        1 * idmPathUtils.isAuthenticationResource(_) >> false
        1 * idmPathUtils.isUnprotectedResource(_) >> false

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        0 * applicationEventPublisher.publishEvent(_)

        and: "no error is thrown"
        notThrown()
    }

    def "AuthApi event: Publishes auth events when auth resource"() {
        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isAuthenticationResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof AuthApiEvent
            assert ((AuthApiEvent) event.event).eventType == "AuthApi"
        }
    }

    def "AuthApi event: is still sent even with no data available and indicates data is not available"() {
        setup:
        prepCommonEventData()
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            AuthApiEvent event = args[0].event
            assert event.eventType == "AuthApi"
            assertCommonEventData(event)
            assert event.userName == ApiEventPostingFilter.DATA_UNAVAILABLE
        }
    }

    @Unroll
    def "AuthApi event: Populates auth event based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName; userName: #userName"() {
        setup:
        prepCommonEventData(eventId, remoteIp, forwardedForIp, nodeName)
        authenticationContext.getUsername() >> userName

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isAuthenticationResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            AuthApiEvent event = args[0].event
            assert event.eventType == "AuthApi"
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
            assert event.userName == StringUtils.defaultString(userName, ApiEventPostingFilter.DATA_UNAVAILABLE)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName, userName] << [[null, null, null, null, null]
                                                                    , ["", "", "", "", ""]
                                                                    , [" ", " ", " ", " ", " "]
                                                                    , ["eventId", "rIp", "fIp", "node", "username"]]
    }

    def "ProtectedApi event: Published when protected resource"() {
        setup:
        ContainerRequest request = Mock(ContainerRequest)
        ContainerResponse response = Mock(ContainerResponse)
        request.getRequestUri() >> new URI("http://request")

        when:
        filter.filter(request, response)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isProtectedResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof ProtectedApiEvent
        }
    }

    def "ProtectedApi event: is still sent even with no data available and indicates data is not available"() {
        setup:
        prepCommonEventData()
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.caller != null
            assert event.caller.id == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.caller.username == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.caller.userType == null
            assert event.maskedCallerToken == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.effectiveCaller != null
            assert event.effectiveCaller.id == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.effectiveCaller.username == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.effectiveCaller.userType == null
            assert event.maskedEffectiveCallerToken == ApiEventPostingFilter.DATA_UNAVAILABLE
        }
    }

    @Unroll
    def "ProtectedApi event: populated w/ common data based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName"() {
        setup:
        prepCommonEventData(eventId, remoteIp, forwardedForIp, nodeName)

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isProtectedResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName] << [[null, null, null, null]
                                                          , ["", "", "", ""]
                                                          , [" ", " ", " ", " "]
                                                          , ["eventId", "rIp", "fIp", "node"]]
    }

    @Unroll
    def "ProtectedApi event: effective caller data populated w/ effective caller data in sec context"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true
        prepCommonEventData()

        authorizationContext.getIdentityUserType() >> userType

        when: "SecContext effectiveCaller is populated"
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = id
            it.username = username
            it
        }

        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then: "Populated data from effective caller as-is"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
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
    def "ProtectedApi event: effective caller userId falls back to effectiveToken in sec context if effectiveCaller missing"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true
        prepCommonEventData()

        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.userRsId = userId
            it
        }

        when: "SecContext effectiveCaller is populated"
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then: "Populated with effective caller data"
        securityContext.getEffectiveCaller() >> new User().with {
            it.id = "otherId"
            it.username = "userName"
            it
        }
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.effectiveCaller.id == "otherId"
            assert event.effectiveCaller.username == "userName"
        }

        when: "SecContext effectiveCaller is not populated"
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then: "Falls back to sec context effectiveCallerToken"
        securityContext.getEffectiveCaller() >> null
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.effectiveCaller.id == userId
            assert event.effectiveCaller.username == ApiEventPostingFilter.DATA_UNAVAILABLE
        }

        where:
        userId << [null, "id"]
    }

    @Unroll
    def "ProtectedApi event: effective caller token masked appropriately"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true
        prepCommonEventData()

        securityContext.getEffectiveCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = rawToken
            it
        }

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.maskedEffectiveCallerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , [" ", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["token", "*****oken"]
        ]
    }

    @Unroll
    def "ProtectedApi event: caller data populated w/ effectiveCaller data in sec context when tokens match"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true

        //blank out common data for this test. Tested in other test
        prepCommonEventData()

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
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then: "Caller data populated with effectiveCaller"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
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
    def "ProtectedApi event: caller data falls back to callerToken when doesn't match effectiveCaller token"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true

        prepCommonEventData()
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
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then: "only userId populated"
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.caller != null
            assert event.caller.id == id
            assert event.caller.username == ApiEventPostingFilter.DATA_UNAVAILABLE
            assert event.caller.userType == null
        }

        where:
        id << [null, "", " ", "userId"]
    }

    @Unroll
    def "ProtectedApi event: caller token masked appropriately when caller token in sec context"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true
        prepCommonEventData()

        when:
        securityContext.getCallerToken() >> new UserScopeAccess().with {
            it.accessTokenString = rawToken
            it
        }

        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.maskedCallerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , [" ", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["token", "*****oken"]
        ]
    }

    @Unroll
    def "ProtectedApi event: caller token falls back to header when not in sec context and masked appropriately"() {
        setup:
        reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        idmPathUtils.isProtectedResource(_) >> true
        prepCommonEventData()

        securityContext.getCallerToken() >> null
        ContainerRequest req = Mock(ContainerRequest)

        when:
        filter.filter(req, Mock(ContainerResponse))

        then:
        1 * req.getHeaderValue(Constants.X_AUTH_TOKEN) >> rawToken
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ProtectedApiEvent event = args[0].event
            assert event.eventType == "ProtectedApi"
            assertCommonEventData(event)
            assert event.maskedCallerToken == expectedMaskedToken
        }

        where:
        [rawToken, expectedMaskedToken] << [[null, ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , [" ", ApiEventPostingFilter.DATA_UNAVAILABLE]
                                            , ["token", "*****oken"]
        ]
    }

    def "UnprotectedAPi event: Publishes unprotected event when unprotected resource"() {
        setup:
        ContainerRequest request = Mock(ContainerRequest)
        ContainerResponse response = Mock(ContainerResponse)
        request.getRequestUri() >> new URI("http://request")

        when:
        filter.filter(request, response)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isUnprotectedResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            ApiEventSpringWrapper event = args[0]
            assert event.event instanceof UnprotectedApiEvent
        }
    }

    @Unroll
    def "UnprotectedApi event: populated w/ common data based on available data: eventId: #eventId; remoteIp: #remoteIp; forwardedForIp: #forwardedForIp; nodeName: #nodeName"() {
        setup:
        prepCommonEventData(eventId, remoteIp, forwardedForIp, nodeName)

        when:
        filter.filter(Mock(ContainerRequest), Mock(ContainerResponse))

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true
        1 * idmPathUtils.isUnprotectedResource(_) >> true

        and:
        1 * applicationEventPublisher.publishEvent(_) >> { args ->
            UnprotectedApiEvent event = args[0].event
            assert event.eventType == "UnprotectedApi"
            assertCommonEventData(event, eventId, remoteIp, forwardedForIp, nodeName)
        }

        where:
        [eventId, remoteIp, forwardedForIp, nodeName] << [[null, null, null, null]
                                                          , ["", "", "", ""]
                                                          , [" ", " ", " ", " "]
                                                          , ["eventId", "rIp", "fIp", "node"]]
    }

    @Unroll
    def "V2 Validate Uris are scrubbed as expected"() {
        ContainerRequest request = Mock(ContainerRequest)

        when:
        String maskedUri = filter.scrubRequestUri(request)

        then:
        idmPathUtils.isV2ValidateTokenResource(request) >> isV2Validate

        request.getRequestUri() >> rawUri
        request.getPath() >> { rawUri.getPath().length() > 1 ? rawUri.getPath().substring(1) : "" }
        maskedUri == expectedMaskedUriStr

        where:
        [isV2Validate, rawUri, expectedMaskedUriStr] << [
                [false, new URI("http://asdf"), "http://asdf"]
                , [true, new URI("http://asdf/cloud/v2.0/tokens/abcdefg"), "http://asdf/cloud/v2.0/tokens/*****defg"]
                , [true, new URI("http://asdf/cloud/v2.0/tokens/abcdefg/"), "http://asdf/cloud/v2.0/tokens/*****defg/"]
        ]
    }

    @Unroll
    def "V2 Validate/Revoke Uris are scrubbed as expected"() {
        ContainerRequest request = Mock(ContainerRequest)
        request.getRequestUri() >> rawUri
        request.getPath() >> { rawUri.getPath().length() > 1 ? rawUri.getPath().substring(1) : "" }

        when:
        String maskedUri = filter.scrubRequestUri(request)

        then:
        1 * idmPathUtils.isV2ValidateTokenResource(request) >> matches
        maskedUri == expectedMaskedUriStr

        when:
        maskedUri = filter.scrubRequestUri(request)

        then:
        idmPathUtils.isV2ValidateTokenResource(request) >> false
        1 * idmPathUtils.isV2RevokeOtherTokenResource(request) >> matches
        maskedUri == expectedMaskedUriStr

        where:
        [matches, rawUri, expectedMaskedUriStr] << [
                [false, new URI("http://asdf"), "http://asdf"]
                , [true, new URI("http://asdf/cloud/v2.0/tokens/abcdefg"), "http://asdf/cloud/v2.0/tokens/*****defg"]
                , [true, new URI("http://asdf/cloud/v2.0/tokens/abcdefg/"), "http://asdf/cloud/v2.0/tokens/*****defg/"]
        ]
    }

    @Unroll
    def "V1.1 Validate/Revoke Uris are scrubbed as expected"() {
        ContainerRequest request = Mock(ContainerRequest)
        request.getRequestUri() >> rawUri
        request.getPath() >> { rawUri.getPath().length() > 1 ? rawUri.getPath().substring(1) : "" }

        when:
        String maskedUri = filter.scrubRequestUri(request)

        then:
        1 * idmPathUtils.isV11ValidateTokenResource(request) >> matches
        maskedUri == expectedMaskedUriStr

        when:
        maskedUri = filter.scrubRequestUri(request)

        then:
        idmPathUtils.isV11ValidateTokenResource(request) >> false
        1 * idmPathUtils.isV11RevokeTokenResource(request) >> matches
        maskedUri == expectedMaskedUriStr

        where:
        [matches, rawUri, expectedMaskedUriStr] << [
                [false, new URI("http://asdf"), "http://asdf"]
                , [true, new URI("http://asdf/cloud/v1.1/token/abcdefg"), "http://asdf/cloud/v1.1/token/*****defg"]
                , [true, new URI("http://asdf/cloud/v1.1/token/abcdefg/"), "http://asdf/cloud/v1.1/token/*****defg/"]
        ]
    }

    void prepCommonEventData(String eventId = null, String remoteIp = null, String forwardedForIp = null, String nodeName = null) {
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

        reloadableConfig.getAENodeNameForSignoff() >> nodeName
    }

    void assertCommonEventData(ApiEvent event, eventId = null, remoteIp = null, forwardedForIp = null, nodeName = null) {
        assert event.eventId == StringUtils.defaultString(eventId, ApiEventPostingFilter.DATA_UNAVAILABLE)
        assert event.remoteIp == StringUtils.defaultString(remoteIp, ApiEventPostingFilter.DATA_UNAVAILABLE)
        assert event.forwardedForIp == StringUtils.defaultString(forwardedForIp, ApiEventPostingFilter.DATA_UNAVAILABLE)
        assert event.nodeName == StringUtils.defaultString(nodeName, ApiEventPostingFilter.DATA_UNAVAILABLE)
    }
}
