package com.rackspace.idm.domain.dozer.converters

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import spock.lang.Shared
import spock.lang.Specification

class MultiFactorStateConverterTest extends Specification {

    @Shared static MultiFactorStateConverter converter = new MultiFactorStateConverter()

    def "convertTo tests with expected values"() {
        expect:
        converter.convertTo(source, MultiFactorStateEnum.ACTIVE) == result

        where:
        source | result
        null | null
        BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE | MultiFactorStateEnum.ACTIVE
        BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED | MultiFactorStateEnum.LOCKED
    }

    def "convertTo throws IllegalArgumentException when invalid value is passed in"() {
        when:
        converter.convertTo("", MultiFactorStateEnum.ACTIVE)

        then:
        thrown(IllegalStateException)

        when:
        converter.convertTo("invalid", MultiFactorStateEnum.ACTIVE)

        then:
        thrown(IllegalStateException)
    }

    def "convertFrom tests"() {
        expect:
        converter.convertFrom(source, "") == result

        where:
        source | result
        MultiFactorStateEnum.ACTIVE | 'ACTIVE'
        MultiFactorStateEnum.LOCKED | 'LOCKED'
        null | null
    }

}
