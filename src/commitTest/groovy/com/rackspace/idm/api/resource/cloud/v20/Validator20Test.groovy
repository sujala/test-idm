package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.service.IdpPolicyFormatEnum
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.validation.JsonValidator
import com.rackspace.idm.validation.Validator20
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
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
        mockFederatedIdentityService(service)
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

    def "validateIdentityProviderIssuerWithDupCheck - if IDP already exists with issuer, throws Dup exception"() {
        given:
        IdentityProvider webProvider = new IdentityProvider().with {
            it.issuer = "duplicate"
            it
        }
        when:
        service.validateIdentityProviderIssuerWithDupCheck(webProvider)

        then: "calls to check dup"
        1 * defaultFederatedIdentityService.getIdentityProviderByIssuer(webProvider.issuer) >> new com.rackspace.idm.domain.entity.IdentityProvider()

        and: "throws dup exception when IDP already exists with that issuer"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, DuplicateException, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS_MSG)
    }

    def "validateIdentityProviderIssuerWithDupCheck - if IDP does not exists with issuer, does not throw Dup exception"() {
        given:
        IdentityProvider webProvider = new IdentityProvider().with {
            it.issuer = "duplicate"
            it
        }
        when:
        service.validateIdentityProviderIssuerWithDupCheck(webProvider)

        then: "calls to check dup"
        1 * defaultFederatedIdentityService.getIdentityProviderByIssuer(webProvider.issuer) >> null

        and: "no error is thrown if dup check doesn't return an IDP"
        notThrown()
    }
}
