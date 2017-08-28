package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.IdpPolicyFormatEnum
import com.rackspace.idm.validation.JsonValidator
import com.rackspace.idm.validation.Validator20
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class Validator20Test extends RootServiceTest {

    @Shared Validator20 service

    @Shared JsonValidator jsonValidator

    def setupSpec() {
        service = new Validator20()
    }

    def setup() {
        jsonValidator = Mock()
        service.jsonValidator = jsonValidator
        mockIdentityConfig(service)
    }

    @Unroll
    def "Validate default policy file: type = #type" () {
        when:
        service.validateIdpPolicy(policy, IdpPolicyFormatEnum.valueOf(type))

        then:
        1 * identityConfig.getReloadableConfig().getIdpPolicyMaxSize() >> 2
        jsonValidatorCalls * jsonValidator.isValidJson(policy) >> true

        where:
        policy                         | type   | jsonValidatorCalls
        '{"policy": {"name":"name"}"}' | "JSON" | 1
        "--- policy: name: name"       | "YAML" | 0
    }
}
