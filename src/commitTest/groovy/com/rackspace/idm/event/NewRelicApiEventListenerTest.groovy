package com.rackspace.idm.event

import spock.lang.Unroll
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

    @Unroll
    def "createSecuredAttributeSupport: loads configurable properties: key: '#key'; attributes: #attributes; useSha256: #useSha256"() {
        when:
        SecuredAttributeSupport sas = listener.createSecuredAttributeSupport()

        then: "loads from config file"
        1 * reloadableConfig.getNewRelicSecuredApiResourceAttributesKey() >> key
        1 * reloadableConfig.getNewRelicSecuredApiResourceAttributes() >> attributes
        1 * reloadableConfig.getNewRelicSecuredApiResourceAttributesUsingSha256() >> useSha256

        and: "vals copied to support"
        sas.hashKey == key
        sas.securedAttributeList == attributes
        sas.hashAlgorithmEnum == useSha256 ? SecuredAttributeSupport.HashAlgorithmEnum.SHA256 : SecuredAttributeSupport.HashAlgorithmEnum.SHA1

        where:
        [key, attributes, useSha256] << [["asdf", ["a"] as Set], ["", ["a", "b"] as Set, true], [null, null, false]]
    }
}
