package com.rackspace.idm.api.error

import spock.lang.Shared
import testHelpers.RootServiceTest

class ClassCastExceptionMapperTest extends RootServiceTest {
    @Shared ClassCastExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new ClassCastExceptionMapper()
    }

    def "exception gets mapped to badRequest"() {
        when:
        def result = exceptionMapper.toResponse(new ClassCastException())

        then:
        result.status == 400
    }
}
