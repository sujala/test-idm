package com.rackspace.idm.event

import testHelpers.RootServiceTest

class NewRelicApiEventListenerTest extends RootServiceTest {

    NewRelicApiEventListener listener

    def setup() {
        listener = new NewRelicApiEventListener()
        mockIdentityConfig(listener)
    }

    def "Does not process if disabled"() {
        setup:
        ApiEventSpringWrapper apiEventSpringWrapper = Mock(ApiEventSpringWrapper)

        when:
        listener.onApplicationEvent(apiEventSpringWrapper)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> false

        and: "event isn't processed"
        // Sort of whitebox here as i know this is first call that would be made should processing be enabled...
        0 * apiEventSpringWrapper.getEvent()

        and: "no error is thrown"
        notThrown()
    }

    def "Processes events if enabled"() {
        setup:
        ApiEventSpringWrapper apiEventSpringWrapper = Mock(ApiEventSpringWrapper)

        when:
        listener.onApplicationEvent(apiEventSpringWrapper)

        then:
        1 * reloadableConfig.isFeatureSendNewRelicCustomDataEnabled() >> true

        and: "event isn't processed"
        1 * apiEventSpringWrapper.getEvent()
    }
}
