package com.rackspace.idm.api.error

import com.rackspace.idm.exception.StalePasswordException
import spock.lang.Shared
import testHelpers.RootServiceTest

class StalePasswordExceptionMapperTest extends RootServiceTest {
    @Shared StalePasswordExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new StalePasswordExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new StalePasswordException())

        then:
        result.status == 409
    }
}
