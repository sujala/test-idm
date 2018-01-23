package com.rackspace.idm.event

import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import java.util.regex.Pattern

class NewRelicApiEventListenerTest extends RootServiceTest {

    NewRelicApiEventListener listener
    @Shared def hmacKey = "akey"
    @Shared SecuredAttributeSupport support = new SecuredAttributeSupport(SecuredAttributeSupport.HashAlgorithmEnum.SHA256, hmacKey, [] as Set)

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

    @Unroll
    def "secureMatchedGroupValues: Secures regex groups. regex: '#regex' ; input: '#input' ; expectedSecuredValue: '#expectedSecuredValue'"() {
        given:
        reloadableConfig.getNewRelicSecuredApiResourceAttributesKey() >> hmacKey
        reloadableConfig.getNewRelicSecuredApiResourceAttributesUsingSha256() >> true
        reloadableConfig.getNewRelicSecuredApiResourceAttributes() >> []

        when:
        def result = listener.secureMatchedGroupValues(Pattern.compile(regex), input, listener.createSecuredAttributeSupport())

        then:
        result == expectedSecuredValue

        where:
        regex           | input             | expectedSecuredValue
        "^.*(ab).*\$"   | "orig_ab_inal"    | "orig_" + support.secureAttributeValue("ab") + "_inal"
        "^.*(ab).*\$"   | "orig_ab_inal"    | "orig_" + support.secureAttributeValue("ab") + "_inal"
        "^.*(ab).*(na).*\$"   | "orig_ab_inal"    | String.format("orig_%s_i%sl", support.secureAttributeValue("ab"), support.secureAttributeValue("na"))
        NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/atoken" | String.format("http://localhost:8083/idm/cloud/v2.0/tokens/%s", support.secureAttributeValue("atoken"))
        NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/atoken/" | String.format("http://localhost:8083/idm/cloud/v2.0/tokens/%s/", support.secureAttributeValue("atoken"))
        NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/" | "http://localhost:8083/idm/cloud/v2.0/tokens/"

        NewRelicApiEventListener.v2TokenEndpointAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/atoken/endpoints/" | String.format("http://localhost:8083/idm/cloud/v2.0/tokens/%s/endpoints/", support.secureAttributeValue("atoken"))
        NewRelicApiEventListener.v2TokenEndpointAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/atoken /endpoints/" | String.format("http://localhost:8083/idm/cloud/v2.0/tokens/%s/endpoints/", support.secureAttributeValue("atoken "))
        NewRelicApiEventListener.v2TokenEndpointAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v2.0/tokens/atoken/endpoints/" | String.format("http://localhost:8083/idm/cloud/v2.0/tokens/%s/endpoints/", support.secureAttributeValue("atoken"))

        NewRelicApiEventListener.v11TokenValidationAbsolutePathPatternRegex | "http://localhost:8083/idm/cloud/v1.1/token/atoken" | String.format("http://localhost:8083/idm/cloud/v1.1/token/%s", support.secureAttributeValue("atoken"))
    }
}
