package com.rackspace.idm.api.error

import spock.lang.Shared
import testHelpers.RootServiceTest

class NumberFormatExceptionMapperTest extends RootServiceTest {
    @Shared NumberFormatExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new NumberFormatExceptionMapper()
    }

    def "exception gets mapped to bad request"() {
        when:
        def result = exceptionMapper.toResponse(new NumberFormatException())

        then:
        result.status == 400
    }
}
